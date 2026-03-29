package com.flamefox.batterysentinel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.flamefox.batterysentinel.core.common.Constants
import com.flamefox.batterysentinel.data.worker.AnomalyDetectionWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BatterySentinelApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleAnomalyDetection()
    }

    private fun scheduleAnomalyDetection() {
        val request = PeriodicWorkRequestBuilder<AnomalyDetectionWorker>(
            Constants.ANOMALY_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "anomaly_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
