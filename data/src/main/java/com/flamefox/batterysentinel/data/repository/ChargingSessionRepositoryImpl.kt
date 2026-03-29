package com.flamefox.batterysentinel.data.repository

import com.flamefox.batterysentinel.core.database.dao.ChargingSessionDao
import com.flamefox.batterysentinel.core.database.entity.ChargingSessionEntity
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingSessionRepositoryImpl @Inject constructor(
    private val dao: ChargingSessionDao
) : ChargingSessionRepository {

    override fun getAllSessions(): Flow<List<ChargingSession>> =
        dao.getAllSessions().map { it.map(::entityToDomain) }

    override suspend fun getSessionById(id: Long): ChargingSession? =
        dao.getSessionById(id)?.let(::entityToDomain)

    override suspend fun insertSession(session: ChargingSession): Long =
        dao.insertSession(domainToEntity(session))

    override suspend fun updateSession(session: ChargingSession) =
        dao.updateSession(domainToEntity(session))

    override suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    override suspend fun getActiveSession(): ChargingSession? =
        dao.getActiveSession()?.let(::entityToDomain)

    override fun getRecentSessions(limit: Int): Flow<List<ChargingSession>> =
        dao.getRecentSessions(limit).map { it.map(::entityToDomain) }

    override fun getAverageHealthPercent(): Flow<Float?> = dao.getAverageHealthPercent()

    private fun entityToDomain(e: ChargingSessionEntity) = ChargingSession(
        id = e.id,
        startTime = e.startTime,
        endTime = e.endTime,
        startPercent = e.startPercent,
        endPercent = e.endPercent,
        startChargeCounter = e.startChargeCounter,
        endChargeCounter = e.endChargeCounter,
        mAhDelivered = e.mAhDelivered,
        estimatedHealthPercent = e.estimatedHealthPercent
    )

    private fun domainToEntity(d: ChargingSession) = ChargingSessionEntity(
        id = d.id,
        startTime = d.startTime,
        endTime = d.endTime,
        startPercent = d.startPercent,
        endPercent = d.endPercent,
        startChargeCounter = d.startChargeCounter,
        endChargeCounter = d.endChargeCounter,
        mAhDelivered = d.mAhDelivered,
        estimatedHealthPercent = d.estimatedHealthPercent
    )
}
