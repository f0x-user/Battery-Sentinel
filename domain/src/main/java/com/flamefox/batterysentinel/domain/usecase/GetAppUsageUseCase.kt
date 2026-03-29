package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository

class GetAppUsageUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(periodMs: Long = 24 * 60 * 60 * 1000L): List<AppUsage> =
        if (repository.hasUsageStatsPermission()) {
            repository.getAppUsageStats(periodMs).sortedByDescending { it.foregroundTimeMs }
        } else {
            emptyList()
        }
}
