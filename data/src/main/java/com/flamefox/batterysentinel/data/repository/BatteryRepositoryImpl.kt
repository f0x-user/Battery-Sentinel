package com.flamefox.batterysentinel.data.repository

import com.flamefox.batterysentinel.core.common.Constants
import com.flamefox.batterysentinel.core.database.dao.BatterySampleDao
import com.flamefox.batterysentinel.data.service.BatteryMonitorService
import com.flamefox.batterysentinel.data.source.BatteryManagerDataSource
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val dataSource: BatteryManagerDataSource,
    private val batterySampleDao: BatterySampleDao
) : BatteryRepository {

    override fun observeBatteryState(): Flow<BatteryState> =
        BatteryMonitorService.batteryState.filterNotNull()

    override fun getCurrentBatteryState(): BatteryState? =
        BatteryMonitorService.batteryState.value

    override fun getDrainRate(): Flow<DrainRate> = flow {
        val now = System.currentTimeMillis()
        val cutoff1h = now - 3600_000L
        val screenOnSamples = batterySampleDao.getScreenOnSamplesSince(cutoff1h)
        val screenOffSamples = batterySampleDao.getScreenOffSamplesSince(cutoff1h)

        fun calcRate(samples: List<Pair<Int, Long>>): Float {
            if (samples.size < 2) return 0f
            val sorted = samples.sortedBy { it.second }
            val durationH = (sorted.last().second - sorted.first().second) / 3_600_000f
            if (durationH < 0.01f) return 0f
            val drop = (sorted.first().first - sorted.last().first).toFloat()
            return if (drop > 0) drop / durationH else 0f
        }

        val onRate = calcRate(screenOnSamples.map { it.percentage to it.timestamp })
        val offRate = calcRate(screenOffSamples.map { it.percentage to it.timestamp })
        val allSamples = (screenOnSamples + screenOffSamples).sortedBy { it.timestamp }
        val overallRate = calcRate(allSamples.map { it.percentage to it.timestamp })

        emit(DrainRate(overallRate, onRate, offRate))
    }

    override fun getSevenDayAverageDrainRate(): Float {
        return 0f // Computed async in worker — stub for sync call
    }
}
