package com.flamefox.batterysentinel.domain.repository

import com.flamefox.batterysentinel.domain.model.AppUsage

interface AppUsageRepository {
    suspend fun getAppUsageStats(periodMs: Long = 24 * 60 * 60 * 1000L): List<AppUsage>
    suspend fun getAppUsageStatsForRange(startTime: Long, endTime: Long): List<AppUsage>
    suspend fun getPerAppBatteryUsage(): List<AppUsage>
    fun hasUsageStatsPermission(): Boolean
    fun hasBatteryStatsPermission(): Boolean
}
