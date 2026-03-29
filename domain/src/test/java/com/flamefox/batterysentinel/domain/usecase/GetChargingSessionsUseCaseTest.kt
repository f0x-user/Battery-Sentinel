package com.flamefox.batterysentinel.domain.usecase

import app.cash.turbine.test
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetChargingSessionsUseCaseTest {

    private val repository: ChargingSessionRepository = mockk()
    private val useCase = GetChargingSessionsUseCase(repository)

    private val sessions = listOf(
        ChargingSession(id = 1, startTime = 0L, endTime = 3600000L, startPercent = 20, endPercent = 80, startChargeCounter = 1000, endChargeCounter = 4000, mAhDelivered = 3000f, estimatedHealthPercent = 90f),
        ChargingSession(id = 2, startTime = 7200000L, endTime = null, startPercent = 30, endPercent = null, startChargeCounter = 1500, endChargeCounter = null)
    )

    @Test
    fun `invoke returns sessions from repository`() = runTest {
        every { repository.getRecentSessions(30) } returns flowOf(sessions)

        useCase().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals(1L, list[0].id)
            awaitComplete()
        }
    }

    @Test
    fun `invoke passes limit parameter to repository`() = runTest {
        every { repository.getRecentSessions(10) } returns flowOf(sessions.take(1))

        useCase(limit = 10).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            awaitComplete()
        }
    }
}
