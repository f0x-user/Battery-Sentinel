package com.flamefox.batterysentinel.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flamefox.batterysentinel.presentation.apps.AppsScreen
import com.flamefox.batterysentinel.presentation.charging.ChargingScreen
import com.flamefox.batterysentinel.presentation.dashboard.DashboardScreen
import com.flamefox.batterysentinel.presentation.onboarding.OnboardingScreen
import com.flamefox.batterysentinel.presentation.optimize.OptimizeScreen
import com.flamefox.batterysentinel.presentation.settings.SettingsScreen

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun AppNavigation(startDestination: String = Screen.Dashboard.route) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.BatteryFull),
        BottomNavItem(Screen.Charging, "Charging", Icons.Filled.Analytics),
        BottomNavItem(Screen.Apps, "Apps", Icons.Filled.Apps),
        BottomNavItem(Screen.Optimize, "Optimize", Icons.Filled.Tune),
        BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route != Screen.Onboarding.route
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Charging.route) { ChargingScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.Optimize.route) { OptimizeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
