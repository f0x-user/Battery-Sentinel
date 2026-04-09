package com.flamefox.batterysentinel.presentation.optimize

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.ui.theme.BatteryGreen
import com.flamefox.batterysentinel.core.ui.theme.BatteryOrange

/**
 * Optimize screen — shows the current system state (IST-Stand) for battery-relevant settings.
 * All values are read-only. Each section links to the relevant Android system screen so the
 * user can adjust settings themselves.
 *
 * READ_ONLY values (Battery Saver, Battery Optimizations) are shown as status cards with
 * links to the relevant Android settings screens — they cannot be set programmatically
 * without elevated permissions.
 */
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
            "Current system settings at a glance. Use the links below to adjust them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ── Display brightness ─────────────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusSectionHeader(
                    title = "Display Brightness",
                    icon = Icons.Filled.Brightness4,
                    tip = "Display is the largest battery consumer — lower brightness saves up to 20%."
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "Adaptive Brightness",
                    value = if (state.isAdaptiveBrightness) "On" else "Off",
                    valueColor = if (state.isAdaptiveBrightness) BatteryGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.isAdaptiveBrightness && state.hasWriteSettingsPermission) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusRow(label = "Brightness Level", value = "${state.brightness} / 255")
                }
                if (!state.hasWriteSettingsPermission) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Write Settings permission not granted.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                        } catch (_: ActivityNotFoundException) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Display Settings →", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Screen timeout ─────────────────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusSectionHeader(
                    title = "Screen Timeout",
                    icon = Icons.Filled.LockClock,
                    tip = "Shorter timeout saves power when you put down your device."
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "Current Timeout",
                    value = formatTimeout(state.screenTimeout)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                        } catch (_: ActivityNotFoundException) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Display Settings →", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Power management ───────────────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Power,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("  Power Management", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Battery saver and battery optimizations are managed through system settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Saver", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (state.isBatterySaverEnabled) "Currently active" else "Currently inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isBatterySaverEnabled) BatteryGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                            } catch (_: ActivityNotFoundException) {}
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Open Settings →", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Optimizations", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = if (state.isIgnoringBatteryOptimizations) {
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                } else {
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                }
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {}
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Configure →", style = MaterialTheme.typography.labelSmall)
                    }
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (state.isIgnoringBatteryOptimizations) "Optimizations disabled"
                            else "Optimizations active",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (state.isIgnoringBatteryOptimizations) Icons.Filled.Check
                            else Icons.Filled.BatteryAlert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.isIgnoringBatteryOptimizations)
                            BatteryGreen.copy(alpha = 0.2f)
                        else
                            BatteryOrange.copy(alpha = 0.2f),
                        labelColor = if (state.isIgnoringBatteryOptimizations) BatteryGreen
                        else BatteryOrange
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Background sync ────────────────────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusSectionHeader(
                    title = "Background Sync",
                    icon = Icons.Filled.Sync,
                    tip = "Disabled sync saves significant power on poor connections."
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "Auto-Sync",
                    value = if (state.isSyncEnabled) "Enabled" else "Disabled",
                    valueColor = if (!state.isSyncEnabled) BatteryGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                        } catch (_: ActivityNotFoundException) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Sync Settings →", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Battery care tips ──────────────────────────────────────────────────────────────────
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
                BatteryTip(Icons.Filled.BatteryAlert, "Charge between 20% and 80% — avoid daily full charges to 100%")
                BatteryTip(Icons.Filled.Thermostat, "Overheating above 40°C permanently damages the battery — disconnect charger in heat")
                BatteryTip(Icons.Filled.BrightnessAuto, "Enabling adaptive brightness saves up to 15% in daily use")
                BatteryTip(Icons.Filled.NetworkCheck, "Using Wi-Fi instead of mobile data saves up to 30% compared to 5G")
                BatteryTip(Icons.Filled.LockClock, "Setting a 15–30s screen timeout noticeably extends battery life")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatTimeout(ms: Int): String = when {
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}min"
    else -> "${ms / 3_600_000}h"
}

@Composable
private fun StatusSectionHeader(title: String, icon: ImageVector, tip: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text("  $title", style = MaterialTheme.typography.titleSmall)
    }
    Text(
        tip,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun BatteryTip(icon: ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
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