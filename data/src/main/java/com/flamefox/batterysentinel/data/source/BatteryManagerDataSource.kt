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

    private fun readSysfsInt(vararg paths: String): Int {
        for (path in paths) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat $path"))
                val value = process.inputStream.bufferedReader().readLine()?.trim()?.toIntOrNull() ?: 0
                process.destroy()
                if (value > 0) return value
            } catch (_: Exception) { }
        }
        return 0
    }

    private fun readSysfsString(vararg paths: String): String {
        for (path in paths) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat $path"))
                val value = process.inputStream.bufferedReader().readLine()?.trim() ?: ""
                process.destroy()
                if (value.isNotEmpty()) return value
            } catch (_: Exception) { }
        }
        return ""
    }

    private fun readCycleCount(): Int = readSysfsInt(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/power_supply/Battery/cycle_count",
        "/sys/class/power_supply/bms/cycle_count"
    )

    private fun readMaxCapacityMah(): Int {
        val uAh = readSysfsInt(
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/Battery/charge_full"
        )
        return if (uAh > 0) uAh / 1000 else 0
    }

    private fun readDesignCapacityMah(): Int {
        val uAh = readSysfsInt(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/Battery/charge_full_design"
        )
        return if (uAh > 0) uAh / 1000 else 0
    }

    private fun readHardwareHealth(): String = readSysfsString(
        "/sys/class/power_supply/battery/health",
        "/sys/class/power_supply/Battery/health"
    )

    fun readCurrentState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return parseBatteryIntent(intent)
    }

    private fun parseBatteryIntent(intent: Intent?): BatteryState {
        val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = rawCurrent / 1000
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = rawTemp / 10f
        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val cycleCount = readCycleCount()
        val maxCapacityMah = readMaxCapacityMah()
        val designCapacityMah = readDesignCapacityMah()
        val hardwareHealth = readHardwareHealth()
        val rawStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
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
            designCapacityMah = designCapacityMah,
            hardwareHealth = hardwareHealth
        )
    }
}
