package com.flamefox.batterysentinel.presentation.optimize

import androidx.lifecycle.ViewModel
import com.flamefox.batterysentinel.domain.usecase.ControlBrightnessUseCase
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class OptimizeUiState(
    val brightness: Int = 127,
    val screenTimeout: Int = 30000,
    val isAdaptiveBrightness: Boolean = false,
    val isBatterySaverEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false
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
        _uiState.value = OptimizeUiState(
            brightness = controlBrightnessUseCase.getCurrentBrightness(),
            screenTimeout = controlBrightnessUseCase.getScreenTimeout(),
            isAdaptiveBrightness = controlBrightnessUseCase.isAdaptiveBrightnessEnabled(),
            isBatterySaverEnabled = settingsRepository.isBatterySaverEnabled(),
            isSyncEnabled = settingsRepository.isSyncEnabled(),
            isIgnoringBatteryOptimizations = settingsRepository.isIgnoringBatteryOptimizations(),
            hasWriteSettingsPermission = controlBrightnessUseCase.hasPermission()
        )
    }
}