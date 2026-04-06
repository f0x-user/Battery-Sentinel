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

/**
 * UI state for [SettingsScreen].
 *
 * @property appSettings         Persisted user preferences (thresholds, notifications toggle).
 * @property hasUsageStatsPermission  PACKAGE_USAGE_STATS — required for the Apps screen.
 * @property hasWriteSettingsPermission  WRITE_SETTINGS — required for brightness / timeout control.
 * @property systemBackup        Latest system backup (convenience shorthand for [allBackups][0]).
 * @property allBackups          All stored backups, newest first (max 5, from SystemBackupDataStore).
 * @property restoreResult       Result of the last restore attempt; null when no attempt was made.
 * @property showAbout           Controls visibility of the About dialog.
 * @property dataClearSuccess    True immediately after a successful data-deletion; triggers a dialog.
 */
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

/** Result of a backup restore operation, mapped to a user-facing message in the UI. */
enum class RestoreResult { SUCCESS, PARTIAL, NO_BACKUP }

/**
 * ViewModel for [SettingsScreen].
 *
 * Observes three cold flows from [SystemSettingsRepository] at init time:
 *   - app settings (DataStore Preferences)
 *   - latest system backup
 *   - all system backups (list, newest first)
 *
 * Permission checks are not reactive (no flow); they are re-evaluated on demand via
 * [refreshPermissions], called at init and whenever the user taps "Refresh Permission Status".
 */
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

    /**
     * Restores the system backup at [index] in [SettingsUiState.allBackups] (newest-first order).
     * Delegates to [SystemSettingsRepository.restoreSystemBackupByBackup] so the exact backup
     * object is passed through — avoids a second DataStore read on the repository side.
     * Sets [SettingsUiState.restoreResult] to trigger a result dialog in the UI.
     */
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
