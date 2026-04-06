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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.toFormattedDateTime
import com.flamefox.batterysentinel.core.common.toFormattedDuration
import com.flamefox.batterysentinel.domain.model.ChargingSession

/**
 * App usage screen with three tabs:
 *   - Usage Time: foreground time per app in the last 24 h (PACKAGE_USAGE_STATS).
 *   - Battery Usage: same usage-time data + link to Android system battery screen.
 *     Previously required BATTERY_STATS (ADB-only); replaced with user-grantable data.
 *   - Per Cycle: foreground time during a selected past charging session.
 *
 * All three tabs reuse [AppUsageRow] and share the same [AppsUiState] from [AppsViewModel].
 */
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    // Tab index held in local UI state; does not need to survive process death.
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("App Usage & Battery", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Usage Time") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Battery Usage") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Per Cycle") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> UsageTab(state, viewModel)
            1 -> BatteryTab(state, viewModel)
            2 -> PerCycleTab(state, viewModel)
        }
    }
}

/**
 * Displays foreground usage time for each app over the last 24 hours.
 * Data comes from UsageStatsManager via [AppsViewModel.loadData].
 * List is sorted descending by usage time (longest first) by the ViewModel.
 * [progress] for each row is relative to the most-used app (= 1.0f).
 */
@Composable
private fun UsageTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Usage Statistics Permission Required",
            description = "Grant PACKAGE_USAGE_STATS to display app usage time (last 24h).",
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = viewModel::refresh
        )
        return
    }

    // Largest usage time in the list — used to compute relative progress bar widths.
    val maxTime = state.appUsage.maxOfOrNull { it.foregroundTimeMs } ?: 1L

    LazyColumn {
        itemsIndexed(state.appUsage) { index, app ->
            // HorizontalDivider between items (not before the first one).
            if (index > 0) HorizontalDivider()
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

/**
 * Battery Usage tab — previously showed per-app mAh consumption via BATTERY_STATS (ADB-only).
 * Replaced in v1.1.4 with the same usage-time data from PACKAGE_USAGE_STATS (user-grantable).
 * A button at the bottom links to the Android system battery screen for detailed mAh data.
 *
 * The "android.settings.BATTERY_USAGE_SETTINGS" action string is used directly because
 * Settings.ACTION_BATTERY_USAGE_SETTINGS is not a public SDK constant (it exists on most
 * devices since Android 12 but is not formally part of android.provider.Settings).
 */
@Composable
private fun BatteryTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current
    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Usage Statistics Permission Required",
            description = "Grant PACKAGE_USAGE_STATS to display app usage time.",
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = viewModel::refresh
        )
        return
    }

    val maxTime = state.appUsage.maxOfOrNull { it.foregroundTimeMs } ?: 1L

    LazyColumn {
        itemsIndexed(state.appUsage) { index, app ->
            if (index > 0) HorizontalDivider()
            AppUsageRow(
                appName = app.appName,
                packageName = app.packageName,
                value = app.foregroundTimeMs.toFormattedDuration(),
                progress = app.foregroundTimeMs / maxTime.toFloat(),
                onClick = { openAppSettings(context, app.packageName) }
            )
        }
        // Footer: hint text + system deep-link for per-app mAh data.
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Detaillierter Akkuverbrauch pro App ist in den Android-Einstellungen einsehbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    // Intent action string (not a Settings constant) — wrapped in try-catch
                    // because the target activity may not exist on all OEM firmwares.
                    try {
                        context.startActivity(Intent("android.settings.BATTERY_USAGE_SETTINGS"))
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("System-Akkuverbrauch öffnen")
            }
        }
    }
}

/**
 * Shows foreground app usage for a user-selected completed charging session.
 *
 * State machine:
 *   1. No PACKAGE_USAGE_STATS permission → [PermissionPrompt].
 *   2. No completed sessions in the Room database → centered empty state with icon.
 *   3. No session selected yet → session picker list.
 *   4. Session selected → header with session summary + app usage list for that time window.
 *
 * [AppsViewModel.selectSession] triggers a Room + UsageStats query for the selected session's
 * time range and stores the result in [AppsUiState.sessionApps].
 */
@Composable
private fun PerCycleTab(state: AppsUiState, viewModel: AppsViewModel) {
    val context = LocalContext.current

    if (!state.hasUsageStatsPermission) {
        PermissionPrompt(
            title = "Usage Statistics Permission Required",
            description = "Grant PACKAGE_USAGE_STATS for the Per Cycle view.",
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = viewModel::refresh
        )
        return
    }

    // Empty state: no completed sessions recorded yet (app was just installed or data was cleared).
    if (state.sessions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.BatteryUnknown,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Noch keine abgeschlossenen Ladesitzungen aufgezeichnet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        return
    }

    val selected = state.selectedSession

    // No session picked yet: show the list of completed sessions as clickable cards.
    if (selected == null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text(
                    "Select a charging session:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            itemsIndexed(state.sessions) { _, session ->
                SessionPickerCard(session = session, onClick = { viewModel.selectSession(session) })
            }
        }
    } else {
        // Detail view: back button + session summary header + app list for that window.
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Back button clears the selection and returns to the session picker.
                IconButton(onClick = { viewModel.selectSession(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // UsageStats query is in progress — show spinner while waiting.
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.sessionApps.isEmpty()) {
                // UsageStats returned no entries for this time window (e.g. window too short,
                // or UsageStats interval granularity does not cover the session period).
                Text(
                    "No app usage data available for this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTime = state.sessionApps.maxOfOrNull { it.foregroundTimeMs } ?: 1L
                LazyColumn {
                    itemsIndexed(state.sessionApps) { index, app ->
                        if (index > 0) HorizontalDivider()
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

/** Clickable card showing a charging session's start time, percentage range, and duration. */
@Composable
private fun SessionPickerCard(session: ChargingSession, onClick: () -> Unit) {
    androidx.compose.material3.Card(
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

/**
 * Reusable row composable used in all three tabs.
 *
 * Layout:
 *   [app name + progress bar (fills width)] [12 dp gap] [formatted time/value]
 *
 * The [progress] value (0f–1f) is relative to the most-used app in the current list,
 * so the top app always has a full-width bar and others scale proportionally.
 * Tapping the row opens the app's system settings page via [openAppSettings].
 */
@Composable
private fun AppUsageRow(
    appName: String,
    packageName: String,
    value: String,
    progress: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },  // guard against floating-point drift
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Shown when the app lacks a required permission.
 * [onGrant] opens the appropriate system settings page; [onRefresh] re-checks permissions
 * after the user returns from settings.
 */
@Composable
private fun PermissionPrompt(
    title: String,
    description: String,
    onGrant: (() -> Unit)?,
    onRefresh: () -> Unit,
    isAdb: Boolean = false
) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
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

/** Opens the system application details screen for [packageName] (shows permissions, storage, etc.). */
private fun openAppSettings(context: android.content.Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
