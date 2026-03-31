package com.flamefox.batterysentinel.domain.model

data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundTimeMs: Long,
    val batteryMah: Float? = null,
    val uid: Int = 0
)

data class DrainRate(
    val percentPerHour: Float,
    val screenOnPercentPerHour: Float,
    val screenOffPercentPerHour: Float,
    val sampledAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val chargeAlarmThreshold: Int = 80,
    val temperatureAlarmThresholdCelsius: Float = 40f,
    val anomalyMultiplier: Float = 2.0f,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)

/**
 * Schnappschuss aller vom App veränderbaren System-Einstellungen.
 * Wird beim App-Start einmalig gespeichert und kann per Button wiederhergestellt werden.
 */
data class SystemBackup(
    val brightness: Int = 127,
    val isAdaptiveBrightness: Boolean = false,
    val screenTimeoutMs: Int = 30000,
    val isBatterySaverEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val dozeConstants: String = "default",
    val savedAt: Long = 0L
)
