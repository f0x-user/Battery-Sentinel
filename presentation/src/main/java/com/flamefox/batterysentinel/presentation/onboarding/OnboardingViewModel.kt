package com.flamefox.batterysentinel.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val hasNotificationPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false,
    val hasBatteryStatsPermission: Boolean = false,
    val hasWriteSecureSettingsPermission: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SystemSettingsRepository,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = appUsageRepository.hasUsageStatsPermission(),
            hasWriteSettingsPermission = settingsRepository.hasWriteSettingsPermission(),
            hasBatteryStatsPermission = appUsageRepository.hasBatteryStatsPermission(),
            hasWriteSecureSettingsPermission = settingsRepository.hasWriteSecureSettingsPermission()
        )
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            val current = settingsRepository.getAppSettings().first()
            settingsRepository.updateAppSettings(current.copy(onboardingCompleted = true))
        }
    }
}
