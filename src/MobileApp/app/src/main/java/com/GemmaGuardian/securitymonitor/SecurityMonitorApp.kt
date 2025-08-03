package com.GemmaGuardian.securitymonitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.GemmaGuardian.securitymonitor.presentation.navigation.Screen
import com.GemmaGuardian.securitymonitor.presentation.navigation.SecurityMonitorNavHost

/** Top level composable representing the main screen of the application. */
@Composable
fun SecurityMonitorApp(
    navController: NavHostController = rememberNavController(),
    initialAlertId: String? = null,
    initialNavigationTarget: String? = null,
    isEmergency: Boolean = false
) {
    // Handle navigation from notification
    LaunchedEffect(initialAlertId, initialNavigationTarget, isEmergency) {
        if (initialAlertId != null && initialNavigationTarget != null) {
            when {
                isEmergency && (initialNavigationTarget == "emergency") -> {
                    android.util.Log.d("SecurityMonitorApp", "� EMERGENCY: Navigating to emergency screen for alert: $initialAlertId")
                    navController.navigate(Screen.EmergencyScreen.createRoute(initialAlertId)) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
                initialNavigationTarget == "alerts" -> {
                    android.util.Log.d("SecurityMonitorApp", "�📱 Navigating to alerts for alert: $initialAlertId")
                    navController.navigate(Screen.Alerts.createRoute(initialAlertId)) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
                else -> {
                    android.util.Log.d("SecurityMonitorApp", "📱 Navigating to default alerts screen")
                    navController.navigate(Screen.Alerts.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
    
    SecurityMonitorNavHost(navController = navController)
}
