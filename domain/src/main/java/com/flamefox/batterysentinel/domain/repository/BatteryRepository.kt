package com.flamefox.batterysentinel.domain.repository

import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.DrainRate
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observeBatteryState(): Flow<BatteryState>
    fun getCurrentBatteryState(): BatteryState?
    fun getDrainRate(): Flow<DrainRate>
    fun getSevenDayAverageDrainRate(): Float
}
