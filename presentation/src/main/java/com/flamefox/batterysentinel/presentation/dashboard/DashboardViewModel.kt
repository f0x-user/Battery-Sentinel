package com.flamefox.batterysentinel.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.model.PluggedType
import com.flamefox.batterysentinel.domain.usecase.GetBatteryStateUseCase
import com.flamefox.batterysentinel.domain.usecase.GetDrainRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class DashboardUiState(
    val batteryState: BatteryState = BatteryState(0, 0, 0, 0f, false, ChargeStatus.UNKNOWN, PluggedType.NONE, 0),
    val drainRate: DrainRate = DrainRate(0f, 0f, 0f),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getBatteryStateUseCase: GetBatteryStateUseCase,
    private val getDrainRateUseCase: GetDrainRateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeBattery()
        observeDrainRate()
    }

    private fun observeBattery() {
        getBatteryStateUseCase()
            .onEach { state ->
                _uiState.value = _uiState.value.copy(batteryState = state, isLoading = false)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    private fun observeDrainRate() {
        getDrainRateUseCase()
            .onEach { rate ->
                _uiState.value = _uiState.value.copy(drainRate = rate)
            }
            .catch { }
            .launchIn(viewModelScope)
    }

}
