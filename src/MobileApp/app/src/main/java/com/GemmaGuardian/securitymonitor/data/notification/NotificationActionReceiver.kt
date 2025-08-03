package com.GemmaGuardian.securitymonitor.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.data.alarm.AlarmManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    @Inject
    lateinit var notificationHandler: NotificationHandler
    
    @Inject
    lateinit var alarmManager: AlarmManager
    
    companion object {
        const val ACTION_ACKNOWLEDGE = "com.GemmaGuardian.securitymonitor.ACKNOWLEDGE_ALERT"
        const val ACTION_VIEW_VIDEO = "com.GemmaGuardian.securitymonitor.VIEW_VIDEO"
        const val ACTION_STOP_ALARM = "com.GemmaGuardian.securitymonitor.STOP_ALARM"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_VIDEO_ID = "video_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("NotificationActionReceiver", "📨 Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_ACKNOWLEDGE -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
                if (alertId != null) {
                    android.util.Log.d("NotificationActionReceiver", "✅ Acknowledging alert: $alertId")
                    acknowledgeAlert(alertId)
                }
            }
            ACTION_VIEW_VIDEO -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
                if (videoId != null) {
                    android.util.Log.d("NotificationActionReceiver", "📹 Opening video: $videoId")
                    openVideoPlayer(context, videoId)
                }
            }
            ACTION_STOP_ALARM -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
                android.util.Log.d("NotificationActionReceiver", "🔇 Stopping emergency alarm for alert: $alertId")
                stopEmergencyAlarm()
            }
        }
    }
    
    private fun acknowledgeAlert(alertId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                securityRepository.acknowledgeAlert(alertId)
                // Optionally clear the specific notification
            } catch (e: Exception) {
                android.util.Log.e("NotificationActionReceiver", "❌ Error acknowledging alert: ${e.message}")
            }
        }
    }
    
    private fun stopEmergencyAlarm() {
        try {
            alarmManager.stopAlarm()
            android.util.Log.d("NotificationActionReceiver", "✅ Emergency alarm stopped successfully")
        } catch (e: Exception) {
            android.util.Log.e("NotificationActionReceiver", "❌ Error stopping emergency alarm: ${e.message}")
        }
    }

    private fun openVideoPlayer(context: Context, videoId: String) {
        val intent = Intent(context, com.GemmaGuardian.securitymonitor.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "video_player")
            putExtra("video_id", videoId)
        }
        context.startActivity(intent)
    }
}
