package com.flamefox.batterysentinel.presentation.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_VERSION = "1.1.3"

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { viewModel.showAbout() }) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" About", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Permission status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    name = "Usage Statistics",
                    description = "App foreground usage time",
                    granted = state.hasUsageStatsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    grantType = "settings"
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Write Settings",
                    description = "Control brightness & screen timeout",
                    granted = state.hasWriteSettingsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)) },
                    grantType = "settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Battery quick links
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Battery Quick Links", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Battery Settings")
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent("android.settings.BATTERY_USAGE_SETTINGS")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Battery Usage")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Per-app battery consumption is available in the Android system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alert thresholds
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Alert Thresholds", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Charge Alert: ${state.appSettings.chargeAlarmThreshold}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = state.appSettings.chargeAlarmThreshold.toFloat(),
                    onValueChange = { viewModel.updateChargeThreshold(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Temperature Alert: %.0f°C".format(state.appSettings.temperatureAlarmThresholdCelsius),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = state.appSettings.temperatureAlarmThresholdCelsius,
                    onValueChange = { viewModel.updateTemperatureThreshold(it) },
                    valueRange = 35f..55f,
                    steps = 19
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notifications
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notifications")
                    Text(
                        "Charge alerts, temperature warnings, anomalies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.appSettings.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { viewModel.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Permission Status")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System backups
        SystemBackupCard(
            backups = state.allBackups,
            onRestore = { viewModel.restoreSystemBackup() }
        )

        // Restore result dialog
        state.restoreResult?.let { result ->
            AlertDialog(
                onDismissRequest = { viewModel.clearRestoreResult() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearRestoreResult() }) { Text("OK") }
                },
                title = { Text("Restore") },
                text = {
                    Text(
                        when (result) {
                            RestoreResult.SUCCESS -> "All settings were successfully restored."
                            RestoreResult.PARTIAL -> "Settings were partially restored. Some permissions may be missing."
                            RestoreResult.NO_BACKUP -> "No backup found. Restart the app to create a backup."
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // GDPR: Data deletion
        GdprCard(
            onClearData = { viewModel.clearAllData() }
        )

        // About dialog
        if (state.showAbout) {
            AboutDialog(onDismiss = { viewModel.hideAbout() })
        }

        // Data deletion confirmation
        if (state.dataClearSuccess) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDataClearSuccess() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDataClearSuccess() }) { Text("OK") }
                },
                title = { Text("Data Deleted") },
                text = { Text("All locally stored battery data has been successfully deleted.") }
            )
        }
    }
}

@Composable
private fun GdprCard(onClearData: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Privacy (GDPR)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "BatterySentinel stores only local device data (battery values, charging sessions, " +
                "system settings). No data is transmitted to the internet and no third-party " +
                "services are used.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Retention: Battery samples 14 days · Charging sessions 90 days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete All Data (Art. 17 GDPR)")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete All Data?") },
            text = {
                Text(
                    "This will permanently delete all locally stored battery samples, charging sessions, " +
                    "and backup data. App settings will be reset."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onClearData() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    var showPrivacy by remember { mutableStateOf(false) }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } },
            title = { Text("Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("As of: 01.04.2025", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Controller", style = MaterialTheme.typography.labelMedium)
                    Text("FlameFox · com.flamefox.batterysentinel", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("2. Data Collected", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "The app collects only technical device data from your own smartphone: " +
                        "battery level, current, voltage, temperature, charging sessions, and app usage times. " +
                        "No personal data such as name, email, or location is collected.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("3. Storage & Transfer", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "All data is stored exclusively on the device. No data is transmitted to " +
                        "external servers, third parties, or other services.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("4. Retention Periods", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Battery samples: 14 days · Charging sessions: 90 days · " +
                        "System setting backups: until manually deleted (max. 5 entries)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("5. Data Subject Rights (GDPR Art. 15–22)", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Since all data is stored locally, you can permanently delete all collected " +
                        "data at any time via Settings → Delete All Data (Art. 17).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("6. Legal Basis", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Data processing is based on Art. 6(1)(a) GDPR " +
                        "(consent through use of the app).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = { showPrivacy = true }) { Text("Privacy") }
        },
        title = { Text("About BatterySentinel") },
        text = {
            Column {
                Text("Version $APP_VERSION", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Changes in this version:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                ChangelogEntry("1.1.3", listOf(
                    "Onboarding: step icons, localized texts, device name removed",
                    "Dashboard: Sessions tile removed, duplicate Plugged tile fixed",
                    "Settings: ADB section removed, native battery shortcut buttons added"
                ))
                Spacer(modifier = Modifier.height(8.dp))
                ChangelogEntry("1.1.2", listOf(
                    "Charge cycles from sysfs (no permissions needed)",
                    "Dashboard: Charge Cycles and Sessions now in separate tiles",
                    "Optimize: Battery Saver and Doze controls replaced with Settings links",
                    "Settings: Removed WRITE_SECURE_SETTINGS, added Battery Quick Links",
                    "New app icon"
                ))
                Spacer(modifier = Modifier.height(8.dp))
                ChangelogEntry("1.1.0", listOf(
                    "Swipe navigation between tabs",
                    "Charge cycles displayed correctly",
                    "Charging: Redesign with active charging status, ETA, and statistics",
                    "Apps: All entries clickable (opens app settings)",
                    "Apps: New 'Per Cycle' view",
                    "Settings: 5 rotating system backups",
                    "Notifications open the app on tap",
                    "Optimize: Revised battery tips",
                    "GDPR: Data deletion + privacy policy + 90-day retention"
                ))
                Spacer(modifier = Modifier.height(8.dp))
                ChangelogEntry("1.0.0", listOf(
                    "Initial release",
                    "Real-time battery monitoring",
                    "Charging session history",
                    "App usage statistics",
                    "System backup & restore"
                ))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Developed by FlameFox · com.flamefox.batterysentinel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun ChangelogEntry(version: String, entries: List<String>) {
    Text("v$version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    entries.forEach { entry ->
        Text("• $entry", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SystemBackupCard(
    backups: List<com.flamefox.batterysentinel.domain.model.SystemBackup>,
    onRestore: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System Backups", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${backups.size}/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Up to 5 backups are stored. The oldest is automatically overwritten.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (backups.isEmpty()) {
                Text(
                    "No backup yet. A backup will be created automatically on the next app start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                backups.forEachIndexed { index, backup ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == 0)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (index == 0) "Latest Backup" else "Backup ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index == 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(backup.savedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Brightness: ${backup.brightness}  |  " +
                                "Adaptive: ${if (backup.isAdaptiveBrightness) "On" else "Off"}  |  " +
                                "Timeout: ${backup.screenTimeoutMs / 1000}s  |  " +
                                "Battery Saver: ${if (backup.isBatterySaverEnabled) "On" else "Off"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore Latest Backup")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Restore Backup?") },
            text = {
                Text(
                    "All system settings (brightness, screen timeout, battery saver, " +
                    "sync, Doze) will be reset to the last saved state."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onRestore()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PermissionRow(
    name: String,
    description: String,
    granted: Boolean,
    onGrant: (() -> Unit)? = null,
    adbCommand: String? = null,
    onCopy: (() -> Unit)? = null,
    grantType: String = "settings"
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        " $name",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (granted) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!granted) {
                when (grantType) {
                    "settings" -> TextButton(onClick = { onGrant?.invoke() }) { Text("Grant") }
                    "adb" -> TextButton(onClick = { onCopy?.invoke() }) { Text("Copy ADB") }
                }
            }
        }
        if (!granted && adbCommand != null) {
            Text(
                adbCommand,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
