package com.flamefox.batterysentinel.presentation.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UpdateDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (!state.isVisible) return

    AlertDialog(
        onDismissRequest = {
            if (state.status != UpdateStatus.DOWNLOADING) viewModel.dismiss()
        },
        icon = {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null)
        },
        title = {
            Text(
                when (state.status) {
                    UpdateStatus.DOWNLOADING -> "Downloading Update..."
                    UpdateStatus.DOWNLOAD_COMPLETE -> "Download Complete"
                    else -> "Update Available"
                }
            )
        },
        text = {
            Column {
                when (state.status) {
                    UpdateStatus.UPDATE_AVAILABLE -> {
                        Text(
                            "A new version of BatterySentinel is available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Current version: ${state.currentVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "New version: ${state.latestVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    UpdateStatus.DOWNLOADING -> {
                        Text(
                            "Downloading v${state.latestVersion}...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${state.downloadProgress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                    UpdateStatus.DOWNLOAD_COMPLETE -> {
                        Text(
                            "v${state.latestVersion} is ready to install.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "The app will restart after installation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (state.status) {
                UpdateStatus.UPDATE_AVAILABLE -> {
                    Button(onClick = viewModel::startDownload) {
                        Text("Update")
                    }
                }
                UpdateStatus.DOWNLOAD_COMPLETE -> {
                    Button(onClick = { viewModel.installApk(context) }) {
                        Text("Install")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (state.status != UpdateStatus.DOWNLOADING) {
                OutlinedButton(onClick = viewModel::dismiss) {
                    Text("Later")
                }
            }
        }
    )
}
