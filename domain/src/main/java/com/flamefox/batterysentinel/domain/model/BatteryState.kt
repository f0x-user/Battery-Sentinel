package com.flamefox.batterysentinel.domain.model

data class BatteryState(
    val percentage: Int,
    val currentMa: Int,
    val voltageMv: Int,
    val temperatureCelsius: Float,
    val isCharging: Boolean,
    val chargeStatus: ChargeStatus,
    val pluggedType: PluggedType,
    val chargeCounter: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChargeStatus {
    CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN
}

enum class PluggedType {
    AC, USB, WIRELESS, NONE
}
