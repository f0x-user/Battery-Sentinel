package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetBatteryHealthUseCaseTest {

    private val repository: ChargingSessionRepository = mockk()
    private val useCase = GetBatteryHealthUseCase(repository)

    @Test
    fun `health estimate for perfect capacity returns 100 percent`() {
        // 5050 mAh delivered from 0 to 100%
        val health = useCase.estimateHealthFromSession(5050f, 0, 100)
        assertEquals(100f, health, 0.1f)
    }

    @Test
    fun `health estimate for half capacity returns 50 percent`() {
        // 2525 mAh delivered from 0 to 100%
        val health = useCase.estimateHealthFromSession(2525f, 0, 100)
        assertEquals(50f, health, 0.1f)
    }

    @Test
    fun `health estimate for partial charge extrapolates correctly`() {
        // 2525 mAh from 0 to 50% → full cap ≈ 5050 → 100% health
        val health = useCase.estimateHealthFromSession(2525f, 0, 50)
        assertEquals(100f, health, 0.1f)
    }

    @Test
    fun `health is clamped to 100 percent max`() {
        val health = useCase.estimateHealthFromSession(6000f, 0, 100)
        assertEquals(100f, health, 0.1f)
    }

    @Test
    fun `health is clamped to 0 percent min`() {
        val health = useCase.estimateHealthFromSession(0f, 50, 60)
        assertEquals(0f, health, 0.1f)
    }
}
