package com.flamefox.batterysentinel.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val steps = listOf(
    Triple("Welcome to BatterySentinel", "Monitor and optimize your Pixel 8 Pro battery life. Let's set up the required permissions.", "Continue"),
    Triple("Notifications", "Allow notifications to receive charge alarms and temperature warnings.", "Grant Notifications"),
    Triple("Battery Optimization", "Exclude BatterySentinel from battery optimization so the service runs reliably.", "Exclude from Optimization"),
    Triple("Usage Stats (Optional)", "Grant PACKAGE_USAGE_STATS to see per-app foreground time.", "Grant Usage Access"),
    Triple("Write Settings (Optional)", "Grant WRITE_SETTINGS to control brightness and screen timeout.", "Grant Write Settings"),
    Triple("ADB Permissions (Optional)", "For battery attribution and advanced power control, grant these via ADB:\n\nadb shell pm grant com.flamefox.batterysentinel android.permission.BATTERY_STATS\nadb shell pm grant com.flamefox.batterysentinel android.permission.WRITE_SECURE_SETTINGS", "All Done")
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val step = state.currentStep.coerceAtMost(steps.size - 1)
    val (title, description, buttonLabel) = steps[step]

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
        viewModel.nextStep()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Step ${step + 1} / ${steps.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when (step) {
                    0 -> viewModel.nextStep()
                    1 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.nextStep()
                        }
                    }
                    2 -> {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:com.flamefox.batterysentinel")
                        }
                        context.startActivity(intent)
                        viewModel.nextStep()
                    }
                    3 -> {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        viewModel.nextStep()
                    }
                    4 -> {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                        viewModel.nextStep()
                    }
                    5 -> {
                        viewModel.markOnboardingComplete()
                        onComplete()
                    }
                }
            }
        ) { Text(buttonLabel) }

        if (step < steps.size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (step == steps.size - 2) {
                        viewModel.markOnboardingComplete()
                        onComplete()
                    } else {
                        viewModel.nextStep()
                    }
                }
            ) { Text("Skip") }
        }
    }
}
