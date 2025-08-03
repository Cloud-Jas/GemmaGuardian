package com.GemmaGuardian.securitymonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.GemmaGuardian.securitymonitor.data.notification.PushNotificationService
import com.GemmaGuardian.securitymonitor.data.alarm.AlarmManager
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var alarmManager: AlarmManager

    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Notification permission granted")
            startNotificationService()
        } else {
            android.util.Log.w("MainActivity", "⚠️ Notification permission denied")
            // Still start service, but notify user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "🚀 MainActivity starting...")

        // No need for splash screen here since we have SplashActivity

        // Request notification permission first
        requestNotificationPermission()

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Fix for three-button nav not properly going edge-to-edge
            window.isNavigationBarContrastEnforced = false
        }

        // Get alert ID and navigation info from notification intent if present
        val alertId = intent?.getStringExtra("alert_id")
        val navigateTo = intent?.getStringExtra("navigate_to")
        val isEmergency = intent?.getBooleanExtra("is_emergency", false) ?: false

        // 🚨 STOP ALARM WHEN NOTIFICATION IS TAPPED 🚨
        if (isEmergency && alertId != null) {
            android.util.Log.d("MainActivity", "🔇 Stopping emergency alarm - notification tapped")
            alarmManager.stopAlarm()
        }

        setContent {
            GemmaGuardTheme(darkTheme = true) { // Use GemmaGuard dark theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecurityMonitorApp(
                        initialAlertId = alertId,
                        initialNavigationTarget = navigateTo,
                        isEmergency = isEmergency
                    )
                }
            }
        }

        // Keep the screen on while the app is running for better monitoring experience
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "📨 New intent received")
        
        // Handle notification click while app is already running
        val alertId = intent.getStringExtra("alert_id")
        val navigateTo = intent.getStringExtra("navigate_to")
        val isEmergency = intent.getBooleanExtra("is_emergency", false)
        
        // 🚨 STOP ALARM WHEN NOTIFICATION IS TAPPED WHILE APP IS RUNNING 🚨
        if (isEmergency && alertId != null) {
            android.util.Log.d("MainActivity", "🔇 Stopping emergency alarm - notification tapped (app running)")
            alarmManager.stopAlarm()
        }
        
        if (alertId != null) {
            android.util.Log.d("MainActivity", "📱 Navigating to ${if (isEmergency) "emergency" else "normal"} alert: $alertId")
            // TODO: Implement navigation to specific alert or emergency screen
            // This would typically involve updating a shared ViewModel or using a navigation event
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "✅ Notification permission already granted")
                    startNotificationService()
                }
                else -> {
                    android.util.Log.d("MainActivity", "📋 Requesting notification permission...")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            android.util.Log.d("MainActivity", "📱 Android < 13, no permission needed")
            startNotificationService()
        }
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(this, PushNotificationService::class.java)
        
        // Use startForegroundService for Android 8.0+ to ensure background operation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
            android.util.Log.d("MainActivity", "📡 PushNotificationService started as foreground service")
        } else {
            startService(serviceIntent)
            android.util.Log.d("MainActivity", "📡 PushNotificationService started as regular service")
        }
    }
}
