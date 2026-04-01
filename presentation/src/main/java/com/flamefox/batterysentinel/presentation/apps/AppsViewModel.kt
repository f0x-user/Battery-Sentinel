package com.flamefox.batterysentinel.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import com.flamefox.batterysentinel.domain.usecase.GetAppUsageUseCase
import com.flamefox.batterysentinel.domain.usecase.GetPerAppBatteryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val appUsage: List<AppUsage> = emptyList(),
    val perAppBattery: List<AppUsage> = emptyList(),
    val hasUsageStatsPermission: Boolean = false,
    val hasBatteryStatsPermission: Boolean = false,
    val isLoading: Boolean = true,
    val sessions: List<ChargingSession> = emptyList(),
    val selectedSession: ChargingSession? = null,
    val sessionApps: List<AppUsage> = emptyList(),
    val isLoadingSession: Boolean = false
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getAppUsageUseCase: GetAppUsageUseCase,
    private val getPerAppBatteryUseCase: GetPerAppBatteryUseCase,
    private val appUsageRepository: AppUsageRepository,
    private val chargingSessionRepository: ChargingSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        chargingSessionRepository.getRecentSessions(20)
            .onEach { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions.filter { it.isComplete })
            }
            .launchIn(viewModelScope)
    }

    fun refresh() { loadData() }

    fun selectSession(session: ChargingSession?) {
        _uiState.value = _uiState.value.copy(selectedSession = session, sessionApps = emptyList())
        if (session != null) {
            val endTime = session.endTime ?: System.currentTimeMillis()
            loadSessionApps(session.startTime, endTime)
        }
    }

    private fun loadSessionApps(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSession = true)
            val apps = appUsageRepository.getAppUsageStatsForRange(startTime, endTime)
            _uiState.value = _uiState.value.copy(sessionApps = apps, isLoadingSession = false)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val hasUsage = appUsageRepository.hasUsageStatsPermission()
            val hasBattery = appUsageRepository.hasBatteryStatsPermission()

            _uiState.value = _uiState.value.copy(
                hasUsageStatsPermission = hasUsage,
                hasBatteryStatsPermission = hasBattery,
                isLoading = true
            )

            val usage = getAppUsageUseCase()
            val battery = getPerAppBatteryUseCase()

            _uiState.value = _uiState.value.copy(
                appUsage = usage,
                perAppBattery = battery,
                isLoading = false
            )
        }
    }
}
