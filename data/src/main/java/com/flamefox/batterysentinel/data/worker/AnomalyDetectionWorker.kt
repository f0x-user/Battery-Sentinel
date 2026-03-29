package com.flamefox.batterysentinel.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flamefox.batterysentinel.core.common.Constants
import com.flamefox.batterysentinel.core.database.dao.BatterySampleDao
import com.flamefox.batterysentinel.data.datastore.AppSettingsDataStore
import com.flamefox.batterysentinel.data.service.BatteryNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AnomalyDetectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batterySampleDao: BatterySampleDao,
    private val notificationManager: BatteryNotificationManager,
    private val settingsDataStore: AppSettingsDataStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsDataStore.settings.first()
            if (!settings.notificationsEnabled) return Result.success()

            val now = System.currentTimeMillis()
            val last15MinCutoff = now - (15 * 60 * 1000L)
            val sevenDayCutoff = now - Constants.SEVEN_DAY_MS

            val recentSamples = batterySampleDao.getScreenOffSamplesSince(last15MinCutoff)
            val historicalSamples = batterySampleDao.getScreenOffSamplesSince(sevenDayCutoff)

            if (recentSamples.size < 3 || historicalSamples.size < 10) return Result.success()

            val currentDrain = calculateDrainRate(recentSamples.map { it.percentage to it.timestamp })
            val baselineDrain = calculateDrainRate(historicalSamples.map { it.percentage to it.timestamp })

            if (baselineDrain > 0 && currentDrain > baselineDrain * settings.anomalyMultiplier) {
                notificationManager.showAnomalyNotification(currentDrain, baselineDrain)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun calculateDrainRate(samples: List<Pair<Int, Long>>): Float {
        if (samples.size < 2) return 0f
        val sorted = samples.sortedBy { it.second }
        val first = sorted.first()
        val last = sorted.last()
        val durationHours = (last.second - first.second) / (1000f * 3600f)
        if (durationHours < 0.05f) return 0f
        val percentDrop = (first.first - last.first).toFloat()
        return if (percentDrop > 0) percentDrop / durationHours else 0f
    }
}
