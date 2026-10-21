package com.aistra.hail.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.aistra.hail.R

enum class AppDestinations(
    val route: String, val label: Int, val icon: ImageVector, val iconSelected: ImageVector
) {
    HOME(
        route = "home", label = R.string.title_home, icon = Icons.Rounded.AcUnit, iconSelected = Icons.Rounded.AcUnit
    ),
    APPS(
        route = "apps",
        label = R.string.title_apps,
        icon = Icons.AutoMirrored.Outlined.ViewList,
        iconSelected = Icons.AutoMirrored.Filled.ViewList
    ),
    SETTINGS(
        route = "settings",
        label = R.string.title_settings,
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings
    ),
    ABOUT(
        route = "about", label = R.string.title_about, icon = Icons.Outlined.Info, iconSelected = Icons.Filled.Info
    )
}

fun NavController.navigateTo(destination: AppDestinations) = navigate(destination.route) {
    popUpTo(graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
