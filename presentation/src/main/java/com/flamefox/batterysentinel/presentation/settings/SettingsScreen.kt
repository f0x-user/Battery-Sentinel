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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flamefox.batterysentinel.core.common.Constants

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
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Permission Status Dashboard
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    name = "Usage Stats",
                    description = "Per-app foreground time",
                    granted = state.hasUsageStatsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    grantType = "settings"
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Write Settings",
                    description = "Brightness & screen timeout control",
                    granted = state.hasWriteSettingsPermission,
                    onGrant = { context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)) },
                    grantType = "settings"
                )
                HorizontalDivider()
                PermissionRow(
                    name = "Battery Stats",
                    description = "Per-app battery attribution (mAh)",
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
                    name = "Write Secure Settings",
                    description = "Battery saver & Doze control",
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

        // Threshold Configuration
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thresholds", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Charge Alarm Threshold: ${state.appSettings.chargeAlarmThreshold}%",
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

        // Notification Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notifications")
                    Text("Charge alarms, temp alerts, anomalies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(description, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
