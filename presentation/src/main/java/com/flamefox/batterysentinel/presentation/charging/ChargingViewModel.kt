package com.flamefox.batterysentinel.presentation.charging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import com.flamefox.batterysentinel.domain.usecase.GetBatteryHealthUseCase
import com.flamefox.batterysentinel.domain.usecase.GetChargingSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ChargingUiState(
    val sessions: List<ChargingSession> = emptyList(),
    val averageHealthPercent: Float? = null,
    val currentBattery: BatteryState? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ChargingViewModel @Inject constructor(
    private val getChargingSessionsUseCase: GetChargingSessionsUseCase,
    private val getBatteryHealthUseCase: GetBatteryHealthUseCase,
    private val batteryRepository: BatteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChargingUiState())
    val uiState: StateFlow<ChargingUiState> = _uiState.asStateFlow()

    init {
        getChargingSessionsUseCase()
            .onEach { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions, isLoading = false)
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)

        getBatteryHealthUseCase()
            .onEach { health ->
                _uiState.value = _uiState.value.copy(averageHealthPercent = health)
            }
            .catch { }
            .launchIn(viewModelScope)

        batteryRepository.observeBatteryState()
            .onEach { battery ->
                _uiState.value = _uiState.value.copy(currentBattery = battery)
            }
            .catch { }
            .launchIn(viewModelScope)
    }
}
