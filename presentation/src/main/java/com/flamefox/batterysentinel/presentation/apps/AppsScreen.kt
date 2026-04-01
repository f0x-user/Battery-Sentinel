package com.flamefox.batterysentinel.presentation.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.toFormattedDateTime
import com.flamefox.batterysentinel.core.common.toFormattedDuration
import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.model.ChargingSession

@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("App-Nutzung & Akku", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Nutzungszeit") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Akkuverbrauch") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Pro Zyklus") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> UsageTab(state, viewModel)
            1 -> BatteryTab(state, viewModel)
            2 -> PerCycleTab(state, viewModel)
        }
    }
}

@Composable
private fun UsageTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Nutzungsstatistik-Berechtigung erforderlich",
            description = "Erteile PACKAGE_USAGE_STATS, um die App-Nutzungszeit (letzte 24h) anzuzeigen.",
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = viewModel::refresh
        )
        return
    }

    val maxTime = state.appUsage.maxOfOrNull { it.foregroundTimeMs } ?: 1L

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.appUsage) { app ->
            AppUsageRow(
                appName = app.appName,
                packageName = app.packageName,
                value = app.foregroundTimeMs.toFormattedDuration(),
                progress = app.foregroundTimeMs / maxTime.toFloat(),
                onClick = { openAppSettings(context, app.packageName) }
            )
        }
    }
}

@Composable
private fun BatteryTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasBatteryStatsPermission) {
        PermissionPrompt(
            title = "BATTERY_STATS-Berechtigung erforderlich",
            description = "Erteile über ADB:\nadb shell pm grant com.flamefox.batterysentinel android.permission.BATTERY_STATS",
            onGrant = null,
            onRefresh = viewModel::refresh,
            isAdb = true
        )
        return
    }

    val maxMah = state.perAppBattery.maxOfOrNull { it.batteryMah ?: 0f } ?: 1f

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.perAppBattery) { app ->
            AppUsageRow(
                appName = app.appName,
                packageName = app.packageName,
                value = "%.2f mAh".format(app.batteryMah ?: 0f),
                progress = (app.batteryMah ?: 0f) / maxMah,
                onClick = { openAppSettings(context, app.packageName) }
            )
        }
    }
}

@Composable
private fun PerCycleTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current

    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Nutzungsstatistik-Berechtigung erforderlich",
            description = "Erteile PACKAGE_USAGE_STATS für die Pro-Zyklus-Ansicht.",
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = viewModel::refresh
        )
        return
    }

    if (state.sessions.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Noch keine abgeschlossenen Ladesitzungen vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val selected = state.selectedSession

    if (selected == null) {
        // Session-Liste anzeigen
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text(
                    "Wähle eine Ladesitzung aus:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(state.sessions) { session ->
                SessionPickerCard(session = session, onClick = { viewModel.selectSession(session) })
            }
        }
    } else {
        // App-Nutzung für gewählte Session
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.selectSession(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column {
                    Text(
                        selected.startTime.toFormattedDateTime(),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "${selected.startPercent}% → ${selected.endPercent ?: "?"}%  •  ${selected.durationMs?.toFormattedDuration() ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoadingSession) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.sessionApps.isEmpty()) {
                Text(
                    "Keine App-Nutzungsdaten für diesen Zeitraum verfügbar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTime = state.sessionApps.maxOfOrNull { it.foregroundTimeMs } ?: 1L
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.sessionApps) { app ->
                        AppUsageRow(
                            appName = app.appName,
                            packageName = app.packageName,
                            value = app.foregroundTimeMs.toFormattedDuration(),
                            progress = app.foregroundTimeMs / maxTime.toFloat(),
                            onClick = { openAppSettings(context, app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionPickerCard(session: ChargingSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.startTime.toFormattedDateTime(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${session.startPercent}% → ${session.endPercent ?: "?"}%  •  ${session.durationMs?.toFormattedDuration() ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AppUsageRow(
    appName: String,
    packageName: String,
    value: String,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "App-Details öffnen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    title: String,
    description: String,
    onGrant: (() -> Unit)?,
    onRefresh: () -> Unit,
    isAdb: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onGrant != null) {
                    Button(onClick = onGrant) { Text("Einstellungen öffnen") }
                }
                Button(onClick = onRefresh) { Text("Aktualisieren") }
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
