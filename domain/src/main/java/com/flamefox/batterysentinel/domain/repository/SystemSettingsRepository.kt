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
    fun isIgnoringBatteryOptimizations(): Boolean
    fun hasWriteSettingsPermission(): Boolean
    fun hasWriteSecureSettingsPermission(): Boolean

    // ── Backup / Restore ────────────────────────────────────────────────────────────────────
    /** Saves current system settings as a new backup slot (circular, max 5). */
    suspend fun saveSystemBackup()
    /** Restores the latest backup (convenience wrapper around [restoreSystemBackupByBackup]). */
    suspend fun restoreSystemBackup(): Boolean
    /**
     * Restores a specific [backup] object. Returns true when all settings were applied
     * successfully; false when at least one setting could not be written (partial restore,
     * e.g. WRITE_SETTINGS permission was revoked between backup and restore).
     */
    suspend fun restoreSystemBackupByBackup(backup: SystemBackup): Boolean
    fun getSystemBackup(): Flow<SystemBackup?>
    /** Returns all stored backups sorted newest-first. Used by SettingsScreen RadioButton list. */
    fun getAllSystemBackups(): Flow<List<SystemBackup>>

    // DSGVO: Alle Nutzerdaten löschen
    suspend fun clearAllUserData()
}
