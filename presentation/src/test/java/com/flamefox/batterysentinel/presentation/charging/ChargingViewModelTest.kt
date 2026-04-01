package com.flamefox.batterysentinel.presentation.charging

import app.cash.turbine.test
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.flamefox.batterysentinel.domain.model.PluggedType
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import com.flamefox.batterysentinel.domain.usecase.GetBatteryHealthUseCase
import com.flamefox.batterysentinel.domain.usecase.GetChargingSessionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getChargingSessions: GetChargingSessionsUseCase = mockk()
    private val getBatteryHealth: GetBatteryHealthUseCase = mockk()
    private val batteryRepository: BatteryRepository = mockk()

    private val sampleBattery = BatteryState(
        percentage = 72, currentMa = 1200, voltageMv = 4100,
        temperatureCelsius = 28f, isCharging = true,
        chargeStatus = ChargeStatus.CHARGING, pluggedType = PluggedType.USB,
        chargeCounter = 3000
    )

    private val sessions = listOf(
        ChargingSession(1L, 1000L, 4600L, 20, 80, 1000, 4000, 3000f, 89f),
        ChargingSession(2L, 10000L, null, 30, null, 1500, null)
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getChargingSessions(any()) } returns flowOf(sessions)
        every { getBatteryHealth() } returns flowOf(89f)
        every { batteryRepository.observeBatteryState() } returns flowOf(sampleBattery)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sessions are loaded into ui state`() = runTest {
        val viewModel = ChargingViewModel(getChargingSessions, getBatteryHealth, batteryRepository)

        viewModel.uiState.test {
            awaitItem() // initial
            testDispatcher.scheduler.advanceUntilIdle()
            val updated = awaitItem()
            assertEquals(2, updated.sessions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `health is loaded into ui state`() = runTest {
        val viewModel = ChargingViewModel(getChargingSessions, getBatteryHealth, batteryRepository)

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            val updated = expectMostRecentItem()
            assertEquals(89f, updated.averageHealthPercent)
        }
    }
}
