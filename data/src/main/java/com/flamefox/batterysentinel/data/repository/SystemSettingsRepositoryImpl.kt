package com.flamefox.batterysentinel.data.repository

import com.flamefox.batterysentinel.data.datastore.AppSettingsDataStore
import com.flamefox.batterysentinel.data.source.SystemSettingsDataSource
import com.flamefox.batterysentinel.domain.model.AppSettings
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsRepositoryImpl @Inject constructor(
    private val dataSource: SystemSettingsDataSource,
    private val dataStore: AppSettingsDataStore
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
}
