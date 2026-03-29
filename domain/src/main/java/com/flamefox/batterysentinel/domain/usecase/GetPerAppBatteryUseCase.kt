package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository

class GetPerAppBatteryUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(): List<AppUsage> =
        if (repository.hasBatteryStatsPermission()) {
            repository.getPerAppBatteryUsage().sortedByDescending { it.batteryMah }
        } else {
            emptyList()
        }
}
