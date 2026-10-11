package com.example.safescreen.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.safescreen.ui.apps.AppsScreen
import com.example.safescreen.ui.home.HomeScreen
import com.example.safescreen.ui.restriction.RestrictionPreviewScreen
import com.example.safescreen.ui.settings.SettingsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Apps("apps", "Apps", Icons.AutoMirrored.Filled.List),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

private const val PREVIEW_ROUTE = "restriction/{packageName}"

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Home.route) {
                HomeScreen(onOpenApps = { navController.navigateToTab(Tab.Apps.route) })
            }
            composable(Tab.Apps.route) {
                AppsScreen(onPreview = { pkg -> navController.navigate("restriction/$pkg") })
            }
            composable(Tab.Settings.route) { SettingsScreen() }
            composable(
                route = PREVIEW_ROUTE,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) { entry ->
                RestrictionPreviewScreen(
                    packageName = entry.arguments?.getString("packageName").orEmpty(),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
