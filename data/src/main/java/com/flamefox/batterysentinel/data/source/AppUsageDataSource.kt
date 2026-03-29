package com.flamefox.batterysentinel.data.source

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.flamefox.batterysentinel.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun hasUsageStatsPermission(): Boolean {
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasBatteryStatsPermission(): Boolean {
        return try {
            context.checkCallingOrSelfPermission("android.permission.BATTERY_STATS") ==
                    PackageManager.PERMISSION_GRANTED
        } catch (e: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getAppUsageStats(periodMs: Long): List<AppUsage> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - periodMs

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ) ?: return emptyList()

        val pm = context.packageManager
        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(stats.packageName, 0)).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stats.packageName
                }
                AppUsage(
                    packageName = stats.packageName,
                    appName = appName,
                    foregroundTimeMs = stats.totalTimeInForeground
                )
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun getPerAppBatteryUsage(): List<AppUsage> {
        return try {
            val managerClass = Class.forName("android.app.usage.BatteryStatsManager")
            val manager = context.getSystemService("batterystats") ?: return emptyList()

            val getUsageStats = managerClass.getMethod("getBatteryUsageStats")
            val usageStats = getUsageStats.invoke(manager) ?: return emptyList()

            val usageStatsClass = usageStats.javaClass
            val getUidConsumers = usageStatsClass.getMethod("getUidBatteryConsumers")
            val consumers = getUidConsumers.invoke(usageStats) as? List<*> ?: return emptyList()

            val pm = context.packageManager
            consumers.mapNotNull { consumer ->
                if (consumer == null) return@mapNotNull null
                val consumerClass = consumer.javaClass
                val getUid = consumerClass.getMethod("getUid")
                val getConsumedPower = consumerClass.getMethod("getConsumedPower")

                val uid = getUid.invoke(consumer) as? Int ?: return@mapNotNull null
                val consumedPower = (getConsumedPower.invoke(consumer) as? Double) ?: return@mapNotNull null
                if (consumedPower <= 0) return@mapNotNull null

                val packages = pm.getPackagesForUid(uid) ?: return@mapNotNull null
                val packageName = packages.firstOrNull() ?: return@mapNotNull null
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }
                AppUsage(
                    packageName = packageName,
                    appName = appName,
                    foregroundTimeMs = 0L,
                    batteryMah = consumedPower.toFloat(),
                    uid = uid
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
