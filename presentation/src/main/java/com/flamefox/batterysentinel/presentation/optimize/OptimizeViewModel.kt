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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptimizeUiState(
    val brightness: Int = 127,
    val screenTimeout: Int = 30000,
    val isAdaptiveBrightness: Boolean = false,
    val isBatterySaverEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val dozeStatus: String = "default",
    val hasWriteSettingsPermission: Boolean = false,
    val hasWriteSecureSettingsPermission: Boolean = false
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
        _uiState.value = OptimizeUiState(
            brightness = controlBrightnessUseCase.getCurrentBrightness(),
            screenTimeout = controlBrightnessUseCase.getScreenTimeout(),
            isAdaptiveBrightness = controlBrightnessUseCase.isAdaptiveBrightnessEnabled(),
            isBatterySaverEnabled = toggleBatterySaverUseCase.isEnabled(),
            isSyncEnabled = settingsRepository.isSyncEnabled(),
            dozeStatus = toggleBatterySaverUseCase.getDozeStatus(),
            hasWriteSettingsPermission = controlBrightnessUseCase.hasPermission(),
            hasWriteSecureSettingsPermission = toggleBatterySaverUseCase.hasPermission()
        )
    }

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            controlBrightnessUseCase.setBrightness(value)
            _uiState.value = _uiState.value.copy(brightness = value)
        }
    }

    fun setAdaptiveBrightness(enabled: Boolean) {
        viewModelScope.launch {
            controlBrightnessUseCase.setAdaptiveBrightness(enabled)
            _uiState.value = _uiState.value.copy(isAdaptiveBrightness = enabled)
        }
    }

    fun setScreenTimeout(ms: Int) {
        viewModelScope.launch {
            controlBrightnessUseCase.setScreenTimeout(ms)
            _uiState.value = _uiState.value.copy(screenTimeout = ms)
        }
    }

    fun toggleBatterySaver() {
        viewModelScope.launch {
            val success = toggleBatterySaverUseCase.toggle()
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isBatterySaverEnabled = !_uiState.value.isBatterySaverEnabled
                )
            }
        }
    }

    fun setSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSync(enabled)
            _uiState.value = _uiState.value.copy(isSyncEnabled = enabled)
        }
    }
}
