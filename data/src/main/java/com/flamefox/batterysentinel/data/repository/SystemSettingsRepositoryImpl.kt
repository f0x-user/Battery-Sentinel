package com.flamefox.batterysentinel.data.repository

import com.flamefox.batterysentinel.data.datastore.AppSettingsDataStore
import com.flamefox.batterysentinel.data.datastore.SystemBackupDataStore
import com.flamefox.batterysentinel.data.source.SystemSettingsDataSource
import com.flamefox.batterysentinel.domain.model.AppSettings
import com.flamefox.batterysentinel.domain.model.SystemBackup
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsRepositoryImpl @Inject constructor(
    private val dataSource: SystemSettingsDataSource,
    private val dataStore: AppSettingsDataStore,
    private val backupStore: SystemBackupDataStore
) : SystemSettingsRepository {

    override fun getAppSettings(): Flow<AppSettings> = dataStore.settings
    override suspend fun updateAppSettings(settings: AppSettings) = dataStore.updateSettings(settings)

    override fun getBrightness(): Int = dataSource.getBrightness()
    override suspend fun setBrightness(value: Int): Boolean = dataSource.setBrightness(value)
    override fun getScreenTimeout(): Int = dataSource.getScreenTimeout()
    override suspend fun setScreenTimeout(value: Int): Boolean = dataSource.setScreenTimeout(value)
    override fun isAdaptiveBrightnessEnabled(): Boolean = dataSource.isAdaptiveBrightnessEnabled()
    override suspend fun setAdaptiveBrightness(enabled: Boolean): Boolean = dataSource.setAdaptiveBrightness(enabled)
    override fun isBatterySaverEnabled(): Boolean = dataSource.isBatterySaverEnabled()
    override suspend fun setBatterySaver(enabled: Boolean): Boolean = dataSource.setBatterySaver(enabled)
    override fun isSyncEnabled(): Boolean = dataSource.isSyncEnabled()
    override suspend fun setSync(enabled: Boolean) = dataSource.setSync(enabled)
    override fun getDozeStatus(): String = dataSource.getDozeStatus()
    override suspend fun setDozeConstants(constants: String): Boolean = dataSource.setDozeConstants(constants)
    override fun hasWriteSettingsPermission(): Boolean = dataSource.hasWriteSettingsPermission()
    override fun hasWriteSecureSettingsPermission(): Boolean = dataSource.hasWriteSecureSettingsPermission()

    override suspend fun saveSystemBackup() {
        backupStore.saveBackup(
            SystemBackup(
                brightness = dataSource.getBrightness(),
                isAdaptiveBrightness = dataSource.isAdaptiveBrightnessEnabled(),
                screenTimeoutMs = dataSource.getScreenTimeout(),
                isBatterySaverEnabled = dataSource.isBatterySaverEnabled(),
                isSyncEnabled = dataSource.isSyncEnabled(),
                dozeConstants = dataSource.getDozeStatus(),
                savedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun restoreSystemBackup(): Boolean {
        var allOk = true
        val b = backupStore.backup.first() ?: return false

        if (!dataSource.setAdaptiveBrightness(b.isAdaptiveBrightness)) allOk = false
        if (!dataSource.setBrightness(b.brightness)) allOk = false
        if (!dataSource.setScreenTimeout(b.screenTimeoutMs)) allOk = false
        if (!dataSource.setBatterySaver(b.isBatterySaverEnabled)) allOk = false
        dataSource.setSync(b.isSyncEnabled)
        if (b.dozeConstants != "default" && b.dozeConstants != "unknown") {
            if (!dataSource.setDozeConstants(b.dozeConstants)) allOk = false
        }
        return allOk
    }

    override fun getSystemBackup(): Flow<SystemBackup?> = backupStore.backup
}
