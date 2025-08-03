package com.GemmaGuardian.securitymonitor.data.network

import com.GemmaGuardian.securitymonitor.domain.model.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface SecurityApiService {
    
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>
    
    @GET("api/security/stats")
    suspend fun getSecurityStats(): Response<SecurityStatsResponse>
    
    @GET("api/security/alerts")
    suspend fun getAlerts(
        @Query("limit") limit: Int? = null,
        @Query("threat_level") threatLevel: String? = null
    ): Response<List<SecurityAlertResponse>>
    
    @GET("api/security/alerts/{alert_id}")
    suspend fun getAlertDetails(@Path("alert_id") alertId: String): Response<AlertDetailsResponse>
    
    @GET("api/security/videos")
    suspend fun getVideos(
        @Query("limit") limit: Int? = null
    ): Response<List<VideoResponse>>
    
    @GET("api/security/videos/{video_id}")
    suspend fun getVideoDetails(@Path("video_id") videoId: String): Response<VideoDetailsResponse>
    
    @GET("api/security/analysis/{session_id}")
    suspend fun getAnalysisSession(@Path("session_id") sessionId: String): Response<AnalysisSessionResponse>
    
    @GET("api/security/cameras")
    suspend fun getCameraStatus(): Response<List<CameraStatusResponse>>
    
    @POST("api/security/alerts/{alert_id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("alert_id") alertId: String): Response<Unit>
    
    @GET("api/system/health")
    suspend fun getSystemHealth(): Response<SystemHealthResponse>
}

// Response DTOs matching our API
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String
)

@Serializable
data class SecurityStatsResponse(
    val totalAlerts: Int,
    val criticalAlerts: Int,
    val highAlerts: Int,
    val mediumAlerts: Int,
    val lowAlerts: Int,
    val lastAlertTime: String?,
    val recentAlerts24h: Int,
    val systemStatus: String,
    val lastUpdated: String
)

@Serializable
data class SecurityAlertResponse(
    val id: String,
    val timestamp: String,
    val threatLevel: String,
    val confidence: Float,
    val isThreatDetected: Boolean,
    val summary: String,
    val keywords: List<String>,
    val description: String,
    val camera: String,
    val isAcknowledged: Boolean,
    val videoClip: VideoClipResponse?
)

@Serializable
data class VideoClipResponse(
    val id: String,
    val url: String?,
    val fileName: String?,
    val filePath: String?,
    val thumbnailUrl: String?,
    val timestamp: String,
    val duration: String
)

@Serializable
data class AlertDetailsResponse(
    val alertId: String,
    val analysisSession: String = "", // JSON string instead of complex object
    val batchAnalyses: List<String> = emptyList(), // List of JSON strings
    val consolidatedAnalysis: String = "",
    val framesAnalyzed: String = "", // JSON string instead of complex object
    val modelUsed: String = "",
    val processingMethod: String = "",
    val analysisPrompt: String = ""
)

@Serializable
data class VideoResponse(
    val id: String,
    val fileName: String,
    val filePath: String,
    val url: String,
    val timestamp: String,
    val fileSize: Long,
    val threatLevel: String,
    val confidence: Float,
    val description: String,
    val camera: String,
    val duration: String,
    val resolution: String,
    val fps: Int
)

@Serializable
data class VideoDetailsResponse(
    val id: String,
    val fileName: String,
    val filePath: String,
    val url: String,
    val timestamp: String,
    val fileSize: Long,
    val threatLevel: String,
    val confidence: Float,
    val description: String,
    val summary: String,
    val keywords: List<String>,
    val camera: String,
    val duration: String,
    val resolution: String,
    val fps: Int,
    val analysisSession: String?, // Simplified to JSON string
    val frameAnalyses: List<FrameAnalysisResponse>,
    val consolidatedAnalysis: String
)

@Serializable
data class FrameAnalysisResponse(
    val batchNumber: Int,
    val frameRange: Map<String, Int>,
    val timestampRange: Map<String, Int>,
    val analysis: String,
    val framesInBatch: Int
)

@Serializable
data class AnalysisSessionResponse(
    val timestamp: String,
    val videoPath: String,
    val modelUsed: String,
    val processingMethod: String,
    val batchSize: Int,
    val framesAnalyzed: String, // Simplified to JSON string
    val batchAnalyses: String, // Simplified to JSON string
    val consolidatedAnalysis: String,
    val analysisPrompt: String
)

@Serializable
data class CameraStatusResponse(
    val id: String,
    val name: String,
    val status: String,
    val location: String,
    val resolution: String,
    val fps: Int,
    val lastSeen: String,
    val isOnline: Boolean,
    val storageUsed: String,
    val recordingEnabled: Boolean
)

@Serializable
data class SystemHealthResponse(
    val status: String,
    val isHealthy: Boolean,
    val timestamp: String,
    val systemUptime: String? = null,
    val camerasOnline: Int? = null,
    val totalCameras: Int? = null,
    val recordingStatus: String? = null,
    val storageAvailable: String? = null,
    val lastAlert: String? = null,
    val alertsToday: Int? = null,
    val error: String? = null
)
