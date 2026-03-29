package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository

class ControlBrightnessUseCase(private val repository: SystemSettingsRepository) {
    fun getCurrentBrightness(): Int = repository.getBrightness()
    suspend fun setBrightness(value: Int): Boolean = repository.setBrightness(value.coerceIn(0, 255))
    fun isAdaptiveBrightnessEnabled(): Boolean = repository.isAdaptiveBrightnessEnabled()
    suspend fun setAdaptiveBrightness(enabled: Boolean): Boolean = repository.setAdaptiveBrightness(enabled)
    fun getScreenTimeout(): Int = repository.getScreenTimeout()
    suspend fun setScreenTimeout(value: Int): Boolean = repository.setScreenTimeout(value)
    fun hasPermission(): Boolean = repository.hasWriteSettingsPermission()
}
