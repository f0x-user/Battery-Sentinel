package com.flamefox.batterysentinel.presentation.dashboard

import app.cash.turbine.test
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import com.flamefox.batterysentinel.domain.model.DrainRate
import com.flamefox.batterysentinel.domain.model.PluggedType
import com.flamefox.batterysentinel.domain.usecase.GetBatteryStateUseCase
import com.flamefox.batterysentinel.domain.usecase.GetDrainRateUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getDrainRate: GetDrainRateUseCase = mockk()

    private val sampleState = BatteryState(
        percentage = 65, currentMa = -320, voltageMv = 3950,
        temperatureCelsius = 27f, isCharging = false,
        chargeStatus = ChargeStatus.DISCHARGING, pluggedType = PluggedType.NONE,
        chargeCounter = 3000
    )

    private val sampleDrain = DrainRate(4f, 7f, 1.5f)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getBatteryState() } returns flowOf(sampleState)
        every { getDrainRate() } returns flowOf(sampleDrain)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects battery state after collection`() = runTest {
        val viewModel = DashboardViewModel(getBatteryState, getDrainRate)

        viewModel.uiState.test {
            val loading = awaitItem()
            // Initial state is loading
            testDispatcher.scheduler.advanceUntilIdle()
            val updated = awaitItem()
            assertEquals(65, updated.batteryState.percentage)
            assertFalse(updated.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects drain rate after collection`() = runTest {
        val viewModel = DashboardViewModel(getBatteryState, getDrainRate)

        viewModel.uiState.test {
            awaitItem() // initial
            testDispatcher.scheduler.advanceUntilIdle()
            val updated = expectMostRecentItem()
            assertEquals(4f, updated.drainRate.percentPerHour)
        }
    }
}
