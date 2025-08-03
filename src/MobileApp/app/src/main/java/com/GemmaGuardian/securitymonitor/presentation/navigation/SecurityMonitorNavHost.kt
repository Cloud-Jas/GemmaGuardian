package com.GemmaGuardian.securitymonitor.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.GemmaGuardian.securitymonitor.domain.model.SecurityAlert
import com.GemmaGuardian.securitymonitor.presentation.home.HomeScreen
import com.GemmaGuardian.securitymonitor.presentation.alerts.AlertsScreen
import com.GemmaGuardian.securitymonitor.presentation.alertdetails.AlertDetailsScreen
import com.GemmaGuardian.securitymonitor.presentation.emergency.EmergencyScreen
import com.GemmaGuardian.securitymonitor.presentation.emergency.EmergencyAlertDetailsScreen
import com.GemmaGuardian.securitymonitor.presentation.emergency.EmergencyTestScreen
import com.GemmaGuardian.securitymonitor.presentation.videos.VideosScreen
import com.GemmaGuardian.securitymonitor.presentation.settings.SettingsScreen
import com.GemmaGuardian.securitymonitor.presentation.videoplayer.VideoPlayerScreen
import com.GemmaGuardian.securitymonitor.data.notification.NotificationPreferences
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors

@Composable
fun SecurityMonitorNavHost(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = GemmaGuardColors.ButtonSecondary.copy(alpha = 0.95f),
                contentColor = GemmaGuardColors.TextPrimary
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = currentDestination?.hierarchy?.any { destination ->
                            destination.route == screen.route || 
                            (screen.route == "alerts" && destination.route?.startsWith("alerts") == true)
                        } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GemmaGuardColors.Primary,
                            selectedTextColor = GemmaGuardColors.Primary,
                            unselectedIconColor = GemmaGuardColors.TextSecondary,
                            unselectedTextColor = GemmaGuardColors.TextSecondary,
                            indicatorColor = GemmaGuardColors.Primary.copy(alpha = 0.2f)
                        ),
                        onClick = {
                            android.util.Log.d("Navigation", "🏠 Clicked nav item: ${screen.route}, current: ${currentDestination?.route}")
                            if (screen.route == Screen.Home.route) {
                                // For Home navigation, always clear the back stack
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseOutExpo)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseOutExpo)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseOutExpo)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseOutExpo)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    innerPadding = innerPadding,
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToVideos = { navController.navigate(Screen.Videos.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAlert = { alertId -> 
                        navController.navigate(Screen.AlertDetails.createRoute(alertId)) 
                    }
                )
            }
            composable(Screen.Alerts.route) {
                AlertsScreen(
                    innerPadding = innerPadding,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAlertDetails = { alert ->
                        navController.navigate(Screen.AlertDetails.createRoute(alert.id))
                    },
                    initialAlertId = null
                )
            }
            composable(Screen.Alerts.routeWithArgs) { backStackEntry ->
                val alertId = backStackEntry.arguments?.getString("alertId")
                AlertsScreen(
                    innerPadding = innerPadding,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAlertDetails = { alert ->
                        navController.navigate(Screen.AlertDetails.createRoute(alert.id))
                    },
                    initialAlertId = alertId
                )
            }
            composable(Screen.Videos.route) {
                VideosScreen(
                    innerPadding = innerPadding,
                    onNavigateToPlayer = { videoId ->
                        navController.navigate("${Screen.VideoPlayer.route}/${videoId}")
                    }
                )
            }
            composable(Screen.Settings.route) {
                // Inject NotificationPreferences using Hilt
                val context = androidx.compose.ui.platform.LocalContext.current
                val notificationPreferences = remember {
                    val sharedPrefs = context.getSharedPreferences("security_monitor_prefs", android.content.Context.MODE_PRIVATE)
                    com.GemmaGuardian.securitymonitor.data.notification.NotificationPreferences(context, sharedPrefs)
                }
                
                SettingsScreen(
                    innerPadding = innerPadding,
                    notificationPreferences = notificationPreferences
                )
            }
            composable("${Screen.VideoPlayer.route}/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                VideoPlayerScreen(
                    videoId = videoId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable(Screen.AlertDetails.routeWithArgs) { backStackEntry ->
                val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                val alertsViewModel: com.GemmaGuardian.securitymonitor.presentation.alerts.AlertsViewModel = hiltViewModel()
                
                var alert by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SecurityAlert?>(null) }
                var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
                var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                
                // Fetch alert details when alertId changes
                androidx.compose.runtime.LaunchedEffect(alertId) {
                    if (alertId.isNotEmpty()) {
                        isLoading = true
                        error = null
                        try {
                            val fetchedAlert = alertsViewModel.getAlertDetails(alertId)
                            if (fetchedAlert != null) {
                                alert = fetchedAlert
                            } else {
                                error = "Alert not found"
                            }
                        } catch (e: Exception) {
                            error = "Failed to load alert: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
                
                when {
                    isLoading -> {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        androidx.compose.foundation.layout.Column(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(
                                text = "Error: $error",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { navController.navigateUp() }
                            ) {
                                androidx.compose.material3.Text("Go Back")
                            }
                        }
                    }
                    alert != null -> {
                        AlertDetailsScreen(
                            alert = alert!!,
                            onBackClick = { navController.navigateUp() },
                            onAcknowledge = { alertsViewModel.acknowledgeAlert(alertId) },
                            onPlayVideo = { 
                                alert!!.videoClip?.let { clip ->
                                    navController.navigate("${Screen.VideoPlayer.route}/${clip.id}")
                                }
                            },
                            innerPadding = innerPadding
                        )
                    }
                }
            }
            
            // 🚨 EMERGENCY SCREEN ROUTE 🚨
            composable(Screen.EmergencyScreen.routeWithArgs) { backStackEntry ->
                val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                val alertsViewModel: com.GemmaGuardian.securitymonitor.presentation.alerts.AlertsViewModel = hiltViewModel()
                
                var alert by remember { mutableStateOf<SecurityAlert?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                
                // Fetch alert details when alertId changes
                LaunchedEffect(alertId) {
                    if (alertId.isNotEmpty()) {
                        isLoading = true
                        try {
                            val fetchedAlert = alertsViewModel.getAlertDetails(alertId)
                            alert = fetchedAlert
                        } catch (e: Exception) {
                            android.util.Log.e("EmergencyScreen", "Failed to load alert: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                }
                
                when {
                    isLoading || alert == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        EmergencyScreen(
                            alert = alert!!,
                            onNavigateToDetails = {
                                navController.navigate(Screen.EmergencyAlertDetails.createRoute(alertId)) {
                                    popUpTo(Screen.EmergencyScreen.createRoute(alertId)) {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // 🚨 EMERGENCY ALERT DETAILS ROUTE 🚨
            composable(Screen.EmergencyAlertDetails.routeWithArgs) { backStackEntry ->
                val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                val alertsViewModel: com.GemmaGuardian.securitymonitor.presentation.alerts.AlertsViewModel = hiltViewModel()
                val emergencyViewModel: com.GemmaGuardian.securitymonitor.presentation.emergency.EmergencyViewModel = hiltViewModel()
                
                var alert by remember { mutableStateOf<SecurityAlert?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var error by remember { mutableStateOf<String?>(null) }
                var isAlarmPlaying by remember { mutableStateOf(false) }
                
                // Check alarm status periodically
                LaunchedEffect(Unit) {
                    while (true) {
                        isAlarmPlaying = emergencyViewModel.isEmergencyAlarmPlaying()
                        kotlinx.coroutines.delay(1000) // Check every second
                    }
                }
                
                // Fetch alert details when alertId changes
                LaunchedEffect(alertId) {
                    if (alertId.isNotEmpty()) {
                        isLoading = true
                        error = null
                        try {
                            val fetchedAlert = alertsViewModel.getAlertDetails(alertId)
                            if (fetchedAlert != null) {
                                alert = fetchedAlert
                            } else {
                                error = "Alert not found"
                            }
                        } catch (e: Exception) {
                            error = "Failed to load alert: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigateUp() }
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                    alert != null -> {
                        EmergencyAlertDetailsScreen(
                            alert = alert!!,
                            onBackClick = { navController.navigateUp() },
                            onAcknowledge = { alertsViewModel.acknowledgeAlert(alertId) },
                            onPlayVideo = { 
                                alert!!.videoClip?.let { clip ->
                                    navController.navigate("${Screen.VideoPlayer.route}/${clip.id}")
                                }
                            },
                            onStopAlarm = { emergencyViewModel.stopEmergencyAlarm() },
                            isAlarmPlaying = isAlarmPlaying,
                            innerPadding = innerPadding
                        )
                    }
                }
            }
            
            // 🧪 EMERGENCY TEST SCREEN ROUTE 🧪
            composable(Screen.EmergencyTest.route) {
                EmergencyTestScreen(
                    onBackClick = { navController.navigateUp() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Alerts,
    Screen.Videos,
    Screen.Settings
)
