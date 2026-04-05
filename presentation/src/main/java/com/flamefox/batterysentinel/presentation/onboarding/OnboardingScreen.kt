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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private data class OnboardingStep(
    val title: String,
    val description: String,
    val buttonLabel: String,
    val icon: ImageVector
)

private val steps = listOf(
    OnboardingStep(
        title = "Welcome to BatterySentinel",
        description = "Monitor and optimize your battery health.",
        buttonLabel = "Get Started",
        icon = Icons.Filled.BatteryChargingFull
    ),
    OnboardingStep(
        title = "Notifications",
        description = "Allow notifications to receive charge alerts and temperature warnings.",
        buttonLabel = "Grant Permission",
        icon = Icons.Filled.Notifications
    ),
    OnboardingStep(
        title = "Battery Optimization",
        description = "Exclude BatterySentinel from battery optimization so the background service runs reliably.",
        buttonLabel = "Exclude App",
        icon = Icons.Filled.BatteryAlert
    ),
    OnboardingStep(
        title = "Usage Statistics (optional)",
        description = "Grant PACKAGE_USAGE_STATS to see per-app foreground time in the Apps tab.",
        buttonLabel = "Grant Access",
        icon = Icons.Filled.BarChart
    ),
    OnboardingStep(
        title = "Write Settings (optional)",
        description = "Grant WRITE_SETTINGS to control brightness and screen timeout directly from the app.",
        buttonLabel = "Grant Permission",
        icon = Icons.Filled.Tune
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val step = state.currentStep.coerceAtMost(steps.size - 1)
    val currentStep = steps[step]

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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step ${step + 1} / ${steps.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = currentStep.icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    currentStep.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    currentStep.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                        viewModel.markOnboardingComplete()
                        onComplete()
                    }
                }
            }
        ) { Text(currentStep.buttonLabel) }

        if (step < steps.size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (step >= steps.size - 2) {
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
