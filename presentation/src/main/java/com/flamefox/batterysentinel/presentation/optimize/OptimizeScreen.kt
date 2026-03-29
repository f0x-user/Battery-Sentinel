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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
        Text("Optimize", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Brightness Controls (WRITE_SETTINGS)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Settings", style = MaterialTheme.typography.titleSmall)
                    if (!state.hasWriteSettingsPermission) {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                        }) { Text("Grant") }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Adaptive Brightness")
                    Switch(
                        checked = state.isAdaptiveBrightness,
                        onCheckedChange = viewModel::setAdaptiveBrightness,
                        enabled = state.hasWriteSettingsPermission
                    )
                }

                if (!state.isAdaptiveBrightness) {
                    Text("Brightness: ${state.brightness}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = state.brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 0f..255f,
                        enabled = state.hasWriteSettingsPermission
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Screen Timeout", style = MaterialTheme.typography.labelMedium)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Battery Saver & Doze (WRITE_SECURE_SETTINGS)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Power Management", style = MaterialTheme.typography.titleSmall)
                    if (!state.hasWriteSecureSettingsPermission) {
                        Text("ADB required", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Battery Saver")
                        Text("Reduces performance and background activity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.isBatterySaverEnabled,
                        onCheckedChange = { viewModel.toggleBatterySaver() },
                        enabled = state.hasWriteSecureSettingsPermission
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Doze Status", style = MaterialTheme.typography.labelMedium)
                Text(state.dozeStatus, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sync Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Background Sync")
                    Text("Disable to reduce background battery drain",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isSyncEnabled,
                    onCheckedChange = { viewModel.setSync(it) }
                )
            }
        }
    }
}
