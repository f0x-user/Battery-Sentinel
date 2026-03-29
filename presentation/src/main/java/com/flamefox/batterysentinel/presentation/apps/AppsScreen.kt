package com.flamefox.batterysentinel.presentation.apps

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import com.flamefox.batterysentinel.core.common.toFormattedDuration
import com.flamefox.batterysentinel.domain.model.AppUsage

@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("App Usage & Battery", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Usage Time") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Battery Use") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> UsageTab(state, viewModel)
            1 -> BatteryTab(state, viewModel)
        }
    }
}

@Composable
private fun UsageTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Usage Stats Permission Required",
            description = "Grant PACKAGE_USAGE_STATS to see per-app foreground time (last 24h).",
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
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
                progress = app.foregroundTimeMs / maxTime.toFloat()
            )
        }
    }
}

@Composable
private fun BatteryTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasBatteryStatsPermission) {
        PermissionPrompt(
            title = "BATTERY_STATS Permission Required",
            description = "Grant via ADB:\nadb shell pm grant com.flamefox.batterysentinel android.permission.BATTERY_STATS",
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
                progress = (app.batteryMah ?: 0f) / maxMah
            )
        }
    }
}

@Composable
private fun AppUsageRow(
    appName: String,
    packageName: String,
    value: String,
    progress: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appName, style = MaterialTheme.typography.bodyMedium)
                    Text(packageName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(value, style = MaterialTheme.typography.labelMedium)
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
                    Button(onClick = onGrant) { Text("Open Settings") }
                }
                Button(onClick = onRefresh) { Text("Refresh") }
            }
        }
    }
}
