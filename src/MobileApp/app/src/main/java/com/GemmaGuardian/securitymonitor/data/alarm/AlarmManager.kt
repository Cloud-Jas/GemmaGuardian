package com.GemmaGuardian.securitymonitor.data.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.getSystemService
import com.GemmaGuardian.securitymonitor.R
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages alarm sounds and vibration for emergency alerts
 * Handles CRITICAL and HIGH threat level notifications with maximum volume
 */
@Singleton
class AlarmManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var alarmJob: Job? = null
    
    // Store original volume levels to restore them later
    private var originalAlarmVolume: Int = -1
    private var originalNotificationVolume: Int = -1
    private var originalMediaVolume: Int = -1
    
    companion object {
        private const val TAG = "AlarmManager"
        private const val ALARM_DURATION_MS = 10000L // 10 seconds max alarm
        
        // Default vibration pattern - cannot be const as arrays aren't allowed
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    }
    
    init {
        setupAudioManager()
        setupVibrator()
    }
    
    private fun setupAudioManager() {
        audioManager = context.getSystemService<AudioManager>()
    }
    
    private fun setupVibrator() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService<VibratorManager>()
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
    
    /**
     * Trigger emergency alarm for critical/high threats
     * Plays loud alarm sound and vibration
     */
    fun triggerEmergencyAlarm(threatLevel: ThreatLevel) {
        if (threatLevel != ThreatLevel.CRITICAL && threatLevel != ThreatLevel.HIGH) {
            Log.d(TAG, "Not triggering alarm for threat level: $threatLevel")
            return
        }
        
        Log.i(TAG, "🚨 Triggering emergency alarm for $threatLevel threat")
        
        // Stop any existing alarm
        stopAlarm()
        
        // Start alarm in coroutine to avoid blocking
        alarmJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Set volume to maximum for emergency
                setMaxVolume()
                
                // Start sound alarm
                startSoundAlarm(threatLevel)
                
                // Start vibration
                startVibration(threatLevel)
                
                // Auto-stop after duration
                delay(ALARM_DURATION_MS)
                stopAlarm()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during emergency alarm: ${e.message}", e)
                stopAlarm()
            }
        }
    }
    
    /**
     * Stop all alarm sounds and vibrations
     */
    fun stopAlarm() {
        Log.d(TAG, "🔇 Stopping emergency alarm")
        
        alarmJob?.cancel()
        alarmJob = null
        
        // Stop media player
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        }
        
        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration: ${e.message}")
        }
        
        // Restore original system volumes
        restoreOriginalVolumes()
    }
    
    private fun setMaxVolume() {
        try {
            audioManager?.let { am ->
                // Store original volumes before changing them
                originalAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                originalMediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                
                // Only set alarm volume to max for emergency alarms
                // DO NOT change notification or media volumes!
                val maxAlarmVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
                
                Log.d(TAG, "Stored original volumes - Alarm: $originalAlarmVolume, Notification: $originalNotificationVolume, Media: $originalMediaVolume")
                Log.d(TAG, "Set ONLY alarm volume to maximum ($maxAlarmVolume) for emergency")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm volume: ${e.message}")
        }
    }

    /**
     * Restore original system volume levels that were changed during emergency alarm
     */
    private fun restoreOriginalVolumes() {
        try {
            audioManager?.let { am ->
                // Only restore if we have valid original values
                if (originalAlarmVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
                    Log.d(TAG, "Restored alarm volume to $originalAlarmVolume")
                }
                if (originalNotificationVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
                    Log.d(TAG, "Restored notification volume to $originalNotificationVolume")
                }
                if (originalMediaVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, originalMediaVolume, 0)
                    Log.d(TAG, "Restored media volume to $originalMediaVolume")
                }
                
                // Reset stored values
                originalAlarmVolume = -1
                originalNotificationVolume = -1
                originalMediaVolume = -1
                
                Log.d(TAG, "✅ All system volumes restored to original levels")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring original volumes: ${e.message}")
        }
    }
    
    private fun startSoundAlarm(threatLevel: ThreatLevel) {
        try {
            // Try to get emergency/alarm ringtone first
            val alarmUri = getEmergencyRingtoneUri()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                
                // Set audio attributes for emergency alarm
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                
                // Loop the alarm sound
                isLooping = true
                
                // Prepare and start
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                    Log.d(TAG, "🔊 Emergency alarm sound started")
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    // Try fallback sound
                    startFallbackAlarm()
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sound alarm: ${e.message}")
            startFallbackAlarm()
        }
    }
    
    private fun startFallbackAlarm() {
        try {
            // Use system default alarm or notification sound
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, defaultUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting fallback alarm: ${e.message}")
        }
    }
    
    private fun startVibration(threatLevel: ThreatLevel) {
        try {
            vibrator?.let { vib ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Create aggressive vibration pattern for emergency
                    val effect = when (threatLevel) {
                        ThreatLevel.CRITICAL -> {
                            // Very aggressive pattern for critical threats
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 800, 200, 800, 200, 800, 400, 1000),
                                0 // Repeat from index 0
                            )
                        }
                        ThreatLevel.HIGH -> {
                            // Strong pattern for high threats
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 500, 300, 500, 300, 500),
                                0 // Repeat from index 0
                            )
                        }
                        else -> {
                            VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                        }
                    }
                    
                    vib.vibrate(effect)
                    Log.d(TAG, "📳 Emergency vibration started for $threatLevel")
                } else {
                    // Fallback for older Android versions
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATION_PATTERN, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration: ${e.message}")
        }
    }
    
    private fun getEmergencyRingtoneUri(): Uri {
        return try {
            // Try to get alarm ringtone
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: tryCustomEmergencyAlarm()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ringtone URI: ${e.message}")
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
    
    private fun tryCustomEmergencyAlarm(): Uri {
        return try {
            // Try to use custom emergency alarm from raw resources
            Uri.parse("android.resource://${context.packageName}/${context.resources.getIdentifier("emergency_alarm", "raw", context.packageName)}")
        } catch (e: Exception) {
            Log.d(TAG, "Custom emergency alarm not found, using default")
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
    
    /**
     * Check if alarm is currently playing
     */
    fun isAlarmPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
    
    /**
     * Test the alarm system (shorter duration for testing)
     */
    fun testAlarm() {
        Log.i(TAG, "🧪 Testing emergency alarm system")
        
        alarmJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                setMaxVolume()
                startSoundAlarm(ThreatLevel.HIGH)
                startVibration(ThreatLevel.HIGH)
                
                // Test for 3 seconds only
                delay(3000)
                stopAlarm()
                
                Log.d(TAG, "✅ Alarm test completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during alarm test: ${e.message}")
                stopAlarm()
            }
        }
    }
}
