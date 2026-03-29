package com.flamefox.batterysentinel.presentation.charging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.toFormattedDateTime
import com.flamefox.batterysentinel.core.common.toFormattedDuration
import com.flamefox.batterysentinel.core.ui.theme.BatteryGreen
import com.flamefox.batterysentinel.core.ui.theme.BatteryOrange
import com.flamefox.batterysentinel.core.ui.theme.BatteryRed
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Charging History", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            HealthCard(healthPercent = state.averageHealthPercent)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            HealthTrendChart(sessions = state.sessions.filter { it.estimatedHealthPercent != null })
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.sessions.isEmpty()) {
            item {
                Text(
                    "No charging sessions yet. Connect your charger to start tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.sessions) { session ->
                ChargingSessionCard(session)
            }
        }
    }
}

@Composable
private fun HealthCard(healthPercent: Float?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Battery Health", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (healthPercent != null) {
                val color = when {
                    healthPercent >= 80f -> BatteryGreen
                    healthPercent >= 60f -> BatteryOrange
                    else -> BatteryRed
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "%.1f%%".format(healthPercent),
                        style = MaterialTheme.typography.headlineMedium,
                        color = color
                    )
                    Text(
                        when {
                            healthPercent >= 80f -> "Good"
                            healthPercent >= 60f -> "Fair"
                            else -> "Poor"
                        },
                        color = color
                    )
                }
                LinearProgressIndicator(
                    progress = { (healthPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = color
                )
                Text(
                    "Pixel 8 Pro design capacity: 5050 mAh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    "Complete a charging session to estimate health",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthTrendChart(sessions: List<ChargingSession>) {
    if (sessions.size < 2) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Health Trend", style = MaterialTheme.typography.titleSmall)
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
                modifier = Modifier.height(120.dp)
            )
        }
    }
}

@Composable
private fun ChargingSessionCard(session: ChargingSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(session.startTime.toFormattedDateTime(), style = MaterialTheme.typography.labelMedium)
                if (session.isComplete) {
                    Text(
                        session.durationMs!!.toFormattedDuration(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text("In progress...", style = MaterialTheme.typography.labelMedium, color = BatteryGreen)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("${session.startPercent}% → ${session.endPercent ?: "?"}%")
                session.mAhDelivered?.let { Text("%.0f mAh".format(it)) }
                session.estimatedHealthPercent?.let {
                    Text("Health: %.1f%%".format(it))
                }
            }
        }
    }
}
