package com.flamefox.batterysentinel.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.model.AppSettings
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val hasUsageStatsPermission: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false,
    val hasBatteryStatsPermission: Boolean = false,
    val hasWriteSecureSettingsPermission: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SystemSettingsRepository,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository.getAppSettings()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(appSettings = settings)
            }
            .launchIn(viewModelScope)

        refreshPermissions()
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = appUsageRepository.hasUsageStatsPermission(),
            hasWriteSettingsPermission = settingsRepository.hasWriteSettingsPermission(),
            hasBatteryStatsPermission = appUsageRepository.hasBatteryStatsPermission(),
            hasWriteSecureSettingsPermission = settingsRepository.hasWriteSecureSettingsPermission()
        )
    }

    fun updateChargeThreshold(value: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.appSettings.copy(chargeAlarmThreshold = value)
            settingsRepository.updateAppSettings(updated)
        }
    }

    fun updateTemperatureThreshold(value: Float) {
        viewModelScope.launch {
            val updated = _uiState.value.appSettings.copy(temperatureAlarmThresholdCelsius = value)
            settingsRepository.updateAppSettings(updated)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.appSettings.copy(notificationsEnabled = enabled)
            settingsRepository.updateAppSettings(updated)
        }
    }
}
