package com.flamefox.batterysentinel.core.common

object Constants {
    const val PIXEL_8_PRO_DESIGN_CAPACITY_MAH = 5050f
    const val BATTERY_POLL_INTERVAL_MS = 1000L
    const val ANOMALY_CHECK_INTERVAL_MINUTES = 15L
    const val SEVEN_DAY_MS = 7L * 24 * 60 * 60 * 1000
    const val TEMPERATURE_ALARM_CELSIUS = 40f
    const val DEFAULT_CHARGE_ALARM_THRESHOLD = 80

    const val NOTIFICATION_CHANNEL_MONITOR = "battery_monitor"
    const val NOTIFICATION_CHANNEL_ALERTS = "battery_alerts"
    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_CHARGE_ALARM = 1002
    const val NOTIFICATION_ID_TEMP_ALARM = 1003
    const val NOTIFICATION_ID_ANOMALY = 1004

    const val ADB_GRANT_BATTERY_STATS =
        "adb shell pm grant com.flamefox.batterysentinel android.permission.BATTERY_STATS"
    const val ADB_GRANT_WRITE_SECURE =
        "adb shell pm grant com.flamefox.batterysentinel android.permission.WRITE_SECURE_SETTINGS"
}
