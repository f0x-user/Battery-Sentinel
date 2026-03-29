package com.flamefox.batterysentinel.presentation.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Charging : Screen("charging")
    data object Apps : Screen("apps")
    data object Optimize : Screen("optimize")
    data object Settings : Screen("settings")
}
