package com.flamefox.batterysentinel.domain.usecase

import app.cash.turbine.test
import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetDrainRateUseCaseTest {

    private val repository: BatteryRepository = mockk()
    private val useCase = GetDrainRateUseCase(repository)

    @Test
    fun `invoke returns drain rate from repository`() = runTest {
        val rate = DrainRate(
            percentPerHour = 5f,
            screenOnPercentPerHour = 8f,
            screenOffPercentPerHour = 2f
        )
        every { repository.getDrainRate() } returns flowOf(rate)

        useCase().test {
            val item = awaitItem()
            assertEquals(5f, item.percentPerHour)
            assertEquals(8f, item.screenOnPercentPerHour)
            assertEquals(2f, item.screenOffPercentPerHour)
            awaitComplete()
        }
    }
}
