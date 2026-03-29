package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import kotlinx.coroutines.flow.first

class DetectAnomalyUseCase(
    private val batteryRepository: BatteryRepository,
    private val settingsRepository: SystemSettingsRepository
) {
    data class AnomalyResult(
        val isAnomaly: Boolean,
        val currentDrainPercentPerHour: Float,
        val baselineDrainPercentPerHour: Float,
        val multiplier: Float
    )

    suspend operator fun invoke(currentScreenOffDrain: Float): AnomalyResult {
        val settings = settingsRepository.getAppSettings().first()
        val baseline = batteryRepository.getSevenDayAverageDrainRate()
        val multiplier = settings.anomalyMultiplier

        return AnomalyResult(
            isAnomaly = baseline > 0 && currentScreenOffDrain > baseline * multiplier,
            currentDrainPercentPerHour = currentScreenOffDrain,
            baselineDrainPercentPerHour = baseline,
            multiplier = multiplier
        )
    }
}
