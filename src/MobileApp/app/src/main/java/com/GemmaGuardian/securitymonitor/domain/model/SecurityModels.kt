package com.GemmaGuardian.securitymonitor.domain.model

import kotlinx.datetime.Instant
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
enum class ThreatLevel : Parcelable {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Parcelize
data class SecurityAlert(
    val id: String,
    val timestamp: @RawValue Instant,
    val threatLevel: ThreatLevel,
    val confidence: Float,
    val isThreatDetected: Boolean,
    val summary: String,
    val keywords: List<String>,
    val description: String = summary, // Fallback for backward compatibility
    val camera: String = "",
    val isAcknowledged: Boolean = false,
    val videoClip: VideoClip? = null
) : Parcelable

@Parcelize
data class VideoClip(
    val id: String,
    val url: String,
    val fileName: String = "",
    val filePath: String = url,
    val thumbnailUrl: String? = null,
    val timestamp: @RawValue Instant? = null,
    val duration: @RawValue kotlin.time.Duration
) : Parcelable

@Parcelize
data class SystemStatus(
    val isOnline: Boolean,
    val serverIp: String?,
    val lastSeen: @RawValue Instant?,
    val activeMonitoring: Boolean,
    val notificationPort: Int
) : Parcelable

@Parcelize
data class NotificationSettings(
    val enabled: Boolean,
    val minThreatLevel: ThreatLevel,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val pushNotifications: Boolean
) : Parcelable

data class DashboardData(
    val systemStatus: SystemStatus,
    val recentAlerts: List<SecurityAlert>,
    val todayStats: SecurityStats
)

data class CameraInfo(
    val id: String,
    val name: String,
    val location: String,
    val status: String,
    val isOnline: Boolean,
    val lastSeen: Instant,
    val resolution: String,
    val fps: Int,
    val storageUsed: String,
    val recordingEnabled: Boolean
)

enum class CameraStatus {
    ONLINE, WARNING, OFFLINE
}

@Parcelize
data class VideoRecording(
    val id: String,
    val fileName: String,
    val filePath: String,
    val url: String,
    val timestamp: @RawValue Instant,
    val fileSize: Long,
    val threatLevel: ThreatLevel,
    val confidence: Float,
    val description: String,
    val camera: String,
    val duration: @RawValue kotlin.time.Duration,
    val resolution: String,
    val fps: Int,
    val analysisSession: @RawValue Map<String, Any>? = null,
    val frameAnalyses: List<FrameAnalysis> = emptyList(),
    val consolidatedAnalysis: String = "",
    val keywords: List<String> = emptyList()
) : Parcelable

@Parcelize
data class FrameAnalysis(
    val batchNumber: Int,
    val frameRange: @RawValue Map<String, Int>,
    val timestampRange: @RawValue Map<String, Int>,
    val analysis: String,
    val framesInBatch: Int
) : Parcelable

data class SystemHealth(
    val status: String,
    val uptime: Long,
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val temperature: Double,
    val lastUpdate: @RawValue Instant
)
