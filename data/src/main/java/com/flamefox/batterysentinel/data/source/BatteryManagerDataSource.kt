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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryManagerDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val batteryManager: BatteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun observeBatteryState(): Flow<BatteryState> = flow {
        while (true) {
            emit(readCurrentState())
            delay(1000L)
        }
    }

    fun observeBatteryChangedEvents(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(parseBatteryIntent(intent))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun readCurrentState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return parseBatteryIntent(intent)
    }

    private fun parseBatteryIntent(intent: Intent?): BatteryState {
        // Percentage — Intent extras need no permissions and are always available.
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale)
        else batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 0)

        // Current (µA → mA) and charge counter (µAh) require BATTERY_STATS on API 36.
        // Gracefully fall back to 0 when the permission is not granted so the app never
        // crashes — users who grant the permission via ADB get the full values.
        val rawCurrent = batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW, 0)
        val currentMa = rawCurrent / 1000

        val chargeCounter = batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER, 0)

        // Cycle count via property ID 9 (BATTERY_PROPERTY_CYCLE_COUNT, API 28+).
        val cycleCount = batteryIntProperty(9, 0).takeIf { it > 2 } ?: 0

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = rawTemp / 10f

        // Health: EXTRA_HEALTH needs no permissions.
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
            cycleCount = cycleCount,
            maxCapacityMah = maxCapacityMah,
            hardwareHealth = hardwareHealth
        )
    }

    /**
     * Reads a BatteryManager integer property, returning [default] on SecurityException.
     * Some properties (CURRENT_NOW, CHARGE_COUNTER, CYCLE_COUNT) require BATTERY_STATS
     * on Android 16+ and are only available after an explicit ADB grant.
     */
    private fun batteryIntProperty(property: Int, default: Int): Int =
        try {
            batteryManager.getIntProperty(property)
        } catch (e: SecurityException) {
            default
        }
}
