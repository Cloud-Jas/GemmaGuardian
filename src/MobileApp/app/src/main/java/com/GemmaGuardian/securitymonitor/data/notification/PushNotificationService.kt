package com.GemmaGuardian.securitymonitor.data.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.GemmaGuardian.securitymonitor.data.network.SecurityNetworkClient
import com.GemmaGuardian.securitymonitor.domain.model.SecurityAlert
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.*
import javax.inject.Inject

@AndroidEntryPoint
class PushNotificationService : Service() {
    
    @Inject
    lateinit var networkClient: SecurityNetworkClient
    
    @Inject
    lateinit var notificationHandler: NotificationHandler
    
    @Inject
    lateinit var notificationPreferences: NotificationPreferences
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var udpListenerJob: Job? = null
    private var httpListenerJob: Job? = null
    
    companion object {
        private const val UDP_LISTEN_PORT = 9999  // Match surveillance system UDP broadcast port
        private const val HTTP_LISTEN_PORT = 8890
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("PushNotificationService", "🚀 Service starting...")
        
        // Start as foreground service to keep running in background
        startForegroundService()
        
        startListening()
        android.util.Log.d("PushNotificationService", "✅ Service started successfully as foreground service")
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
    }
    
    private fun startListening() {
        if (!notificationPreferences.arePushNotificationsEnabled()) {
            android.util.Log.w("PushNotificationService", "⚠️ Push notifications are disabled in preferences")
            return
        }
        
        android.util.Log.d("PushNotificationService", "🎧 Starting UDP and HTTP listeners...")
        
        // Start UDP listener for real-time notifications
        udpListenerJob = serviceScope.launch {
            startUdpListener()
        }
        
        // Start HTTP webhook listener
        httpListenerJob = serviceScope.launch {
            startHttpListener()
        }
        
        android.util.Log.d("PushNotificationService", "✅ Both listeners started")
    }
    
    private fun stopListening() {
        udpListenerJob?.cancel()
        httpListenerJob?.cancel()
    }

    /**
     * Start the service as a foreground service to keep it running in background
     */
    private fun startForegroundService() {
        try {
            val notification = androidx.core.app.NotificationCompat.Builder(this, "system_notifications_v2")
                .setContentTitle("Security Monitor Active")
                .setContentText("Listening for security alerts from surveillance system")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .build()
            
            startForeground(9998, notification)
            android.util.Log.d("PushNotificationService", "✅ Started as foreground service with notification")
        } catch (e: Exception) {
            android.util.Log.e("PushNotificationService", "❌ Failed to start foreground service: ${e.message}")
        }
    }
    
    private suspend fun startUdpListener() {
        try {
            val socket = DatagramSocket(UDP_LISTEN_PORT)
            socket.reuseAddress = true
            
            android.util.Log.d("PushNotificationService", "✅ UDP listener started on port $UDP_LISTEN_PORT")
            
            while (serviceScope.isActive) {
                try {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(buffer, buffer.size)
                    
                    android.util.Log.d("PushNotificationService", "🎧 Waiting for UDP packet...")
                    socket.receive(packet)
                    
                    val message = String(packet.data, 0, packet.length)
                    android.util.Log.d("PushNotificationService", "📦 Received UDP message: $message")
                    processNotificationMessage(message)
                    
                } catch (e: SocketTimeoutException) {
                    // Continue listening
                    android.util.Log.d("PushNotificationService", "⏰ UDP socket timeout, continuing...")
                } catch (e: Exception) {
                    android.util.Log.e("PushNotificationService", "❌ UDP listener error: ${e.message}", e)
                    delay(1000) // Avoid busy loop on persistent errors
                }
            }
            
            socket.close()
            android.util.Log.d("PushNotificationService", "🔌 UDP socket closed")
            
        } catch (e: Exception) {
            android.util.Log.e("PushNotificationService", "❌ Failed to start UDP listener: ${e.message}", e)
        }
    }
    
    private suspend fun startHttpListener() {
        try {
            val serverSocket = ServerSocket(HTTP_LISTEN_PORT)
            
            while (serviceScope.isActive) {
                try {
                    val clientSocket = serverSocket.accept()
                    
                    // Handle each request in a separate coroutine
                    serviceScope.launch {
                        handleHttpRequest(clientSocket)
                    }
                    
                } catch (e: Exception) {
                    // Log error and continue
                    delay(1000)
                }
            }
            
            serverSocket.close()
            
        } catch (e: Exception) {
            // Failed to start HTTP listener
        }
    }
    
