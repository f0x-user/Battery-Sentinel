package com.flamefox.batterysentinel.data.repository

import com.flamefox.batterysentinel.data.source.AppUsageDataSource
import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val dataSource: AppUsageDataSource
) : AppUsageRepository {

    override suspend fun getAppUsageStats(periodMs: Long): List<AppUsage> =
        dataSource.getAppUsageStats(periodMs)

    override suspend fun getPerAppBatteryUsage(): List<AppUsage> =
        dataSource.getPerAppBatteryUsage()

    override fun hasUsageStatsPermission(): Boolean =
        dataSource.hasUsageStatsPermission()

    override fun hasBatteryStatsPermission(): Boolean =
        dataSource.hasBatteryStatsPermission()
}
