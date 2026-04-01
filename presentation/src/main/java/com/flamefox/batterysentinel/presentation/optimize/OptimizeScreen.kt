package com.flamefox.batterysentinel.presentation.optimize

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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.ui.theme.BatteryGreen
import com.flamefox.batterysentinel.core.ui.theme.BatteryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizeScreen(viewModel: OptimizeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Akku optimieren", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Passe Systemeinstellungen direkt an, um die Akkulaufzeit zu verlängern.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Helligkeitssteuerung
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Display-Helligkeit",
                    icon = Icons.Filled.Brightness4,
                    tip = "Display ist oft der größte Akkuverbraucher — niedrigere Helligkeit spart bis zu 20%.",
                    hasPermission = state.hasWriteSettingsPermission,
                    onGrantPermission = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(label = "Adaptive Helligkeit", description = "Helligkeit automatisch anpassen") {
                    Switch(
                        checked = state.isAdaptiveBrightness,
                        onCheckedChange = viewModel::setAdaptiveBrightness,
                        enabled = state.hasWriteSettingsPermission
                    )
                }

                if (!state.isAdaptiveBrightness) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Helligkeit: ${state.brightness} / 255",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = state.brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 0f..255f,
                        enabled = state.hasWriteSettingsPermission
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickSetButton("Niedrig (60)", enabled = state.hasWriteSettingsPermission) {
                            viewModel.setBrightness(60)
                        }
                        QuickSetButton("Mittel (128)", enabled = state.hasWriteSettingsPermission) {
                            viewModel.setBrightness(128)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Screen-Timeout
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Bildschirm-Timeout",
                    icon = Icons.Filled.LockClock,
                    tip = "Kürzeres Timeout spart Strom wenn du das Gerät weglegt.",
                    hasPermission = state.hasWriteSettingsPermission,
                    onGrantPermission = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                val timeoutOptions = listOf(15000, 30000, 60000, 120000, 300000, 600000)
                val timeoutLabels = listOf("15s", "30s", "1m", "2m", "5m", "10m")
                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = timeoutLabels[
                    timeoutOptions.indexOfFirst { it == state.screenTimeout }.coerceAtLeast(0)
                ]

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    TextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Timeout-Dauer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = state.hasWriteSettingsPermission
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        timeoutOptions.zip(timeoutLabels).forEach { (ms, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setScreenTimeout(ms)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Energiesparmodus & Doze
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Energieverwaltung",
                    icon = Icons.Filled.Power,
                    tip = "Energiesparmodus und Doze reduzieren Hintergrundaktivitäten erheblich.",
                    hasPermission = state.hasWriteSecureSettingsPermission,
                    onGrantPermission = null,
                    permissionNote = "ADB-Berechtigung erforderlich (in Einstellungen → Berechtigungen)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(
                    label = "Energiesparmodus",
                    description = "Reduziert Leistung und Hintergrundaktivität"
                ) {
                    Switch(
                        checked = state.isBatterySaverEnabled,
                        onCheckedChange = { viewModel.toggleBatterySaver() },
                        enabled = state.hasWriteSecureSettingsPermission
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Doze-Status", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.dozeStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hintergrund-Synchronisierung
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Hintergrund-Synchronisierung",
                    icon = Icons.Filled.Sync,
                    tip = "Deaktivierter Sync spart bei schlechter Verbindung erheblich Strom."
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(
                    label = "Automatisch synchronisieren",
                    description = "Hintergrundaktualisierung aller Konten"
                ) {
                    Switch(
                        checked = state.isSyncEnabled,
                        onCheckedChange = { viewModel.setSync(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Praktische Tipps (keine Systemsteuerung)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "  Akku-Pflegetipps",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                BatteryTip(
                    icon = Icons.Filled.BatteryAlert,
                    text = "Lade zwischen 20% und 80% — vermeide tägliche Vollladungen auf 100%"
                )
                BatteryTip(
                    icon = Icons.Filled.Thermostat,
                    text = "Überhitzung über 40°C schadet dem Akku dauerhaft — Ladegerät bei Wärme trennen"
                )
                BatteryTip(
                    icon = Icons.Filled.BrightnessAuto,
                    text = "Adaptive Helligkeit aktivieren spart im Alltag bis zu 15% Strom"
                )
                BatteryTip(
                    icon = Icons.Filled.NetworkCheck,
                    text = "WLAN statt Mobilfunk nutzen spart bis zu 30% im Vergleich zu 5G"
                )
                BatteryTip(
                    icon = Icons.Filled.LockClock,
                    text = "15-30s Bildschirm-Timeout einzustellen verlängert die Laufzeit spürbar"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OptimizeSectionHeader(
    title: String,
    icon: ImageVector,
    tip: String,
    hasPermission: Boolean = true,
    onGrantPermission: (() -> Unit)? = null,
    permissionNote: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text("  $title", style = MaterialTheme.typography.titleSmall)
        }
        when {
            hasPermission -> Icon(
                Icons.Filled.Check,
                contentDescription = "Berechtigung erteilt",
                tint = BatteryGreen,
                modifier = Modifier.size(16.dp)
            )
            onGrantPermission != null -> Button(
                onClick = onGrantPermission,
                modifier = Modifier.height(32.dp)
            ) { Text("Berechtigung", style = MaterialTheme.typography.labelSmall) }
            else -> Text(
                "ADB",
                style = MaterialTheme.typography.labelSmall,
                color = BatteryOrange
            )
        }
    }
    Text(
        tip,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
    if (!hasPermission && permissionNote != null) {
        Text(
            permissionNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun OptimizeRow(label: String, description: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}

@Composable
private fun QuickSetButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.height(32.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BatteryTip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp).padding(top = 1.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            "  $text",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
