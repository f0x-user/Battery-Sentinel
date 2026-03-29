package com.flamefox.batterysentinel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.flamefox.batterysentinel.core.ui.theme.BatterySentinelTheme
import com.flamefox.batterysentinel.data.service.BatteryMonitorService
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import com.flamefox.batterysentinel.presentation.navigation.AppNavigation
import com.flamefox.batterysentinel.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SystemSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startForegroundService(Intent(this, BatteryMonitorService::class.java))

        lifecycleScope.launch {
            val settings = settingsRepository.getAppSettings().first()
            val startDestination = if (settings.onboardingCompleted) {
                Screen.Dashboard.route
            } else {
                Screen.Onboarding.route
            }

            setContent {
                BatterySentinelTheme {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
