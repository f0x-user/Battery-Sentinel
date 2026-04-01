package com.flamefox.batterysentinel.domain.repository

import com.flamefox.batterysentinel.domain.model.AppSettings
import com.flamefox.batterysentinel.domain.model.SystemBackup
import kotlinx.coroutines.flow.Flow

interface SystemSettingsRepository {
    fun getAppSettings(): Flow<AppSettings>
    suspend fun updateAppSettings(settings: AppSettings)
    fun getBrightness(): Int
    suspend fun setBrightness(value: Int): Boolean
    fun getScreenTimeout(): Int
    suspend fun setScreenTimeout(value: Int): Boolean
    fun isAdaptiveBrightnessEnabled(): Boolean
    suspend fun setAdaptiveBrightness(enabled: Boolean): Boolean
    fun isBatterySaverEnabled(): Boolean
    suspend fun setBatterySaver(enabled: Boolean): Boolean
    fun isSyncEnabled(): Boolean
    suspend fun setSync(enabled: Boolean)
    fun getDozeStatus(): String
    suspend fun setDozeConstants(constants: String): Boolean
    fun hasWriteSettingsPermission(): Boolean
    fun hasWriteSecureSettingsPermission(): Boolean

    // Backup / Restore
    suspend fun saveSystemBackup()
    suspend fun restoreSystemBackup(): Boolean
    fun getSystemBackup(): Flow<SystemBackup?>
    fun getAllSystemBackups(): Flow<List<SystemBackup>>
}
