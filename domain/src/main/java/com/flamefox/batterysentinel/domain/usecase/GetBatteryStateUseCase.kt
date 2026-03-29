package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow

class GetBatteryStateUseCase(private val repository: BatteryRepository) {
    operator fun invoke(): Flow<BatteryState> = repository.observeBatteryState()
}
