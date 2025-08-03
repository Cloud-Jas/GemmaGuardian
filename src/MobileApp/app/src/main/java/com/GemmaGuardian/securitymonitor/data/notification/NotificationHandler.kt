package com.GemmaGuardian.securitymonitor.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.GemmaGuardian.securitymonitor.MainActivity
import com.GemmaGuardian.securitymonitor.R
import com.GemmaGuardian.securitymonitor.data.alarm.AlarmManager
import com.GemmaGuardian.securitymonitor.domain.model.SecurityAlert
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPreferences: NotificationPreferences,
    private val alarmManager: AlarmManager
) {
    
    companion object {
        private const val CHANNEL_ID_SECURITY = "security_alerts_v2" // Changed ID to force recreate
        private const val CHANNEL_ID_SYSTEM = "system_notifications_v2" // Changed ID to force recreate  
        private const val CHANNEL_ID_CRITICAL = "critical_alerts_v2" // Changed ID to force recreate
        
        private const val GROUP_KEY_SECURITY = "security_group"
        
        private val notificationIdGenerator = AtomicInteger(1000)
    }
    
    private val _newAlerts = MutableSharedFlow<SecurityAlert>()
    val newAlerts: SharedFlow<SecurityAlert> = _newAlerts.asSharedFlow()
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Show security alert notification
     */
    fun showSecurityAlert(alert: SecurityAlert) {
        android.util.Log.d("NotificationHandler", "🔔 Showing security alert: ${alert.description}")
        
        // Check if notifications are enabled
        if (!notificationPreferences.areNotificationsEnabled()) {
            android.util.Log.w("NotificationHandler", "⚠️ Notifications disabled in preferences")
            return
        }
        
        // Check minimum threat level
        if (alert.threatLevel < notificationPreferences.getMinimumThreatLevel()) {
            android.util.Log.d("NotificationHandler", "📉 Alert below minimum threat level")
            return
        }
        
        // Check if current time is within scheduled alarm periods
        if (!notificationPreferences.isCurrentTimeInSchedule()) {
            android.util.Log.d("NotificationHandler", "⏰ Current time outside of scheduled alarm periods")
            return
        }
        
        // 🚨 TRIGGER EMERGENCY ALARM FOR CRITICAL/HIGH THREATS 🚨
        // AlarmManager handles continuous emergency alarm, so disable notification sounds for these
        val isEmergencyAlert = alert.threatLevel == ThreatLevel.CRITICAL || alert.threatLevel == ThreatLevel.HIGH
        if (isEmergencyAlert) {
            android.util.Log.w("NotificationHandler", "🚨 EMERGENCY ALERT: Triggering AlarmManager for ${alert.threatLevel} threat")
            alarmManager.triggerEmergencyAlarm(alert.threatLevel)
        }
        
        val channelId = when (alert.threatLevel) {
            ThreatLevel.CRITICAL, ThreatLevel.HIGH -> CHANNEL_ID_CRITICAL
            else -> CHANNEL_ID_SECURITY
        }
        
        // Create expanded content with video info
        val expandedText = buildString {
            append(alert.description)
            append("\n\n📹 Camera: ${alert.camera}")
            append("\n🎯 Confidence: ${(alert.confidence * 100).toInt()}%")
            append("\n⏰ Time: ${alert.timestamp}")
            
            alert.videoClip?.let { video ->
                append("\n🎬 Video available: ${video.fileName}")
                append("\n🔗 Tap to view video")
            }
        }
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getAlertIcon(alert.threatLevel))
            .setContentTitle(getAlertTitle(alert.threatLevel))
            .setContentText(alert.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setColor(getAlertColor(alert.threatLevel))
            .setGroup(GROUP_KEY_SECURITY)
            .setAutoCancel(true)
            .setContentIntent(createEmergencyPendingIntent(alert)) // Use emergency intent for CRITICAL/HIGH
            .addAction(createAcknowledgeAction(alert))
            .setPriority(getNotificationPriority(alert.threatLevel))
        
        // Add Stop Alarm action for emergency notifications
        if (alert.threatLevel == ThreatLevel.CRITICAL || alert.threatLevel == ThreatLevel.HIGH) {
            notificationBuilder.addAction(createStopAlarmAction(alert))
        }
        
        // Add video action if video is available
        alert.videoClip?.let { video ->
            notificationBuilder.addAction(createViewVideoAction(alert))
        }
        
        // Set custom sound and vibration for alerts
        // Note: For emergency alerts (CRITICAL/HIGH), AlarmManager handles audio
        // For all alerts, show completely silent notifications
        if (alert.threatLevel == ThreatLevel.CRITICAL || alert.threatLevel == ThreatLevel.HIGH) {
            android.util.Log.d("NotificationHandler", "🚨 Emergency alert - AlarmManager handles audio, completely silent notification")
            
            // Disable notification sounds for emergency alerts (AlarmManager handles audio)
            notificationBuilder.setDefaults(0) // Clear all defaults
            notificationBuilder.setSound(null) // Explicitly disable sound
            notificationBuilder.setVibrate(null) // Completely disable vibration too
            android.util.Log.d("NotificationHandler", "� Completely silent notification for ${alert.threatLevel}")
            
        } else {
            // For non-emergency alerts (LOW/MEDIUM), completely silent notifications
            android.util.Log.d("NotificationHandler", "🔕 Non-emergency alert - completely silent notification")
            notificationBuilder.setDefaults(0) // Clear all defaults
            notificationBuilder.setSound(null) // No sound
            notificationBuilder.setVibrate(null) // No vibration
        }
        
        val notification = notificationBuilder.build()
        val notificationId = notificationIdGenerator.incrementAndGet()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            android.util.Log.d("NotificationHandler", "✅ Notification shown with ID: $notificationId")
            
            // Emit to flow for real-time updates
            _newAlerts.tryEmit(alert)
            
            // Show summary notification if multiple alerts
            showSummaryNotification()
            
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHandler", "❌ Security exception showing notification: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationHandler", "❌ Failed to show notification: ${e.message}")
        }
    }
    
    /**
     * Show system notification (non-security)
     */
    fun showSystemNotification(
        title: String,
        message: String,
        isImportant: Boolean = false
    ) {
        if (!notificationPreferences.areSystemNotificationsEnabled()) {
            return
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.drawable.ic_system_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(if (isImportant) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationId = notificationIdGenerator.incrementAndGet()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission denial gracefully
        }
    }
    
    /**
     * Clear all security notifications
     */
    fun clearAllSecurityNotifications() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(GROUP_KEY_SECURITY.hashCode())
        } catch (e: SecurityException) {
            // Handle permission denial gracefully
        }
    }
    
    /**
     * Stop emergency alarm
     */
    fun stopEmergencyAlarm() {
        android.util.Log.d("NotificationHandler", "🔇 Stopping emergency alarm via NotificationHandler")
        alarmManager.stopAlarm()
    }
    
    /**
     * Check if emergency alarm is currently playing
     */
    fun isEmergencyAlarmPlaying(): Boolean {
        return alarmManager.isAlarmPlaying()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            android.util.Log.d("NotificationHandler", "🔧 Creating notification channels...")
            
            // Delete old channels that might have sound settings
            try {
                notificationManager.deleteNotificationChannel("security_alerts")
                notificationManager.deleteNotificationChannel("critical_alerts") 
                notificationManager.deleteNotificationChannel("system_notifications")
                android.util.Log.d("NotificationHandler", "🗑️ Deleted old notification channels")
            } catch (e: Exception) {
                android.util.Log.w("NotificationHandler", "Could not delete old channels: ${e.message}")
            }
            
            // Security alerts channel - completely silent
            val securityChannel = NotificationChannel(
                CHANNEL_ID_SECURITY,
                "Security Alerts",
                NotificationManager.IMPORTANCE_LOW // LOW importance to prevent any sound
            ).apply {
                description = "Security monitoring alerts - completely silent"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(false) // No vibration
                setSound(null, null) // No sound
                android.util.Log.d("NotificationHandler", "✅ Security channel created - completely silent")
            }
            
            // Critical alerts channel - completely silent (AlarmManager handles audio)
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Critical Security Alerts",
                NotificationManager.IMPORTANCE_LOW // LOW importance to prevent any sound
            ).apply {
                description = "Critical security threats - AlarmManager handles audio, notifications are completely silent"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(false) // No vibration from notification
                setSound(null, null) // No sound - AlarmManager handles emergency audio
                android.util.Log.d("NotificationHandler", "🚨 Critical channel created - completely silent (AlarmManager handles audio)")
            }
            
            // System notifications channel - also silent
            val systemChannel = NotificationChannel(
                CHANNEL_ID_SYSTEM,
                "System Notifications",
                NotificationManager.IMPORTANCE_LOW // LOW importance to prevent any sound
            ).apply {
                description = "System status and general notifications - silent"
                enableLights(false)
                enableVibration(false)
                setSound(null, null) // No sound
            }
            
            notificationManager.createNotificationChannels(listOf(
                securityChannel,
                criticalChannel,
                systemChannel
            ))
        }
    }
    
    private fun createPendingIntent(alert: SecurityAlert): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alert.id)
            putExtra("navigate_to", "alerts")
        }
        
        return PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Create emergency pending intent for CRITICAL/HIGH threats
     * Opens emergency splash screen first, then navigates to alert details
     */
    private fun createEmergencyPendingIntent(alert: SecurityAlert): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alert.id)
            putExtra("navigate_to", if (alert.threatLevel == ThreatLevel.CRITICAL || alert.threatLevel == ThreatLevel.HIGH) "emergency" else "alerts")
            putExtra("is_emergency", true)
            putExtra("threat_level", alert.threatLevel.name)
        }
        
        return PendingIntent.getActivity(
            context,
            alert.id.hashCode() + 1000, // Different request code for emergency
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createAcknowledgeAction(alert: SecurityAlert): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ACKNOWLEDGE
            putExtra("alert_id", alert.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_check,
            "Acknowledge",
            pendingIntent
        ).build()
    }
    
    private fun createStopAlarmAction(alert: SecurityAlert): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_ALARM
            putExtra("alert_id", alert.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alert.id.hashCode() + 2000, // Different request code for stop alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_volume_off,
            "Stop Alarm",
            pendingIntent
        ).build()
    }    private fun createViewVideoAction(alert: SecurityAlert): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_id", alert.id)
            putExtra("navigate_to", "video_player")
            alert.videoClip?.let { clip ->
                putExtra("video_id", clip.id)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_play_arrow,
            "View Video",
            pendingIntent
        ).build()
    }
    
    private fun showSummaryNotification() {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
            .setSmallIcon(R.drawable.ic_security_alert)
            .setContentTitle("Security Monitoring")
            .setContentText("Multiple security alerts")
            .setGroup(GROUP_KEY_SECURITY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(GROUP_KEY_SECURITY.hashCode(), summaryNotification)
        } catch (e: SecurityException) {
            // Handle permission denial gracefully
        }
    }
    
    private fun getAlertIcon(threatLevel: ThreatLevel): Int {
        return when (threatLevel) {
            ThreatLevel.LOW -> R.drawable.ic_info
            ThreatLevel.MEDIUM -> R.drawable.ic_warning
            ThreatLevel.HIGH -> R.drawable.ic_error
            ThreatLevel.CRITICAL -> R.drawable.ic_critical_alert
        }
    }
    
    private fun getAlertTitle(threatLevel: ThreatLevel): String {
        return when (threatLevel) {
            ThreatLevel.LOW -> "Security Notice"
            ThreatLevel.MEDIUM -> "Security Alert"
            ThreatLevel.HIGH -> "High Security Alert"
            ThreatLevel.CRITICAL -> "CRITICAL SECURITY THREAT"
        }
    }
    
    private fun getAlertColor(threatLevel: ThreatLevel): Int {
        return when (threatLevel) {
            ThreatLevel.LOW -> Color.parseColor("#4CAF50")      // Green
            ThreatLevel.MEDIUM -> Color.parseColor("#FF9800")   // Orange
            ThreatLevel.HIGH -> Color.parseColor("#F44336")     // Red
            ThreatLevel.CRITICAL -> Color.parseColor("#9C27B0") // Purple
        }
    }
    
    private fun getNotificationPriority(threatLevel: ThreatLevel): Int {
        return when (threatLevel) {
            ThreatLevel.LOW -> NotificationCompat.PRIORITY_MIN // Lowest possible priority
            ThreatLevel.MEDIUM -> NotificationCompat.PRIORITY_LOW // Low priority
            ThreatLevel.HIGH -> NotificationCompat.PRIORITY_LOW // Low priority to prevent sound
            ThreatLevel.CRITICAL -> NotificationCompat.PRIORITY_LOW // Low priority to prevent sound
        }
    }
}
