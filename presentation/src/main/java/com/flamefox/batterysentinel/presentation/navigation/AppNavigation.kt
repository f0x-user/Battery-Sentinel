package com.flamefox.batterysentinel.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.flamefox.batterysentinel.presentation.apps.AppsScreen
import com.flamefox.batterysentinel.presentation.charging.ChargingScreen
import com.flamefox.batterysentinel.presentation.dashboard.DashboardScreen
import com.flamefox.batterysentinel.presentation.onboarding.OnboardingScreen
import com.flamefox.batterysentinel.presentation.optimize.OptimizeScreen
import com.flamefox.batterysentinel.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

private data class TabItem(val label: String, val icon: ImageVector)

private val mainTabs = listOf(
    TabItem("Dashboard", Icons.Filled.BatteryFull),
    TabItem("Charging", Icons.Filled.Analytics),
    TabItem("Apps", Icons.Filled.Apps),
    TabItem("Optimize", Icons.Filled.Tune),
    TabItem("Settings", Icons.Filled.Settings)
)

@Composable
fun AppNavigation(startDestination: String = Screen.Dashboard.route) {
    var onboardingDone by remember {
        mutableStateOf(startDestination != Screen.Onboarding.route)
    }

    if (!onboardingDone) {
        OnboardingScreen(onComplete = { onboardingDone = true })
    } else {
        MainTabsScreen()
    }
}

@Composable
private fun MainTabsScreen() {
    val pagerState = rememberPagerState(pageCount = { mainTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                mainTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    index,
                                    animationSpec = tween(300)
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> DashboardScreen()
                1 -> ChargingScreen()
                2 -> AppsScreen()
                3 -> OptimizeScreen()
                4 -> SettingsScreen()
            }
        }
    }
}
