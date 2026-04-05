package com.flamefox.batterysentinel.presentation.optimize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.usecase.ControlBrightnessUseCase
import com.flamefox.batterysentinel.domain.usecase.ToggleBatterySaverUseCase
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptimizeUiState(
    // Current system state (what is actually active on the device)
    val brightness: Int = 127,
    val screenTimeout: Int = 30000,
    val isAdaptiveBrightness: Boolean = false,
    val isBatterySaverEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val dozeStatus: String = "default",
    val hasWriteSettingsPermission: Boolean = false,
    val hasWriteSecureSettingsPermission: Boolean = false,
    // Pending state (what the user has configured but not yet applied)
    val pendingBrightness: Int = 127,
    val pendingScreenTimeout: Int = 30000,
    val pendingIsAdaptiveBrightness: Boolean = false,
    val pendingIsBatterySaverEnabled: Boolean = false,
    val pendingIsSyncEnabled: Boolean = true,
    // Run button state
    val hasPendingChanges: Boolean = false,
    val isApplying: Boolean = false
)

@HiltViewModel
class OptimizeViewModel @Inject constructor(
    private val controlBrightnessUseCase: ControlBrightnessUseCase,
    private val toggleBatterySaverUseCase: ToggleBatterySaverUseCase,
    private val settingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizeUiState())
    val uiState: StateFlow<OptimizeUiState> = _uiState.asStateFlow()

    init { refreshState() }

    fun refreshState() {
        val brightness = controlBrightnessUseCase.getCurrentBrightness()
        val timeout = controlBrightnessUseCase.getScreenTimeout()
        val adaptive = controlBrightnessUseCase.isAdaptiveBrightnessEnabled()
        val batterySaver = toggleBatterySaverUseCase.isEnabled()
        val sync = settingsRepository.isSyncEnabled()

        _uiState.value = OptimizeUiState(
            brightness = brightness,
            screenTimeout = timeout,
            isAdaptiveBrightness = adaptive,
            isBatterySaverEnabled = batterySaver,
            isSyncEnabled = sync,
            dozeStatus = toggleBatterySaverUseCase.getDozeStatus(),
            hasWriteSettingsPermission = controlBrightnessUseCase.hasPermission(),
            hasWriteSecureSettingsPermission = toggleBatterySaverUseCase.hasPermission(),
            // Pending starts equal to current — no changes pending
            pendingBrightness = brightness,
            pendingScreenTimeout = timeout,
            pendingIsAdaptiveBrightness = adaptive,
            pendingIsBatterySaverEnabled = batterySaver,
            pendingIsSyncEnabled = sync,
            hasPendingChanges = false,
            isApplying = false
        )
    }

    fun setBrightness(value: Int) {
        _uiState.update { state ->
            state.copy(
                pendingBrightness = value,
                hasPendingChanges = true
            )
        }
    }

    fun setAdaptiveBrightness(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                pendingIsAdaptiveBrightness = enabled,
                hasPendingChanges = true
            )
        }
    }

    fun setScreenTimeout(ms: Int) {
        _uiState.update { state ->
            state.copy(
                pendingScreenTimeout = ms,
                hasPendingChanges = true
            )
        }
    }

    fun toggleBatterySaver() {
        _uiState.update { state ->
            state.copy(
                pendingIsBatterySaverEnabled = !state.pendingIsBatterySaverEnabled,
                hasPendingChanges = true
            )
        }
    }

    fun setSync(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                pendingIsSyncEnabled = enabled,
                hasPendingChanges = true
            )
        }
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

            if (s.hasWriteSecureSettingsPermission) {
                val currentlySaving = toggleBatterySaverUseCase.isEnabled()
                if (currentlySaving != s.pendingIsBatterySaverEnabled) {
                    toggleBatterySaverUseCase.toggle()
                }
            }

            settingsRepository.setSync(s.pendingIsSyncEnabled)

            refreshState()
        }
    }
}
