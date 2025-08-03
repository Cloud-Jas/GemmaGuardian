package com.GemmaGuardian.securitymonitor.presentation.emergency

import androidx.lifecycle.ViewModel
import com.GemmaGuardian.securitymonitor.data.alarm.AlarmManager
import com.GemmaGuardian.securitymonitor.data.notification.NotificationHandler
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for emergency alert functionality
 * Provides access to alarm and notification management from UI components
 */
@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val alarmManager: AlarmManager,
    private val notificationHandler: NotificationHandler
) : ViewModel() {
    
    /**
     * Stop the emergency alarm
     */
    fun stopEmergencyAlarm() {
        alarmManager.stopAlarm()
    }
    
    /**
     * Check if emergency alarm is currently playing
     */
    fun isEmergencyAlarmPlaying(): Boolean {
        return alarmManager.isAlarmPlaying()
    }
    
    /**
     * Test the alarm system
     */
    fun testAlarm() {
        alarmManager.testAlarm()
    }
    
    /**
     * Trigger emergency alarm for testing
     */
    fun triggerEmergencyAlarm(threatLevel: ThreatLevel) {
        alarmManager.triggerEmergencyAlarm(threatLevel)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Stop any playing alarms when ViewModel is destroyed
        alarmManager.stopAlarm()
    }
}
