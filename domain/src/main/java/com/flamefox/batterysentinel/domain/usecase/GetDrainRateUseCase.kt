package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow

class GetDrainRateUseCase(private val repository: BatteryRepository) {
    operator fun invoke(): Flow<DrainRate> = repository.getDrainRate()
}
