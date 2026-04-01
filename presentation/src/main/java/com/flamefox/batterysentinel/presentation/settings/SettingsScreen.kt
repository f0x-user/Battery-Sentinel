package com.flamefox.batterysentinel.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.flamefox.batterysentinel.core.common.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_VERSION = "1.1.0"

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
            Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { viewModel.showAbout() }) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Über", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Berechtigungsstatus
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Berechtigungen", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    name = "Nutzungsstatistik",
                    description = "App-Nutzungszeit im Vordergrund",
                    granted = state.hasUsageStatsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    grantType = "settings"
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Einstellungen schreiben",
                    description = "Helligkeit & Screen-Timeout steuern",
                    granted = state.hasWriteSettingsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)) },
                    grantType = "settings"
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Akkustatistik",
                    description = "App-bezogener Akkuverbrauch (mAh)",
                    granted = state.hasBatteryStatsPermission,
                    adbCommand = Constants.ADB_GRANT_BATTERY_STATS,
                    grantType = "adb",
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("adb", Constants.ADB_GRANT_BATTERY_STATS))
                    }
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Sichere Einstellungen schreiben",
                    description = "Energiesparmodus & Doze steuern",
                    granted = state.hasWriteSecureSettingsPermission,
                    adbCommand = Constants.ADB_GRANT_WRITE_SECURE,
                    grantType = "adb",
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("adb", Constants.ADB_GRANT_WRITE_SECURE))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schwellenwerte
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Alarmschwellen", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Ladealarm: ${state.appSettings.chargeAlarmThreshold}%",
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
                    "Temperaturalarm: %.0f°C".format(state.appSettings.temperatureAlarmThresholdCelsius),
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

        // Benachrichtigungen
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Benachrichtigungen")
                    Text(
                        "Ladealarme, Temperaturwarnungen, Anomalien",
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
            Text("Berechtigungsstatus aktualisieren")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System-Backups
        SystemBackupCard(
            backups = state.allBackups,
            onRestore = { viewModel.restoreSystemBackup() }
        )

        // Ergebnis-Dialog nach Wiederherstellung
        state.restoreResult?.let { result ->
            AlertDialog(
                onDismissRequest = { viewModel.clearRestoreResult() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearRestoreResult() }) { Text("OK") }
                },
                title = { Text("Wiederherstellung") },
                text = {
                    Text(
                        when (result) {
                            RestoreResult.SUCCESS -> "Alle Einstellungen wurden erfolgreich wiederhergestellt."
                            RestoreResult.PARTIAL -> "Einstellungen wurden teilweise wiederhergestellt. Einige Berechtigungen fehlen möglicherweise."
                            RestoreResult.NO_BACKUP -> "Kein Backup vorhanden. Starte die App neu, um ein Backup zu erstellen."
                        }
                    )
                }
            )
        }

        // About-Dialog
        if (state.showAbout) {
            AboutDialog(onDismiss = { viewModel.hideAbout() })
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
        title = { Text("Über BatterySentinel") },
        text = {
            Column {
                Text("Version $APP_VERSION", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Änderungen in dieser Version:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                ChangelogEntry("1.1.0", listOf(
                    "Swipe-Navigation zwischen Tabs",
                    "Ladezyklen korrekt anzeigen (statt mAh)",
                    "Charging-Tab: Neugestaltung mit aktivem Ladestatus, ETA und Statistiken",
                    "Apps: Alle Einträge anklickbar (öffnet App-Einstellungen)",
                    "Apps: Neue Ansicht 'Pro Zyklus' für sessionbezogene Nutzung",
                    "Settings: 5 rotierende System-Backups",
                    "Benachrichtigungen öffnen nun die App beim Antippen",
                    "Optimize: Überarbeitete Batterie-Tipps"
                ))
                Spacer(modifier = Modifier.height(8.dp))
                ChangelogEntry("1.0.0", listOf(
                    "Erstveröffentlichung",
                    "Echtzeit-Akkuüberwachung",
                    "Ladesitzungsverlauf",
                    "App-Nutzungsstatistiken",
                    "System-Backup & -Wiederherstellung"
                ))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Entwickelt von FlameFox · com.flamefox.batterysentinel",
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
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System-Backups", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${backups.size}/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Maximal 5 Backups werden gespeichert. Das älteste wird automatisch überschrieben.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (backups.isEmpty()) {
                Text(
                    "Noch kein Backup vorhanden. Das Backup wird beim nächsten App-Start automatisch angelegt.",
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
                                    if (index == 0) "Neuestes Backup" else "Backup ${index + 1}",
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
                                "Helligkeit: ${backup.brightness}  |  " +
                                "Adaptiv: ${if (backup.isAdaptiveBrightness) "Ein" else "Aus"}  |  " +
                                "Timeout: ${backup.screenTimeoutMs / 1000}s  |  " +
                                "Sparmodus: ${if (backup.isBatterySaverEnabled) "Ein" else "Aus"}",
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
                    Text("Neuestes Backup wiederherstellen")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Backup wiederherstellen?") },
            text = {
                Text(
                    "Alle System-Einstellungen (Helligkeit, Screen-Timeout, Energiesparmodus, " +
                    "Synchronisierung, Doze) werden auf den zuletzt gespeicherten Zustand zurückgesetzt."
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
                ) { Text("Wiederherstellen") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Abbrechen") }
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
                    "settings" -> TextButton(onClick = { onGrant?.invoke() }) { Text("Erteilen") }
                    "adb" -> TextButton(onClick = { onCopy?.invoke() }) { Text("ADB kopieren") }
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
