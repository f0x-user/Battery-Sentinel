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

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Current",
                value = battery.currentMa.toSignedCurrentString(),
                modifier = Modifier.weight(1f),
                valueColor = if (battery.isCharging) ChargingBlue else MaterialTheme.colorScheme.onSurface
            )
            StatCard(
                label = "Voltage",
                value = "${battery.voltageMv} mV",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                value = if (battery.cycleCount > 0) "${battery.cycleCount}" else "—",
                modifier = Modifier.weight(1f),
                unit = "hardware"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                unit = if (battery.maxCapacityMah > 0) "est." else ""
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Screen-On Drain",
                value = state.drainRate.screenOnPercentPerHour.toPercentPerHourString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Screen-Off Drain",
                value = state.drainRate.screenOffPercentPerHour.toPercentPerHourString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
