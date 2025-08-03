package com.GemmaGuardian.securitymonitor.data.repository

import android.util.Log
import com.GemmaGuardian.securitymonitor.data.network.SecurityNetworkClient
import com.GemmaGuardian.securitymonitor.data.network.SecurityApiService
import com.GemmaGuardian.securitymonitor.data.network.NetworkResult
import com.GemmaGuardian.securitymonitor.data.network.ConnectionState
import com.GemmaGuardian.securitymonitor.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import com.GemmaGuardian.securitymonitor.data.cache.VideoCacheManager
import com.GemmaGuardian.securitymonitor.config.ConfigurationManager
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SecurityRepository @Inject constructor(
    private val apiService: SecurityApiService,
    private val networkClient: SecurityNetworkClient,
    private val configurationManager: ConfigurationManager,
    @ApplicationContext private val context: Context
) {
    
    private val videoCacheManager by lazy { VideoCacheManager(context) }
    
    companion object {
        private const val TAG = "SecurityRepository"
        
        /**
         * Parse timestamp with microsecond precision to Instant
         */
        private fun parseTimestamp(timestampStr: String): Instant {
            return try {
                // Try standard ISO-8601 format first
                Instant.parse(timestampStr)
            } catch (e: Exception) {
                try {
                    // Handle microsecond precision timestamps from backend
                    val zonedDateTime = ZonedDateTime.parse(timestampStr + "Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    Instant.fromEpochSeconds(zonedDateTime.toEpochSecond(), zonedDateTime.nano)
                } catch (e2: Exception) {
                    try {
                        // Fallback: truncate microseconds to milliseconds
                        val truncated = if (timestampStr.contains('.') && timestampStr.length > 23) {
                            timestampStr.substring(0, 23) + "Z"
                        } else {
                            timestampStr + "Z"
                        }
                        Instant.parse(truncated)
                    } catch (e3: Exception) {
                        Log.e(TAG, "Failed to parse timestamp '$timestampStr': ${e3.message}")
                        Clock.System.now()
                    }
                }
            }
        }
    }
    
    // Connection management
    val connectionState = networkClient.connectionState
    val serverInfo = networkClient.serverInfo
    
    suspend fun discoverServer() = networkClient.discoverServer()
    suspend fun connectToServer(ip: String, port: Int? = null) = networkClient.connectToServer(ip, port ?: networkClient.getDefaultPort())
    
    fun getDefaultIp(): String = networkClient.getDefaultIp()
    
    fun getDefaultPort(): Int = networkClient.getDefaultPort()
    fun disconnect() = networkClient.disconnect()
    fun getConnectionStatusMessage() = networkClient.getConnectionStatusMessage()
    
    /**
     * Execute API call with error handling and connection state management
     */
    private suspend fun <T> executeApiCall(call: suspend () -> Response<T>): NetworkResult<T> {
        return try {
            if (connectionState.value != ConnectionState.CONNECTED) {
                return NetworkResult.Error("Not connected to surveillance system")
            }
            
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    NetworkResult.Success(body)
                } ?: NetworkResult.Error("Empty response")
            } else {
                NetworkResult.Error("HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error("Connection timeout")
        } catch (e: ConnectException) {
            NetworkResult.Error("Cannot connect to server")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown network error")
        }
    }
    
    // Real API methods - no mock fallbacks
    suspend fun getSecurityStats(): SecurityStats {
        return try {
            Log.d(TAG, "Fetching real-time security statistics")
            when (val result = networkClient.executeApiCall { apiService.getSecurityStats() }) {
                is NetworkResult.Success -> {
                    val response = result.data
                    SecurityStats(
                        totalAlerts = response.totalAlerts,
                        criticalAlerts = response.criticalAlerts,
                        highAlerts = response.highAlerts,
                        mediumAlerts = response.mediumAlerts,
                        lowAlerts = response.lowAlerts,
                        lastAlertTime = response.lastAlertTime?.let { parseTimestamp(it) }
                    )
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch security stats: ${result.message}")
                    // Return empty stats instead of mock data
                    SecurityStats(
                        totalAlerts = 0,
                        criticalAlerts = 0,
                        highAlerts = 0,
                        mediumAlerts = 0,
                        lowAlerts = 0,
                        lastAlertTime = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch security stats: ${e.message}")
            SecurityStats(
                totalAlerts = 0,
                criticalAlerts = 0,
                highAlerts = 0,
                mediumAlerts = 0,
                lowAlerts = 0,
                lastAlertTime = null
            )
        }
    }

    suspend fun getAllAlerts(): List<SecurityAlert> {
        return try {
            Log.d(TAG, "🔄 Fetching real-time security alerts")
            when (val result = networkClient.executeApiCall { apiService.getAlerts() }) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ API returned ${result.data.size} alerts")
                    val alerts = result.data.mapNotNull { response ->
                        try {
                            Log.d(TAG, "📧 Mapping alert: ${response.id} - ${response.summary} - Level: ${response.threatLevel}")
                            SecurityAlert(
                                id = response.id,
                                timestamp = parseTimestamp(response.timestamp),
                                threatLevel = ThreatLevel.valueOf(response.threatLevel.uppercase()),
                                confidence = response.confidence,
                                isThreatDetected = response.isThreatDetected,
                                summary = response.summary,
                                keywords = response.keywords,
                                description = response.description,
                                camera = response.camera,
                                isAcknowledged = response.isAcknowledged,
                                videoClip = response.videoClip?.let { clip ->
                                    try {
                                        VideoClip(
                                            id = clip.id,
                                            url = clip.url ?: "",
                                            fileName = clip.fileName ?: "${clip.id}.mp4",
                                            filePath = clip.filePath ?: "",
                                            thumbnailUrl = clip.thumbnailUrl,
                                            timestamp = parseTimestamp(clip.timestamp),
                                            duration = kotlin.time.Duration.parse(clip.duration)
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed to parse video clip for alert ${response.id}: ${e.message}")
                                        null
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to map alert ${response.id}: ${e.message}")
                            null
                        }
                    }
                    Log.d(TAG, "🎯 Successfully mapped ${alerts.size} alerts")
                    alerts
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "❌ Failed to fetch security alerts: ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception fetching security alerts: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getAlertDetails(alertId: String): SecurityAlert? {
        return try {
            Log.d(TAG, "🔍 Fetching alert details for ID: $alertId")
            // For now, we'll get the basic alert from the alerts list since the details response 
            // has serialization issues with complex nested data structures.
            // TODO: Fix AlertDetailsResponse serialization for advanced analysis data
            getAllAlerts().find { it.id == alertId }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception fetching alert details for $alertId: ${e.message}")
            null
        }
    }

    suspend fun getVideoRecordings(limit: Int = 20): List<VideoRecording> {
        return try {
            Log.d(TAG, "Fetching real video recordings (limit: $limit)")
            when (val result = networkClient.executeApiCall { apiService.getVideos(limit) }) {
                is NetworkResult.Success -> {
                    result.data.map { response ->
                        VideoRecording(
                            id = response.id,
                            fileName = response.fileName,
                            filePath = response.filePath,
                            url = response.url,
                            timestamp = parseTimestamp(response.timestamp),
                            fileSize = response.fileSize,
                            threatLevel = ThreatLevel.valueOf(response.threatLevel.uppercase()),
                            confidence = response.confidence,
                            description = response.description,
                            camera = response.camera,
                            duration = kotlin.time.Duration.parse(response.duration),
                            resolution = response.resolution,
                            fps = response.fps
                        )
                    }
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch video recordings: ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch video recordings: ${e.message}")
            emptyList()
        }
    }

    suspend fun getVideoDetails(videoId: String): VideoRecording? {
        return try {
            Log.d(TAG, "Fetching video details for: $videoId")
            
            // First check if we have cached video details
            val cachedVideoUri = videoCacheManager.getCachedVideoUriIfExists(videoId)
            if (cachedVideoUri != null) {
                Log.d(TAG, "📁 Using cached video for $videoId")
                // Try to get video details from cache or minimal API call
                // For now, we still need API call for metadata, but use cached video URL
                return getVideoDetailsWithCachedUrl(videoId, cachedVideoUri.toString())
            }
            
            when (val result = networkClient.executeApiCall { apiService.getVideoDetails(videoId) }) {
                is NetworkResult.Success -> {
                    val response = result.data
                    Log.d(TAG, "✅ Video details retrieved: ${response.fileName} at ${response.filePath}")
                    
                    // Convert local file path to HTTP URL for streaming
                    val videoUrl = convertFilePathToHttpUrl(response.filePath, response.url)
                    Log.d(TAG, "🎬 Video URL: $videoUrl")
                    
                    // Get cached video URI (download if not cached)
                    val cachedVideoUriResult = videoCacheManager.getCachedVideoUri(videoUrl, videoId)
                    val finalVideoUrl = if (cachedVideoUriResult.scheme == "file") {
                        cachedVideoUriResult.toString()
                    } else {
                        videoUrl // Fallback to HTTP URL if caching failed
                    }
                    
                    Log.d(TAG, "📁 Final video URL (cached): $finalVideoUrl")
                    
                    VideoRecording(
                        id = response.id,
                        fileName = response.fileName,
                        filePath = response.filePath,
                        url = finalVideoUrl, // Use cached URL when available
                        timestamp = parseTimestamp(response.timestamp),
                        fileSize = response.fileSize,
                        threatLevel = ThreatLevel.valueOf(response.threatLevel.uppercase()),
                        confidence = response.confidence,
                        description = response.description,
                        camera = response.camera,
                        duration = kotlin.time.Duration.parse(response.duration),
                        resolution = response.resolution,
                        fps = response.fps,
                        analysisSession = parseAnalysisSession(response.analysisSession),
                        frameAnalyses = response.frameAnalyses.map { frame ->
                            FrameAnalysis(
                                batchNumber = frame.batchNumber,
                                frameRange = frame.frameRange,
                                timestampRange = frame.timestampRange,
                                analysis = frame.analysis,
                                framesInBatch = frame.framesInBatch
                            )
                        },
                        consolidatedAnalysis = response.consolidatedAnalysis,
                        keywords = response.keywords
                    )
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch video details: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch video details: ${e.message}")
            null
        }
    }
    
    /**
     * Get video details when we already have cached video URL
     */
    private suspend fun getVideoDetailsWithCachedUrl(videoId: String, cachedUrl: String): VideoRecording? {
        return try {
            // Make API call only for metadata, use cached URL for video
            when (val result = networkClient.executeApiCall { apiService.getVideoDetails(videoId) }) {
                is NetworkResult.Success -> {
                    val response = result.data
                    Log.d(TAG, "✅ Video metadata retrieved for cached video: ${response.fileName}")
                    
                    VideoRecording(
                        id = response.id,
                        fileName = response.fileName,
                        filePath = response.filePath,
                        url = cachedUrl, // Use cached URL directly
                        timestamp = parseTimestamp(response.timestamp),
                        fileSize = response.fileSize,
                        threatLevel = ThreatLevel.valueOf(response.threatLevel.uppercase()),
                        confidence = response.confidence,
                        description = response.description,
                        camera = response.camera,
                        duration = kotlin.time.Duration.parse(response.duration),
                        resolution = response.resolution,
                        fps = response.fps,
                        analysisSession = parseAnalysisSession(response.analysisSession),
                        frameAnalyses = response.frameAnalyses.map { frame ->
                            FrameAnalysis(
                                batchNumber = frame.batchNumber,
                                frameRange = frame.frameRange,
                                timestampRange = frame.timestampRange,
                                analysis = frame.analysis,
                                framesInBatch = frame.framesInBatch
                            )
                        },
                        consolidatedAnalysis = response.consolidatedAnalysis,
                        keywords = response.keywords
                    )
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch video metadata for cached video: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch video metadata for cached video: ${e.message}")
            null
        }
    }
    
    /**
     * Get all cached videos
     */
    fun getCachedVideos(): List<com.GemmaGuardian.securitymonitor.data.cache.CachedVideo> {
        return videoCacheManager.getCachedVideos()
    }
    
    /**
     * Check if a video is cached
     */
    fun isVideoCached(videoId: String): Boolean {
        return videoCacheManager.isVideoCached(videoId)
    }
    
    /**
     * Get cache information
     */
    fun getCacheInfo(): com.GemmaGuardian.securitymonitor.data.cache.CacheInfo {
        return videoCacheManager.getCacheInfo()
    }
    
    /**
     * Clear video cache
     */
    fun clearVideoCache(): Boolean {
        return videoCacheManager.clearCache()
    }
    
    /**
     * Convert local file path to HTTP URL for video streaming
     */
    private fun convertFilePathToHttpUrl(filePath: String, fallbackUrl: String): String {
        return when {
            // If URL is already provided, use it
            fallbackUrl.startsWith("http") -> fallbackUrl
            
            // Convert local file path to HTTP URL using current server configuration
            filePath.contains(configurationManager.getServerHost()) || filePath.startsWith("/") -> {
                // Extract filename from path
                val fileName = filePath.substringAfterLast("/")
                // Create HTTP URL using current server configuration
                "${configurationManager.getBaseUrl()}video/$fileName"
            }
            
            // For other file paths, try to construct HTTP URL
            else -> {
                val fileName = filePath.substringAfterLast("\\").substringAfterLast("/")
                "${configurationManager.getBaseUrl()}video/$fileName"
            }
        }
    }
    
    /**
     * Parse analysis session string to Map
     */
    private fun parseAnalysisSession(analysisSessionStr: String?): Map<String, Any>? {
        return if (analysisSessionStr.isNullOrBlank()) {
            null
        } else {
            try {
                // Try to parse as JSON if it's a JSON string
                // For now, create a simple map structure
                mapOf(
                    "raw_data" to analysisSessionStr,
                    "parsed" to true,
                    "timestamp" to Clock.System.now().toString()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse analysis session: ${e.message}")
                mapOf("raw_data" to analysisSessionStr)
            }
        }
    }

    suspend fun getCameraStatus(): List<CameraInfo> {
        return try {
            Log.d(TAG, "Fetching real camera status")
            when (val result = networkClient.executeApiCall { apiService.getCameraStatus() }) {
                is NetworkResult.Success -> {
                    result.data.map { response ->
                        CameraInfo(
                            id = response.id,
                            name = response.name,
                            location = response.location,
                            status = response.status,
                            isOnline = response.isOnline,
                            lastSeen = parseTimestamp(response.lastSeen),
                            resolution = response.resolution,
                            fps = response.fps,
                            storageUsed = response.storageUsed,
                            recordingEnabled = response.recordingEnabled
                        )
                    }
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch camera status: ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch camera status: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun acknowledgeAlert(alertId: String) {
        when (val result = networkClient.executeApiCall { apiService.acknowledgeAlert(alertId) }) {
            is NetworkResult.Success -> {
                // Successfully acknowledged
            }
            is NetworkResult.Error -> {
                throw Exception("Failed to acknowledge alert: ${result.message}")
            }
        }
    }
    
    suspend fun getSystemHealth(): SystemHealth {
        return when (val result = networkClient.executeApiCall { apiService.getSystemHealth() }) {
            is NetworkResult.Success -> {
                val response = result.data
                Log.d(TAG, "🔍 Received SystemHealth response: $response")
                Log.d(TAG, "🔍 System status from backend: ${response.status}")
                SystemHealth(
                    status = response.status,
                    uptime = if (response.systemUptime == "running") 1L else 0L, // Convert string to simple numeric representation
                    cpuUsage = 0.0, // Backend doesn't provide this yet
                    memoryUsage = 0.0, // Backend doesn't provide this yet
                    diskUsage = 0.0, // Backend doesn't provide this yet
                    temperature = 0.0, // Backend doesn't provide this yet
                    lastUpdate = parseTimestamp(response.timestamp)
                )
            }
            is NetworkResult.Error -> {
                Log.e(TAG, "Failed to fetch system health: ${result.message}")
                // Return empty/default system health instead of mock data
                SystemHealth(
                    status = "disconnected",
                    uptime = 0L,
                    cpuUsage = 0.0,
                    memoryUsage = 0.0,
                    diskUsage = 0.0,
                    temperature = 0.0,
                    lastUpdate = Clock.System.now()
                )
            }
        }
    }
    
    suspend fun getRecentAlerts(limit: Int = 10): List<SecurityAlert> {
        return getAllAlerts().take(limit)
    }

    // Removed polling getAlertsFlow() - alerts should come via UDP notifications
    // Use NotificationHandler.newAlerts flow for real-time alerts instead
    
    /**
     * Get video cache information
     */
    fun getVideoCacheInfo() = videoCacheManager.getCacheInfo()
}

data class VideoClipInfo(
    val id: String,
    val title: String,
    val timestamp: Instant,
    val duration: kotlin.time.Duration,
    val thumbnailUrl: String?,
    val videoUrl: String,
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val detectedObjects: List<String> = emptyList(),
    val fileSize: Long,
    val camera: String
)
