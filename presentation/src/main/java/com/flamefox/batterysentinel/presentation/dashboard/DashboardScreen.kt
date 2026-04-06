package com.flamefox.batterysentinel.presentation.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.toPercentPerHourString
import com.flamefox.batterysentinel.core.common.toSignedCurrentString
import com.flamefox.batterysentinel.core.common.toTemperatureString
import com.flamefox.batterysentinel.core.ui.components.BatteryRingChart
import com.flamefox.batterysentinel.core.ui.components.StatCard
import com.flamefox.batterysentinel.core.ui.theme.BatteryGreen
import com.flamefox.batterysentinel.core.ui.theme.BatteryOrange
import com.flamefox.batterysentinel.core.ui.theme.BatteryRed
import com.flamefox.batterysentinel.core.ui.theme.BatteryYellow
import com.flamefox.batterysentinel.core.ui.theme.ChargingBlue
import com.flamefox.batterysentinel.core.ui.theme.TempWarnOrange
import com.flamefox.batterysentinel.domain.model.ChargeStatus

/**
 * Main dashboard screen. Displays real-time battery metrics in a scrollable grid of StatCards.
 *
 * Data flow: BatteryMonitorService → BatteryRepository → DashboardViewModel → DashboardUiState → here.
 * The ViewModel collects two flows: battery state (every 1 s) and drain rate (every 60 s).
 *
 * Removed tiles (v1.1.4): Screen-On Drain, Screen-Off Drain — too noisy, data needs longer
 * measurement window to be meaningful. Overall Drain Rate tile remains.
 */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    // Show a centered spinner until the first battery state arrives from the service.
    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text("Starting battery monitor...", modifier = Modifier.padding(top = 8.dp))
        }
        return
    }

    val battery = state.batteryState

    // Animate ring color based on charge level and charging state.
    // Charging always shows blue; discharging transitions green → yellow → orange → red.
    val batteryColor by animateColorAsState(
        targetValue = when {
            battery.isCharging -> ChargingBlue
            battery.percentage > 50 -> BatteryGreen
            battery.percentage > 20 -> BatteryYellow
            battery.percentage > 10 -> BatteryOrange
            else -> BatteryRed
        },
        label = "batteryColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Battery Dashboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Circular ring chart — percentage and charge status label inside the ring.
        BatteryRingChart(
            percentage = battery.percentage,
            size = 200.dp,
            strokeWidth = 20.dp,
            progressColor = batteryColor,
            centerContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${battery.percentage}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = batteryColor
                    )
                    Text(
                        text = when (battery.chargeStatus) {
                            ChargeStatus.CHARGING -> "Charging"
                            ChargeStatus.FULL -> "Full"
                            ChargeStatus.DISCHARGING -> "Discharging"
                            ChargeStatus.NOT_CHARGING -> "Not Charging"
                            ChargeStatus.UNKNOWN -> "Unknown"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = batteryColor
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Row 1: Current (signed mA, blue when charging) and Voltage (mV).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Current",
                value = battery.currentMa.toSignedCurrentString(),
                modifier = Modifier.weight(1f),
                // Positive current = charging (shown in blue), negative = discharging.
                valueColor = if (battery.isCharging) ChargingBlue else MaterialTheme.colorScheme.onSurface
            )
            StatCard(
                label = "Voltage",
                value = "${battery.voltageMv} mV",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Temperature (orange warning above 40 °C) and hardware charge cycle count.
        // Cycle count reads sysfs first, then BatteryManager property 9 (hidden API).
        // Shows "—" when unavailable (returns -1 or 0 from data layer).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Temperature",
                value = battery.temperatureCelsius.toTemperatureString(),
                modifier = Modifier.weight(1f),
                valueColor = if (battery.temperatureCelsius >= 40f) TempWarnOrange
                else MaterialTheme.colorScheme.onSurface
            )
            StatCard(
                label = "Charge Cycles",
                // cycleCount > 0 means a real value was read; 0 or -1 means unavailable.
                value = if (battery.cycleCount > 0) "${battery.cycleCount}" else "—",
                modifier = Modifier.weight(1f),
                unit = "hardware"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Hardware health string (mapped from BatteryManager health codes) and
        // estimated max capacity derived from charge counter ÷ current percentage.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Health Status",
                value = battery.hardwareHealth.ifEmpty { "—" },
                modifier = Modifier.weight(1f),
                valueColor = when (battery.hardwareHealth.lowercase()) {
                    "good" -> BatteryGreen
                    "overheat", "cold" -> BatteryOrange
                    "dead", "over voltage", "overvoltage", "failure" -> BatteryRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            StatCard(
                label = "Max Capacity",
                value = if (battery.maxCapacityMah > 0) "${battery.maxCapacityMah} mAh" else "—",
                modifier = Modifier.weight(1f),
                // "est." label makes clear this is a calculated approximation, not a hardware read.
                unit = if (battery.maxCapacityMah > 0) "est." else ""
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: Power source (AC / USB / Wireless / None) and overall drain rate in %/h.
        // Drain rate is computed from BatterySample history over the last 60 minutes.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Plugged",
                value = battery.pluggedType.name,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Drain Rate",
                value = state.drainRate.percentPerHour.toPercentPerHourString(),
                modifier = Modifier.weight(1f)
            )
        }

    }
}
