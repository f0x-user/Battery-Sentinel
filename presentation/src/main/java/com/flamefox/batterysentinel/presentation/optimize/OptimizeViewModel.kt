package com.flamefox.batterysentinel.presentation.optimize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.usecase.ControlBrightnessUseCase
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptimizeUiState(
    // Current system state
    val brightness: Int = 127,
    val screenTimeout: Int = 30000,
    val isAdaptiveBrightness: Boolean = false,
    val isBatterySaverEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false,
    // Pending (user-configured, not yet applied)
    val pendingBrightness: Int = 127,
    val pendingScreenTimeout: Int = 30000,
    val pendingIsAdaptiveBrightness: Boolean = false,
    val pendingIsSyncEnabled: Boolean = true,
    // Run button state
    val hasPendingChanges: Boolean = false,
    val isApplying: Boolean = false
)

@HiltViewModel
class OptimizeViewModel @Inject constructor(
    private val controlBrightnessUseCase: ControlBrightnessUseCase,
    private val settingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizeUiState())
    val uiState: StateFlow<OptimizeUiState> = _uiState.asStateFlow()

    init { refreshState() }

    fun refreshState() {
        val brightness = controlBrightnessUseCase.getCurrentBrightness()
        val timeout = controlBrightnessUseCase.getScreenTimeout()
        val adaptive = controlBrightnessUseCase.isAdaptiveBrightnessEnabled()
        val sync = settingsRepository.isSyncEnabled()

        _uiState.value = OptimizeUiState(
            brightness = brightness,
            screenTimeout = timeout,
            isAdaptiveBrightness = adaptive,
            isBatterySaverEnabled = settingsRepository.isBatterySaverEnabled(),
            isSyncEnabled = sync,
            isIgnoringBatteryOptimizations = settingsRepository.isIgnoringBatteryOptimizations(),
            hasWriteSettingsPermission = controlBrightnessUseCase.hasPermission(),
            pendingBrightness = brightness,
            pendingScreenTimeout = timeout,
            pendingIsAdaptiveBrightness = adaptive,
            pendingIsSyncEnabled = sync,
            hasPendingChanges = false,
            isApplying = false
        )
    }

    fun setBrightness(value: Int) {
        _uiState.update { it.copy(pendingBrightness = value, hasPendingChanges = true) }
    }

    fun setAdaptiveBrightness(enabled: Boolean) {
        _uiState.update { it.copy(pendingIsAdaptiveBrightness = enabled, hasPendingChanges = true) }
    }

    fun setScreenTimeout(ms: Int) {
        _uiState.update { it.copy(pendingScreenTimeout = ms, hasPendingChanges = true) }
    }

    fun setSync(enabled: Boolean) {
        _uiState.update { it.copy(pendingIsSyncEnabled = enabled, hasPendingChanges = true) }
    }

    fun runOptimizations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val s = _uiState.value

            if (s.hasWriteSettingsPermission) {
                controlBrightnessUseCase.setAdaptiveBrightness(s.pendingIsAdaptiveBrightness)
                if (!s.pendingIsAdaptiveBrightness) {
                    controlBrightnessUseCase.setBrightness(s.pendingBrightness)
                }
                controlBrightnessUseCase.setScreenTimeout(s.pendingScreenTimeout)
            }

            settingsRepository.setSync(s.pendingIsSyncEnabled)

            refreshState()
        }
    }
}
