package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import kotlinx.coroutines.flow.Flow

class GetChargingSessionsUseCase(private val repository: ChargingSessionRepository) {
    operator fun invoke(limit: Int = 30): Flow<List<ChargingSession>> =
        repository.getRecentSessions(limit)
}
