package com.flamefox.batterysentinel.domain.repository

import com.flamefox.batterysentinel.domain.model.ChargingSession
import kotlinx.coroutines.flow.Flow

interface ChargingSessionRepository {
    fun getAllSessions(): Flow<List<ChargingSession>>
    suspend fun getSessionById(id: Long): ChargingSession?
    suspend fun insertSession(session: ChargingSession): Long
    suspend fun updateSession(session: ChargingSession)
    suspend fun deleteSession(id: Long)
    suspend fun getActiveSession(): ChargingSession?
    fun getRecentSessions(limit: Int = 30): Flow<List<ChargingSession>>
    fun getAverageHealthPercent(): Flow<Float?>
}
