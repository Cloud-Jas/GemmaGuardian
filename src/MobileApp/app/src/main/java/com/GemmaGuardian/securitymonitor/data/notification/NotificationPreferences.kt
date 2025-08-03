package com.GemmaGuardian.securitymonitor.data.notification

import android.content.Context
import android.content.SharedPreferences
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SharedPreferences
) {
    
    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SYSTEM_NOTIFICATIONS_ENABLED = "system_notifications_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_MIN_THREAT_LEVEL = "min_threat_level"
        private const val KEY_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"
        private const val KEY_CRITICAL_ONLY = "critical_only"
        private const val KEY_ALARM_DURATION_SECONDS = "alarm_duration_seconds"
        private const val KEY_SCHEDULE_WEEKDAYS = "schedule_weekdays"
        private const val KEY_SCHEDULE_WEEKENDS = "schedule_weekends"
    }
    
    fun areNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    fun areSystemNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, true)
    }
    
    fun setSystemNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    fun isSoundEnabled(): Boolean {
        return preferences.getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun isVibrationEnabled(): Boolean {
        return preferences.getBoolean(KEY_VIBRATION_ENABLED, true)
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
    
    fun getMinimumThreatLevel(): ThreatLevel {
        val ordinal = preferences.getInt(KEY_MIN_THREAT_LEVEL, ThreatLevel.MEDIUM.ordinal)
        return ThreatLevel.values().getOrElse(ordinal) { ThreatLevel.MEDIUM }
    }
    
    fun setMinimumThreatLevel(threatLevel: ThreatLevel) {
        preferences.edit().putInt(KEY_MIN_THREAT_LEVEL, threatLevel.ordinal).apply()
    }
    
    fun arePushNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, true)
    }
    
    fun setPushNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    fun isCriticalOnlyMode(): Boolean {
        return preferences.getBoolean(KEY_CRITICAL_ONLY, false)
    }
    
    fun setCriticalOnlyMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CRITICAL_ONLY, enabled).apply()
        if (enabled) {
            setMinimumThreatLevel(ThreatLevel.CRITICAL)
        }
    }
    
    fun getAlarmDurationSeconds(): Int {
        return preferences.getInt(KEY_ALARM_DURATION_SECONDS, 10) // Default 10 seconds
    }
    
    fun setAlarmDurationSeconds(durationSeconds: Int) {
        preferences.edit().putInt(KEY_ALARM_DURATION_SECONDS, durationSeconds).apply()
    }
    
    fun getWeekdaySchedule(): List<String> {
        val scheduleString = preferences.getString(KEY_SCHEDULE_WEEKDAYS, "12:00 AM - 7:00 AM")
        android.util.Log.d("NotificationPreferences", "Loaded weekday schedule: $scheduleString")
        return if (scheduleString.isNullOrEmpty()) {
            listOf("12:00 AM - 7:00 AM")
        } else {
            scheduleString.split("|").filter { it.isNotBlank() }
        }
    }
    
    fun setWeekdaySchedule(timeSlots: List<String>) {
        val scheduleString = timeSlots.joinToString("|")
        preferences.edit().putString(KEY_SCHEDULE_WEEKDAYS, scheduleString).apply()
        android.util.Log.d("NotificationPreferences", "Saved weekday schedule: $scheduleString")
    }
    
    fun getWeekendSchedule(): List<String> {
        val scheduleString = preferences.getString(KEY_SCHEDULE_WEEKENDS, "12:00 AM - 7:00 AM")
        android.util.Log.d("NotificationPreferences", "Loaded weekend schedule: $scheduleString")
        return if (scheduleString.isNullOrEmpty()) {
            listOf("12:00 AM - 7:00 AM")
        } else {
            scheduleString.split("|").filter { it.isNotBlank() }
        }
    }
    
    fun setWeekendSchedule(timeSlots: List<String>) {
        val scheduleString = timeSlots.joinToString("|")
        preferences.edit().putString(KEY_SCHEDULE_WEEKENDS, scheduleString).apply()
        android.util.Log.d("NotificationPreferences", "Saved weekend schedule: $scheduleString")
    }
    
    fun isCurrentTimeInSchedule(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute // Convert to minutes since midnight
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        
        val scheduleToCheck = if (isWeekend) getWeekendSchedule() else getWeekdaySchedule()
        
        return scheduleToCheck.any { timeSlot ->
            isTimeInRange(currentTime, timeSlot)
        }
    }
    
    private fun isTimeInRange(currentTime: Int, timeSlot: String): Boolean {
        try {
            val parts = timeSlot.split(" - ")
            if (parts.size != 2) return false
            
            val startTime = parseTimeToMinutes(parts[0])
            val endTime = parseTimeToMinutes(parts[1])
            
            return if (startTime <= endTime) {
                // Same day range (e.g., 9:00 AM - 5:00 PM)
                currentTime in startTime..endTime
            } else {
                // Overnight range (e.g., 10:00 PM - 6:00 AM)
                currentTime >= startTime || currentTime <= endTime
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun parseTimeToMinutes(timeString: String): Int {
        try {
            val trimmed = timeString.trim()
            val parts = trimmed.split(":")
            val hour = parts[0].toInt()
            val minuteAndPeriod = parts[1].trim()
            val minute = minuteAndPeriod.substring(0, 2).toInt()
            val period = minuteAndPeriod.substring(3).trim().uppercase()
            
            var adjustedHour = hour
            if (period == "PM" && hour != 12) {
                adjustedHour += 12
            } else if (period == "AM" && hour == 12) {
                adjustedHour = 0
            }
            
            return adjustedHour * 60 + minute
        } catch (e: Exception) {
            return 0
        }
    }
}
