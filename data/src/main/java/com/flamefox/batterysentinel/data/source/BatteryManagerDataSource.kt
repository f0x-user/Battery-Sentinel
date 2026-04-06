package com.flamefox.batterysentinel.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import com.flamefox.batterysentinel.domain.model.PluggedType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for raw battery data from the Android system.
 *
 * Two read strategies are offered:
 *   - [observeBatteryState]: polling flow (1 s interval) used by BatteryMonitorService to persist
 *     samples and drive the Dashboard UI.
 *   - [observeBatteryChangedEvents]: broadcast-based flow that fires only when the system sends
 *     ACTION_BATTERY_CHANGED (level change, plug/unplug, temperature change).
 *
 * Permission notes (Android 16 / API 36):
 *   CURRENT_NOW, CHARGE_COUNTER require BATTERY_STATS — fall back to 0 if not granted.
 *   Cycle count uses sysfs (no permission on Pixel) or BatteryManager hidden property 9.
 */
@Singleton
class BatteryManagerDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val batteryManager: BatteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    /**
     * Emits a fresh [BatteryState] every second by polling [readCurrentState].
     * Used by [BatteryMonitorService] to write samples to Room and update the live state flow.
     */
    fun observeBatteryState(): Flow<BatteryState> = flow {
        while (true) {
            emit(readCurrentState())
            delay(1000L)
        }
    }

    /**
     * Emits a [BatteryState] each time the system broadcasts ACTION_BATTERY_CHANGED.
     * Suitable for reacting to plug/unplug events without continuous polling.
     * The flow is cancelled (and the receiver unregistered) when the collector is cancelled.
     */
    fun observeBatteryChangedEvents(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(parseBatteryIntent(intent))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Reads the current battery state synchronously via a sticky broadcast.
     * Passing null as receiver to registerReceiver returns the last sticky intent immediately.
     */
    fun readCurrentState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return parseBatteryIntent(intent)
    }

    /**
     * Reads the hardware charge cycle count with two fallback strategies:
     *
     * 1. sysfs path `/sys/class/power_supply/battery/cycle_count` — readable without any
     *    permission on Pixel and many other devices. Fast, no IPC.
     * 2. BatteryManager.getIntProperty(9) — integer 9 is the hidden constant
     *    BATTERY_PROPERTY_CYCLE_COUNT (not part of the public SDK surface, added in API 28).
     *    Requires BATTERY_STATS on Android 16+; SecurityException is caught and -1 is returned.
     *
     * Returns -1 when neither source provides a valid (> 0) value. The UI displays "—" for -1.
     */
    fun readCycleCount(): Int {
        // Priority 1: sysfs — works without root or special permissions on most Pixel devices.
        try {
            val sysfsValue = File("/sys/class/power_supply/battery/cycle_count")
                .takeIf { it.exists() && it.canRead() }
                ?.readText()?.trim()?.toIntOrNull()
            if (sysfsValue != null && sysfsValue > 0) return sysfsValue
        } catch (_: Exception) {}

        // Priority 2: hidden BatteryManager API.
        // Note: BATTERY_PROPERTY_CHARGE_COUNTER (= 1) measures µAh, NOT cycles — do NOT use it here.
        return try {
            batteryManager.getIntProperty(9 /* BATTERY_PROPERTY_CYCLE_COUNT, hidden API */)
                .takeIf { it > 0 } ?: -1
        } catch (_: SecurityException) { -1 }
    }

    /**
     * Parses an ACTION_BATTERY_CHANGED intent into a [BatteryState] domain model.
     *
     * All fields that can be read from intent extras are read there (no permission required).
     * Fields that require BATTERY_STATS are wrapped in [batteryIntProperty] which returns a
     * safe default (0) on SecurityException so the app never crashes.
     */
    private fun parseBatteryIntent(intent: Intent?): BatteryState {
        // Percentage from Intent extras — no permissions required, always available.
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale)
        else batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 0)

        // Current (µA → mA) and charge counter (µAh) require BATTERY_STATS on API 36.
        // Fall back to 0 when the permission is not granted — app never crashes.
        val rawCurrent = batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW, 0)
        val currentMa = rawCurrent / 1000   // µA → mA

        // chargeCounter in µAh; used to estimate max capacity (see below).
        val chargeCounter = batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER, 0)

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = rawTemp / 10f     // Android stores temperature as tenths of degrees Celsius.

        // Health needs no permissions.
        val healthCode = intent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val hardwareHealth = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> ""
        }

        // Max capacity estimate: chargeCounter (µAh) / (percentage / 100) → mAh.
        // Only computed when percentage is in a reliable range (5–99 %) to avoid division
        // edge cases and wildly inaccurate readings near full or empty.
        val maxCapacityMah = if (chargeCounter > 0 && percentage in 5..99) {
            (chargeCounter / (percentage / 100.0) / 1000.0).toInt()
        } else 0

        val rawStatus = intent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        val rawPlugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val chargeStatus = when (rawStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargeStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeStatus.NOT_CHARGING
            else -> ChargeStatus.UNKNOWN
        }

        val pluggedType = when (rawPlugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> PluggedType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PluggedType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedType.WIRELESS
            else -> PluggedType.NONE
        }

        // isCharging is true for both CHARGING and FULL states, driving the blue color in the UI.
        val isCharging = chargeStatus == ChargeStatus.CHARGING || chargeStatus == ChargeStatus.FULL

        return BatteryState(
            percentage = percentage,
            currentMa = currentMa,
            voltageMv = voltageMv,
            temperatureCelsius = tempCelsius,
            isCharging = isCharging,
            chargeStatus = chargeStatus,
            pluggedType = pluggedType,
            chargeCounter = chargeCounter,
            cycleCount = readCycleCount(),   // called on every update; sysfs is fast (< 1 ms).
            maxCapacityMah = maxCapacityMah,
            hardwareHealth = hardwareHealth
        )
    }

    /**
     * Wraps [BatteryManager.getIntProperty] with SecurityException handling.
     * CURRENT_NOW, CHARGE_COUNTER, CYCLE_COUNT require BATTERY_STATS on Android 16+.
     * Returns [default] instead of throwing when the permission is missing.
     */
    private fun batteryIntProperty(property: Int, default: Int): Int =
        try {
            batteryManager.getIntProperty(property)
        } catch (_: SecurityException) {
            default
        }
}
