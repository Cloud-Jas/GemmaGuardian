package com.GemmaGuardian.securitymonitor.presentation.videoplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alexvas.rtsp.widget.RtspSurfaceView
import com.GemmaGuardian.securitymonitor.presentation.components.GemmaGuardButton
import com.GemmaGuardian.securitymonitor.presentation.components.GemmaGuardButtonVariant
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.theme.SecurityMonitorTheme
import java.text.SimpleDateFormat
import java.util.*

class FullscreenVideoActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_RTSP_URL = "rtsp_url"
        
        fun createIntent(context: Context, rtspUrl: String): Intent {
            return Intent(context, FullscreenVideoActivity::class.java).apply {
                putExtra(EXTRA_RTSP_URL, rtspUrl)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set landscape orientation for this activity only
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Make fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val rtspUrl = intent.getStringExtra(EXTRA_RTSP_URL) ?: ""
        
        setContent {
            SecurityMonitorTheme {
                FullscreenVideoPlayerScreen(
                    rtspUrl = rtspUrl,
                    onExitFullscreen = { finish() }
                )
            }
        }
    }
}

@Composable
private fun FullscreenVideoPlayerScreen(
    rtspUrl: String,
    onExitFullscreen: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var isStreamLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        }
    }
    
    // Handle back button
    BackHandler {
        onExitFullscreen()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isControlsVisible = !isControlsVisible }
    ) {
        // Video Player
        if (rtspUrl.isNotEmpty()) {
            FullscreenRtspPlayer(
                rtspUrl = rtspUrl,
                modifier = Modifier.fillMaxSize(),
                onLoadingChanged = { isLoading -> 
                    isStreamLoading = isLoading
                    android.util.Log.d("FullscreenVideo", "📡 Loading state: $isLoading")
                },
                onError = { error ->
                    hasError = true
                    isStreamLoading = false
                    android.util.Log.e("FullscreenVideo", "❌ Stream error: $error")
                }
            )
        }
        
        // Loading Overlay
        if (isStreamLoading && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = GemmaGuardColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Live Feed...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
        
        // Error Overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = GemmaGuardColors.ThreatCritical
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Feed Unavailable",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check camera connection and try again",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    GemmaGuardButton(
                        text = "Retry Connection",
                        onClick = { 
                            hasError = false
                            isStreamLoading = true
                        },
                        variant = GemmaGuardButtonVariant.Primary
                    )
                }
            }
        }
        
        // Controls Overlay (Top)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button
                IconButton(
                    onClick = onExitFullscreen,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Title and status
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Video Feed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        hasError -> GemmaGuardColors.ThreatCritical
                                        isStreamLoading -> GemmaGuardColors.ThreatMedium
                                        else -> GemmaGuardColors.ThreatLow
                                    },
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Text(
                            text = when {
                                hasError -> "OFFLINE"
                                isStreamLoading -> "CONNECTING"
                                else -> "LIVE"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Controls Overlay (Bottom)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stream info
                Column {
                    Text(
                        text = "Security Camera",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { /* TODO: Take screenshot */ },
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Screenshot",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Record video */ },
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Record Video",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenRtspPlayer(
    rtspUrl: String,
    modifier: Modifier = Modifier,
    onLoadingChanged: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            RtspSurfaceView(context).apply {
                try {
                    onLoadingChanged(true)
                    
                    // Parse RTSP URL and extract credentials
                    val uri = android.net.Uri.parse(rtspUrl)
                    val userInfo = uri.userInfo
                    val username = userInfo?.substringBefore(":") ?: ""
                    val password = userInfo?.substringAfter(":") ?: ""
                    
                    android.util.Log.d("FullscreenRtsp", "🎥 Initializing fullscreen RTSP stream: $rtspUrl")
                    
                    // Initialize the RTSP connection
                    init(uri, username, password, "GemmaGuard Security Monitor")
                    
                    // Start the stream with a delay for proper initialization
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            start(
                                requestVideo = true,
                                requestAudio = false,
                                requestApplication = false
                            )
                            android.util.Log.d("FullscreenRtsp", "✅ Fullscreen RTSP stream started successfully")
                            
                            // Mark as loaded after stream starts
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                onLoadingChanged(false)
                            }, 1500)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("FullscreenRtsp", "❌ Failed to start fullscreen RTSP stream: ${e.message}")
                            onError(e.message ?: "Failed to start stream")
                            onLoadingChanged(false)
                        }
                    }, 100)
                    
                } catch (e: Exception) {
                    android.util.Log.e("FullscreenRtsp", "❌ Failed to initialize fullscreen RTSP: ${e.message}")
                    onError(e.message ?: "Stream connection failed")
                    onLoadingChanged(false)
                }
            }
        },
        modifier = modifier,
        onRelease = { view ->
            try {
                android.util.Log.d("FullscreenRtsp", "🛑 Releasing fullscreen RTSP stream")
                view.stop()
            } catch (e: Exception) {
                android.util.Log.e("FullscreenRtsp", "❌ Error releasing fullscreen RTSP stream: ${e.message}")
            }
        }
    )
}
