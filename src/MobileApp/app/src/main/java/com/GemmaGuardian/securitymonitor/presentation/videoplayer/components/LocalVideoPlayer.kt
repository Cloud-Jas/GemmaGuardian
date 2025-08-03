package com.GemmaGuardian.securitymonitor.presentation.videoplayer.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.GemmaGuardian.securitymonitor.config.ConfigurationManager
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors

@Composable
fun LocalVideoPlayer(
    videoFileName: String,
    configurationManager: ConfigurationManager,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    videoUrl: String? = null // Add support for direct URL
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    // Initialize ExoPlayer
    LaunchedEffect(videoFileName, videoUrl) {
        val player = ExoPlayer.Builder(context).build()
        
        // Determine the video URI to use
        val uri = when {
            // Use provided URL if available
            !videoUrl.isNullOrBlank() -> {
                if (videoUrl.startsWith("file://")) {
                    Log.d("LocalVideoPlayer", "📁 Using cached file: $videoUrl")
                } else {
                    Log.d("LocalVideoPlayer", "🎬 Using provided URL: $videoUrl")
                }
                android.net.Uri.parse(videoUrl)
            }
            
            // Try to construct HTTP URL from filename
            videoFileName.isNotBlank() -> {
                val httpUrl = "${configurationManager.getBaseUrl()}video/$videoFileName"
                Log.d("LocalVideoPlayer", "🌐 Constructed HTTP URL: $httpUrl")
                android.net.Uri.parse(httpUrl)
            }
            
            // Fallback to sample video
            else -> {
                Log.d("LocalVideoPlayer", "📺 Using fallback sample video")
                android.net.Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            }
        }
        
        try {
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            
            // Auto-play the video once it's ready
            player.playWhenReady = true
            
            // Add error listener
            player.addListener(object : com.google.android.exoplayer2.Player.Listener {
                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    Log.e("LocalVideoPlayer", "❌ Video playback error: ${error.message}")
                    Log.e("LocalVideoPlayer", "❌ Error cause: ${error.cause}")
                    Log.e("LocalVideoPlayer", "❌ Error type: ${error.errorCode}")
                    
                    val detailedMessage = when {
                        error.message?.contains("Unable to connect", ignoreCase = true) == true -> 
                            "Cannot connect to video server. Check network connection."
                        error.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Connection timeout. Server may be offline."
                        error.message?.contains("not found", ignoreCase = true) == true -> 
                            "Video file not found on server."
                        else -> "Failed to load video: ${error.message}"
                    }
                    
                    hasError = true
                    isLoading = false
                    errorMessage = detailedMessage
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        com.google.android.exoplayer2.Player.STATE_BUFFERING -> {
                            Log.d("LocalVideoPlayer", "🔄 Video buffering")
                            isLoading = true
                        }
                        com.google.android.exoplayer2.Player.STATE_READY -> {
                            Log.d("LocalVideoPlayer", "✅ Video ready to play")
                            hasError = false
                            isLoading = false
                        }
                        com.google.android.exoplayer2.Player.STATE_ENDED -> {
                            Log.d("LocalVideoPlayer", "🏁 Video playback ended")
                            isLoading = false
                        }
                        com.google.android.exoplayer2.Player.STATE_IDLE -> {
                            Log.d("LocalVideoPlayer", "⏸️ Video player idle")
                            isLoading = false
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                    Log.d("LocalVideoPlayer", "▶️ Playing state changed: $isPlayingParam")
                    isPlaying = isPlayingParam
                }
            })
            
            exoPlayer = player
        } catch (e: Exception) {
            Log.e("LocalVideoPlayer", "💥 Failed to initialize player: ${e.message}")
            hasError = true
            errorMessage = "Failed to initialize video player: ${e.message}"
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Don't auto-resume, let user control
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
        }
    }
    
    // Update playback position
    LaunchedEffect(exoPlayer) {
        exoPlayer?.let { player ->
            while (true) {
                currentPosition = player.currentPosition
                duration = player.duration.takeIf { it > 0 } ?: 0L
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Video Player View
        AndroidView(
            factory = { context ->
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    useController = false // We'll create custom controls
                    setBackgroundColor(Color.Black.toArgb())
                    
                    // Ensure video surface is visible
                    setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(false)
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Error overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Video Error",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = errorMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Debug info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Debug Info:",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "URL: ${videoUrl ?: "Generated from filename"}",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Filename: $videoFileName",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                hasError = false
                                isLoading = true
                                
                                // Retry by reinitializing the player
                                exoPlayer?.release()
                                
                                // Trigger recomposition to reinitialize player
                                val currentVideoUrl = videoUrl
                                val currentFileName = videoFileName
                                
                                // Reset and reinitialize
                                exoPlayer = null
                                
                                // This will trigger the LaunchedEffect again
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retry")
                        }
                        
                        Button(
                            onClick = { 
                                hasError = false
                                isLoading = true
                                
                                // Test with sample video
                                exoPlayer?.release()
                                exoPlayer = null
                                
                                // This will trigger fallback to sample video
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GemmaGuardColors.ThreatHigh
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test")
                        }
                        
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GemmaGuardColors.Primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        // Loading overlay
        if (isLoading && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = GemmaGuardColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Loading video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Debug info for loading
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            val sourceUrl = videoUrl ?: "${configurationManager.getBaseUrl()}video/$videoFileName"
                            val isCached = sourceUrl.startsWith("file://")
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCached) Icons.Default.Storage else Icons.Default.CloudDownload,
                                    contentDescription = if (isCached) "Cached" else "Downloading",
                                    tint = if (isCached) Color.Green else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCached) "Playing from cache" else "Downloading and caching...",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            Text(
                                text = "Source: $sourceUrl",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Custom Controls Overlay (only show if no error)
        if (showControls && !hasError) {
            VideoControlsOverlay(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = {
                    exoPlayer?.let { player ->
                        Log.d("LocalVideoPlayer", "🎮 Play/Pause clicked. Current state - isPlaying: ${player.isPlaying}, playWhenReady: ${player.playWhenReady}")
                        if (player.isPlaying) {
                            player.pause()
                            Log.d("LocalVideoPlayer", "⏸️ Pausing video")
                        } else {
                            player.play()
                            Log.d("LocalVideoPlayer", "▶️ Playing video")
                        }
                    } ?: Log.e("LocalVideoPlayer", "❌ ExoPlayer is null when trying to play/pause")
                },
                onSeek = { position ->
                    exoPlayer?.seekTo(position)
                },
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
                videoUrl = videoUrl
            )
        }
        
        // Tap to toggle controls (only if no error)
        if (!hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable { showControls = !showControls }
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    videoUrl: String? = null
) {
    Box(modifier = modifier) {
        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Security Footage",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            
            // Placeholder for additional controls
            Spacer(modifier = Modifier.size(48.dp))
        }
        
        // Center play/pause button
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            // Progress bar
            val progress = if (duration > 0) {
                (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
            
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val newPosition = (newProgress * duration).toLong()
                    onSeek(newPosition)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