    private suspend fun handleHttpRequest(socket: Socket) {
        try {
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream().bufferedWriter()
            
            val requestLine = input.readLine()
            val headers = mutableMapOf<String, String>()
            
            // Read headers
            var line = input.readLine()
            while (line.isNotEmpty()) {
                val parts = line.split(": ", limit = 2)
                if (parts.size == 2) {
                    headers[parts[0].lowercase()] = parts[1]
                }
                line = input.readLine()
            }
            
            // Read body if present
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                input.read(buffer, 0, contentLength)
                String(buffer)
            } else ""
            
            // Process notification
            if (requestLine?.contains("POST") == true && body.isNotEmpty()) {
                processNotificationMessage(body)
            }
            
            // Send response
            output.write("HTTP/1.1 200 OK\r\n")
            output.write("Content-Type: application/json\r\n")
            output.write("Content-Length: 22\r\n")
            output.write("\r\n")
            output.write("{\"status\": \"received\"}")
            output.flush()
            
            socket.close()
            
        } catch (e: Exception) {
            // Handle request error
            try {
                socket.close()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }
    
    private fun processNotificationMessage(message: String) {
        try {
            android.util.Log.d("PushNotificationService", "🔍 Processing message: $message")
            
            val notification = Json.decodeFromString<SecurityNotification>(message)
            android.util.Log.d("PushNotificationService", "✅ Parsed notification: ${notification.summary}")
            
            // Handle different notification types
            when (notification.type) {
                "test_notification" -> {
                    android.util.Log.d("PushNotificationService", "🧪 Test notification received from ${notification.server_info?.ip}")
                    return
                }
                "security_alert" -> {
                    val alert = SecurityAlert(
                        id = notification.id,
                        timestamp = Instant.parse(notification.timestamp),
                        threatLevel = ThreatLevel.valueOf(notification.threatLevel.uppercase()),
                        confidence = notification.confidence,
                        isThreatDetected = notification.is_threat_detected,
                        summary = notification.summary,
                        keywords = notification.keywords,
                        description = notification.summary, // Use summary as description
                        camera = notification.camera,
                        isAcknowledged = false,
                        videoClip = notification.video_id?.let { videoId ->
                            // Create a lightweight video clip reference for the notification
                            // The full details will be fetched via HTTP when user taps notification
                            com.GemmaGuardian.securitymonitor.domain.model.VideoClip(
                                id = videoId,
                                url = notification.api_endpoint ?: "",
                                fileName = notification.video_filename ?: "${videoId}.mp4",
                                filePath = "", // Will be populated when fetched via API
                                thumbnailUrl = null, // Will be populated when fetched via API
                                timestamp = Instant.parse(notification.timestamp),
                                duration = kotlin.time.Duration.parse("PT2M0S") // Default duration
                            )
                        }
                    )
                    
                    android.util.Log.d("PushNotificationService", "📱 Showing security alert notification...")
                    // Show notification - when user taps, it will use the API to get full details
                    notificationHandler.showSecurityAlert(alert)
                    android.util.Log.d("PushNotificationService", "✅ Security alert notification shown successfully")
                }
                else -> {
                    android.util.Log.w("PushNotificationService", "⚠️ Unknown notification type: ${notification.type}")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("PushNotificationService", "❌ Failed to parse/show notification: ${e.message}", e)
            // Try to handle legacy format
            try {
                // Fallback for older notification formats
                android.util.Log.d("PushNotificationService", "🔄 Trying legacy format parsing...")
                val legacyAlert = parseLegacyNotification(message)
                if (legacyAlert != null) {
                    notificationHandler.showSecurityAlert(legacyAlert)
                    android.util.Log.d("PushNotificationService", "✅ Legacy notification processed")
                }
            } catch (legacyError: Exception) {
                android.util.Log.e("PushNotificationService", "❌ Legacy parsing also failed: ${legacyError.message}")
            }
        }
    }
    
    private fun parseLegacyNotification(message: String): SecurityAlert? {
        // Implementation for parsing older notification formats if needed
        return null
    }
}

@Serializable
data class SecurityNotification(
    val type: String = "security_alert",
    val id: String,
    val timestamp: String,
    val threatLevel: String,
    val confidence: Float,
    val summary: String,
    val camera: String,
    val keywords: List<String> = emptyList(),
    val video_id: String? = null,
    val video_filename: String? = null,
    val is_threat_detected: Boolean = true,
    val api_endpoint: String? = null,
    val server_info: ServerInfo? = null,
    // Legacy fields for backward compatibility
    val description: String = summary,
    val videoClip: VideoClipNotification? = null
)

@Serializable
data class ServerInfo(
    val ip: String,
    val port: Int
)

@Serializable
data class VideoClipNotification(
    val id: String,
    val url: String,
    val thumbnailUrl: String?,
    val duration: String
)
