package com.flamefox.batterysentinel.domain.usecase

import app.cash.turbine.test
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.model.PluggedType
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetBatteryStateUseCaseTest {

    private val repository: BatteryRepository = mockk()
    private val useCase = GetBatteryStateUseCase(repository)

    private val sampleState = BatteryState(
        percentage = 75,
        currentMa = -500,
        voltageMv = 4100,
        temperatureCelsius = 28.5f,
        isCharging = false,
        chargeStatus = ChargeStatus.DISCHARGING,
        pluggedType = PluggedType.NONE,
        chargeCounter = 3800
    )

    @Test
    fun `invoke returns battery state from repository`() = runTest {
        every { repository.observeBatteryState() } returns flowOf(sampleState)

        useCase().test {
            val item = awaitItem()
            assertEquals(75, item.percentage)
            assertEquals(-500, item.currentMa)
            assertEquals(4100, item.voltageMv)
            awaitComplete()
        }
    }
}
