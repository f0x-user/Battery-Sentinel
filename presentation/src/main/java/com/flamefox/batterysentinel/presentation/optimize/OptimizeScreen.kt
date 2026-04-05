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
        Text("Optimize Battery", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Adjust system settings directly to extend battery life.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Display brightness
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Display Brightness",
                    icon = Icons.Filled.Brightness4,
                    tip = "Display is often the largest battery consumer — lower brightness saves up to 20%.",
                    hasPermission = state.hasWriteSettingsPermission,
                    onGrantPermission = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(label = "Adaptive Brightness", description = "Automatically adjust brightness") {
                    Switch(
                        checked = state.isAdaptiveBrightness,
                        onCheckedChange = viewModel::setAdaptiveBrightness,
                        enabled = state.hasWriteSettingsPermission
                    )
                }

                if (!state.isAdaptiveBrightness) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Brightness: ${state.brightness} / 255",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = state.brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 0f..255f,
                        enabled = state.hasWriteSettingsPermission
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickSetButton("Low (60)", enabled = state.hasWriteSettingsPermission) {
                            viewModel.setBrightness(60)
                        }
                        QuickSetButton("Medium (128)", enabled = state.hasWriteSettingsPermission) {
                            viewModel.setBrightness(128)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Screen timeout
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Screen Timeout",
                    icon = Icons.Filled.LockClock,
                    tip = "Shorter timeout saves power when you put down your device.",
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
                        label = { Text("Timeout Duration") },
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

        // Power management
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Power Management",
                    icon = Icons.Filled.Power,
                    tip = "Battery saver and Doze significantly reduce background activity.",
                    hasPermission = state.hasWriteSecureSettingsPermission,
                    onGrantPermission = null,
                    permissionNote = "ADB permission required (in Settings → Permissions)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(
                    label = "Battery Saver",
                    description = "Reduces performance and background activity"
                ) {
                    Switch(
                        checked = state.isBatterySaverEnabled,
                        onCheckedChange = { viewModel.toggleBatterySaver() },
                        enabled = state.hasWriteSecureSettingsPermission
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Doze Status", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.dozeStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Background sync
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OptimizeSectionHeader(
                    title = "Background Sync",
                    icon = Icons.Filled.Sync,
                    tip = "Disabled sync saves significant power on poor connections."
                )
                Spacer(modifier = Modifier.height(8.dp))

                OptimizeRow(
                    label = "Auto-Sync",
                    description = "Background refresh for all accounts"
                ) {
                    Switch(
                        checked = state.isSyncEnabled,
                        onCheckedChange = { viewModel.setSync(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Battery care tips
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
                        "  Battery Care Tips",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                BatteryTip(
                    icon = Icons.Filled.BatteryAlert,
                    text = "Charge between 20% and 80% — avoid daily full charges to 100%"
                )
                BatteryTip(
                    icon = Icons.Filled.Thermostat,
                    text = "Overheating above 40°C permanently damages the battery — disconnect charger in heat"
                )
                BatteryTip(
                    icon = Icons.Filled.BrightnessAuto,
                    text = "Enabling adaptive brightness saves up to 15% in daily use"
                )
                BatteryTip(
                    icon = Icons.Filled.NetworkCheck,
                    text = "Using Wi-Fi instead of mobile data saves up to 30% compared to 5G"
                )
                BatteryTip(
                    icon = Icons.Filled.LockClock,
                    text = "Setting a 15–30s screen timeout noticeably extends battery life"
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
                contentDescription = "Permission granted",
                tint = BatteryGreen,
                modifier = Modifier.size(16.dp)
            )
            onGrantPermission != null -> Button(
                onClick = onGrantPermission,
                modifier = Modifier.height(32.dp)
            ) { Text("Grant", style = MaterialTheme.typography.labelSmall) }
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
