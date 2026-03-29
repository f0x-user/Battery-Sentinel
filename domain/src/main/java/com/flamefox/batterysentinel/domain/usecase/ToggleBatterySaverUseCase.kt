package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository

class ToggleBatterySaverUseCase(private val repository: SystemSettingsRepository) {
    fun isEnabled(): Boolean = repository.isBatterySaverEnabled()
    suspend fun toggle(): Boolean {
        if (!repository.hasWriteSecureSettingsPermission()) return false
        return repository.setBatterySaver(!repository.isBatterySaverEnabled())
    }
    fun hasPermission(): Boolean = repository.hasWriteSecureSettingsPermission()
    fun getDozeStatus(): String = repository.getDozeStatus()
    suspend fun setDozeConstants(constants: String): Boolean = repository.setDozeConstants(constants)
}
