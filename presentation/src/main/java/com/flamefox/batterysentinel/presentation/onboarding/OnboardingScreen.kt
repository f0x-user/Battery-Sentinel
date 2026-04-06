package com.flamefox.batterysentinel.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Three onboarding steps — no device-specific names, no optional permission steps.
private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val steps = listOf(
    OnboardingStep(
        title = "Willkommen bei BatterySentinel",
        description = "Überwache und optimiere deine Akku-Gesundheit.",
        icon = Icons.Filled.BatteryChargingFull
    ),
    OnboardingStep(
        title = "Benachrichtigungen",
        description = "Erhalte Alarme bei vollem Akku und Temperaturwarnungen.",
        icon = Icons.Filled.NotificationsActive
    ),
    OnboardingStep(
        title = "Akku-Optimierungen",
        description = "Verhindere, dass Android den Monitoring-Service beendet.",
        icon = Icons.Filled.BatteryAlert
    )
)

/**
 * Full-screen onboarding flow with three steps.
 * Uses explicit background and text colors so the screen is readable in both light and dark theme.
 */
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

    // Explicit background ensures readable contrast in both light and dark theme.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            Text(
                text = "${step + 1} / ${steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step icon — 80 dp, primary color
            Icon(
                imageVector = currentStep.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Content card — surfaceVariant background with explicit text colors
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentStep.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary action button — label depends on current step
            when (step) {
                0 -> Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Los geht's", fontSize = 16.sp)
                }

                1 -> Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Berechtigung erteilen", fontSize = 16.sp)
                }

                2 -> Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        viewModel.markOnboardingComplete()
                        onComplete()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Optimierungen deaktivieren", fontSize = 16.sp)
                }
            }

            // Skip button — always visible except on the last step where the primary action
            // completes onboarding anyway
            if (step < steps.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        if (step >= steps.size - 2) {
                            viewModel.markOnboardingComplete()
                            onComplete()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Überspringen")
                }
            }
        }
    }
}
