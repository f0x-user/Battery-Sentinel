package com.flamefox.batterysentinel.presentation.charging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.toFormattedDateTime
import com.flamefox.batterysentinel.core.common.toFormattedDuration
import com.flamefox.batterysentinel.core.ui.theme.BatteryGreen
import com.flamefox.batterysentinel.core.ui.theme.BatteryOrange
import com.flamefox.batterysentinel.core.ui.theme.BatteryRed
import com.flamefox.batterysentinel.core.ui.theme.ChargingBlue
import com.flamefox.batterysentinel.domain.model.ChargingSession
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun ChargingScreen(viewModel: ChargingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Active charging status
        state.currentBattery?.let { battery ->
            if (battery.isCharging) {
                item {
                    ActiveChargingCard(
                        percentage = battery.percentage,
                        currentMa = battery.currentMa,
                        voltageMv = battery.voltageMv
                    )
                }
            }
        }

        // Battery health
        item {
            HealthCard(
                healthPercent = state.averageHealthPercent,
                sessionCount = state.sessions.count { it.isComplete }
            )
        }

        // Statistics
        if (state.sessions.isNotEmpty()) {
            item { StatsCard(sessions = state.sessions) }
        }

        // Health trend
        val healthSessions = state.sessions.filter { it.estimatedHealthPercent != null }
        if (healthSessions.size >= 2) {
            item { HealthTrendChart(sessions = healthSessions) }
        }

        // Session history header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Charging History", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state.sessions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No charging sessions yet. Plug in your charger to start recording.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.sessions) { session ->
                ChargingSessionCard(session)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ActiveChargingCard(percentage: Int, currentMa: Int, voltageMv: Int) {
    val watt = remember(currentMa, voltageMv) {
        (currentMa.toFloat() * voltageMv.toFloat() / 1_000_000f)
    }
    val remainingPercent = 100 - percentage
    val estimatedMinutes = if (currentMa > 0 && remainingPercent > 0) {
        val ratePerMinute = currentMa.toFloat() / 60_000f
        if (ratePerMinute > 0) (remainingPercent / ratePerMinute).toInt() else null
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChargingBlue.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.BatteryChargingFull,
                    contentDescription = null,
                    tint = ChargingBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Charging",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChargingBlue
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChargingInfoItem(
                    label = "Level",
                    value = "$percentage%",
                    icon = Icons.Filled.BatteryFull
                )
                ChargingInfoItem(
                    label = "Power",
                    value = if (watt > 0) "%.1f W".format(watt) else "—",
                    icon = Icons.Filled.Speed
                )
                if (estimatedMinutes != null && estimatedMinutes > 0) {
                    ChargingInfoItem(
                        label = "ETA (est.)",
                        value = "${estimatedMinutes}m",
                        icon = Icons.Filled.BatteryChargingFull
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = ChargingBlue
            )
        }
    }
}

@Composable
private fun ChargingInfoItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = ChargingBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = ChargingBlue)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthCard(healthPercent: Float?, sessionCount: Int) {
    val color = when {
        healthPercent == null -> MaterialTheme.colorScheme.primary
        healthPercent >= 80f -> BatteryGreen
        healthPercent >= 60f -> BatteryOrange
        else -> BatteryRed
    }
    val statusText = when {
        healthPercent == null -> "No Data"
        healthPercent >= 80f -> "Good"
        healthPercent >= 60f -> "Fair"
        else -> "Poor"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Battery Health",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$sessionCount completed charging sessions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (healthPercent != null) "%.1f%%".format(healthPercent) else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = color
                    )
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.small,
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (healthPercent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (healthPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Estimate based on charge cycles · Device: Pixel 8 Pro (5050 mAh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Complete a full charging session to estimate battery health.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsCard(sessions: List<ChargingSession>) {
    val completed = sessions.filter { it.isComplete }
    val avgDurationMs = completed.mapNotNull { it.durationMs }.takeIf { it.isNotEmpty() }?.average()?.toLong()
    val avgMah = completed.mapNotNull { it.mAhDelivered }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val avgStartPct = completed.map { it.startPercent }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val avgEndPct = completed.mapNotNull { it.endPercent }.takeIf { it.isNotEmpty() }?.average()?.toInt()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Sessions", "${completed.size}")
                StatItem("Avg Duration", avgDurationMs?.toFormattedDuration() ?: "—")
                StatItem("Avg mAh", avgMah?.let { "%.0f".format(it) } ?: "—")
                StatItem("Avg Start→End", if (avgStartPct != null && avgEndPct != null) "$avgStartPct→$avgEndPct%" else "—")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthTrendChart(sessions: List<ChargingSession>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Health Trend (last ${minOf(sessions.size, 10)} sessions)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            val data = remember(sessions) {
                sessions.takeLast(10).mapNotNull { it.estimatedHealthPercent }
            }
            val model = remember(data) {
                entryModelOf(data.mapIndexed { i, v ->
                    com.patrykandpatrick.vico.core.entry.FloatEntry(i.toFloat(), v)
                })
            }

            Chart(
                chart = lineChart(),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.height(140.dp)
            )
        }
    }
}

@Composable
private fun ChargingSessionCard(session: ChargingSession) {
    val durationColor = if (session.isComplete) MaterialTheme.colorScheme.primary else BatteryGreen
    val healthColor = session.estimatedHealthPercent?.let { h ->
        when {
            h >= 80f -> BatteryGreen
            h >= 60f -> BatteryOrange
            else -> BatteryRed
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    session.startTime.toFormattedDateTime(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.isComplete) {
                    Text(
                        session.durationMs!!.toFormattedDuration(),
                        style = MaterialTheme.typography.labelMedium,
                        color = durationColor
                    )
                } else {
                    Text("In Progress...", style = MaterialTheme.typography.labelMedium, color = BatteryGreen)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${session.startPercent}% → ${session.endPercent ?: "?"}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                session.mAhDelivered?.let {
                    Text(
                        "%.0f mAh".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                session.estimatedHealthPercent?.let {
                    Text(
                        "Health: %.1f%%".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = healthColor ?: MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
