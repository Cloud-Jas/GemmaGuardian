package com.GemmaGuardian.securitymonitor.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.GemmaGuardian.securitymonitor.R

sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector
) {
    object Home : Screen(
        route = "home",
        titleResId = R.string.home,
        icon = Icons.Default.Home
    )
    
    object Alerts : Screen(
        route = "alerts", 
        titleResId = R.string.alerts,
        icon = Icons.Default.NotificationImportant
    ) {
        const val routeWithArgs = "alerts/{alertId}"
        fun createRoute(alertId: String) = "alerts/$alertId"
    }
    
    object Videos : Screen(
        route = "videos",
        titleResId = R.string.videos,
        icon = Icons.Default.VideoLibrary
    )
    
    object Settings : Screen(
        route = "settings",
        titleResId = R.string.settings,
        icon = Icons.Default.Settings
    )
    
    object VideoPlayer : Screen(
        route = "video_player",
        titleResId = R.string.videos, // Using videos string since it's for video player
        icon = Icons.Default.VideoLibrary
    )
    
    object AlertDetails : Screen(
        route = "alert_details",
        titleResId = R.string.alerts,
        icon = Icons.Default.NotificationImportant
    ) {
        const val routeWithArgs = "alert_details/{alertId}"
        fun createRoute(alertId: String) = "alert_details/$alertId"
    }
    
    object EmergencyScreen : Screen(
        route = "emergency",
        titleResId = R.string.alerts, // Using alerts string resource
        icon = Icons.Default.NotificationImportant
    ) {
        const val routeWithArgs = "emergency/{alertId}"
        fun createRoute(alertId: String) = "emergency/$alertId"
    }
    
    object EmergencyAlertDetails : Screen(
        route = "emergency_alert_details",
        titleResId = R.string.alerts,
        icon = Icons.Default.NotificationImportant
    ) {
        const val routeWithArgs = "emergency_alert_details/{alertId}"
        fun createRoute(alertId: String) = "emergency_alert_details/$alertId"
    }
    
    object EmergencyTest : Screen(
        route = "emergency_test",
        titleResId = R.string.alerts, // Using alerts string resource
        icon = Icons.Default.NotificationImportant
    )
}
