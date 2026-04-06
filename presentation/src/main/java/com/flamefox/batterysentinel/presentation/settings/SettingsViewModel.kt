package com.flamefox.batterysentinel.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.model.AppSettings
import com.flamefox.batterysentinel.domain.model.SystemBackup
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
    val systemBackup: SystemBackup? = null,
    val allBackups: List<SystemBackup> = emptyList(),
    val restoreResult: RestoreResult? = null,
    val showAbout: Boolean = false,
    val dataClearSuccess: Boolean = false
)

enum class RestoreResult { SUCCESS, PARTIAL, NO_BACKUP }

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

        settingsRepository.getSystemBackup()
            .onEach { backup ->
                _uiState.value = _uiState.value.copy(systemBackup = backup)
            }
            .launchIn(viewModelScope)

        settingsRepository.getAllSystemBackups()
            .onEach { backups ->
                _uiState.value = _uiState.value.copy(allBackups = backups)
            }
            .launchIn(viewModelScope)

        refreshPermissions()
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = appUsageRepository.hasUsageStatsPermission(),
            hasWriteSettingsPermission = settingsRepository.hasWriteSettingsPermission()
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

    fun restoreSystemBackup() {
        viewModelScope.launch {
            val backup = _uiState.value.systemBackup
            if (backup == null) {
                _uiState.value = _uiState.value.copy(restoreResult = RestoreResult.NO_BACKUP)
                return@launch
            }
            val allOk = settingsRepository.restoreSystemBackup()
            _uiState.value = _uiState.value.copy(
                restoreResult = if (allOk) RestoreResult.SUCCESS else RestoreResult.PARTIAL
            )
        }
    }

    fun restoreBackup(index: Int) {
        viewModelScope.launch {
            val backups = _uiState.value.allBackups
            if (index < 0 || index >= backups.size) {
                _uiState.value = _uiState.value.copy(restoreResult = RestoreResult.NO_BACKUP)
                return@launch
            }
            val allOk = settingsRepository.restoreSystemBackupByBackup(backups[index])
            _uiState.value = _uiState.value.copy(
                restoreResult = if (allOk) RestoreResult.SUCCESS else RestoreResult.PARTIAL
            )
        }
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(restoreResult = null)
    }

    fun showAbout() {
        _uiState.value = _uiState.value.copy(showAbout = true)
    }

    fun hideAbout() {
        _uiState.value = _uiState.value.copy(showAbout = false)
    }

    fun clearAllData() {
        viewModelScope.launch {
            settingsRepository.clearAllUserData()
            _uiState.value = _uiState.value.copy(dataClearSuccess = true)
        }
    }

    fun dismissDataClearSuccess() {
        _uiState.value = _uiState.value.copy(dataClearSuccess = false)
    }
}
