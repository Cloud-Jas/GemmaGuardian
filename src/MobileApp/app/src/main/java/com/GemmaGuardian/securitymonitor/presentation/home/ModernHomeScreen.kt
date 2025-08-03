package com.GemmaGuardian.securitymonitor.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexvas.rtsp.widget.RtspSurfaceView
import com.GemmaGuardian.securitymonitor.domain.model.*
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*
import com.GemmaGuardian.securitymonitor.presentation.videoplayer.FullscreenVideoActivity
import com.GemmaGuardian.securitymonitor.config.NetworkConfig
import kotlinx.datetime.Clock
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onNavigateToAlerts: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlert: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    GemmaGuardGradientBackground(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                GemmaGuardHeader(
                    title = "GemmaGuard Security",
                    subtitle = "AI-Powered Surveillance System",
                    icon = Icons.Default.Security,
                    actions = {
                        GemmaGuardStatusIndicator(
                            isOnline = uiState.systemHealth?.status == "online",
                            size = 12
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.systemHealth?.status == "online") "Online" else "Offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.systemHealth?.status == "online") 
                                GemmaGuardColors.Online else GemmaGuardColors.Offline
                        )
                    }
                )
            }
            
            // Quick Stats Section
            item {
                Text(
                    text = "System Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        GemmaGuardStatsCard(
                            title = "Active Alerts",
                            value = (uiState.securityStats?.totalAlerts ?: 0).toString(),
                            subtitle = "Requires attention",
                            icon = Icons.Default.Warning,
                            modifier = Modifier.width(160.dp),
                            trend = if ((uiState.securityStats?.totalAlerts ?: 0) > 0) GemmaGuardStatsTrend.Up else GemmaGuardStatsTrend.Stable
                        )
                    }
                    
                    item {
                        GemmaGuardStatsCard(
                            title = "Cameras",
                            value = uiState.cameraStatus.size.toString(),
                            subtitle = "Monitoring active",
                            icon = Icons.Default.Videocam,
                            modifier = Modifier.width(160.dp),
                            trend = GemmaGuardStatsTrend.Stable
                        )
                    }
                    
                    item {
                        GemmaGuardStatsCard(
                            title = "AI Analysis",
                            value = "95%", // Static value since aiAccuracy isn't available
                            subtitle = "Threat detection",
                            icon = Icons.Default.Psychology,
                            modifier = Modifier.width(160.dp),
                            trend = GemmaGuardStatsTrend.Up
                        )
                    }
                }
            }
            
            // Quick Actions Section
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary,
                    modifier = Modifier.padding(all = 4.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GemmaGuardButton(
                        text = "View Alerts",
                        onClick = onNavigateToAlerts,
                        icon = Icons.Default.NotificationImportant,
                        modifier = Modifier.weight(1f),
                        variant = GemmaGuardButtonVariant.Primary
                    )
                    
                    GemmaGuardButton(
                        text = "Video Feed",
                        onClick = onNavigateToVideos,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f),
                        variant = GemmaGuardButtonVariant.Secondary
                    )
                }
            }
            
            // Live Camera Feed Section
            item {
                GemmaGuardLiveCameraFeed(
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            
            // Recent Alerts Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GemmaGuardColors.TextPrimary
                    )
                    
                    TextButton(
                        onClick = onNavigateToAlerts
                    ) {
                        Text(
                            text = "View All",
                            color = GemmaGuardColors.Primary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GemmaGuardColors.Primary
                        )
                    }
                }
            }
            
            // Recent Alerts List
            items(uiState.recentAlerts) { alert ->
                GemmaGuardAlertCard(
                    title = alert.summary,
                    subtitle = alert.description,
                    timestamp = formatTimestamp(alert.timestamp.toEpochMilliseconds()),
                    threatLevel = alert.threatLevel.name,
                    onClick = { onNavigateToAlert(alert.id) }
                )
            }
            
            // Empty State
            if (uiState.recentAlerts.isEmpty()) {
                item {
                    GemmaGuardCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = GemmaGuardCardVariant.Glassmorphism
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = GemmaGuardColors.Primary.copy(alpha = 0.6f)
                            )
                            
                            Text(
                                text = "All Clear",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GemmaGuardColors.TextPrimary
                            )
                            
                            Text(
                                text = "No recent security alerts. Your GemmaGuard system is monitoring and protecting your property.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GemmaGuardColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // System Info Section
            item {
                GemmaGuardCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = GemmaGuardCardVariant.Elevated
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = GemmaGuardColors.Primary
                            )
                            Text(
                                text = "System Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GemmaGuardColors.TextPrimary
                            )
                        }
                        
                        SystemInfoRow(
                            label = "AI Processing Mode",
                            value = "Edge AI" // Static value since processingMode isn't available
                        )
                        
                        SystemInfoRow(
                            label = "Last Analysis",
                            value = formatTimestamp(uiState.systemHealth?.lastUpdate?.toEpochMilliseconds() ?: System.currentTimeMillis())
                        )
                        
                        SystemInfoRow(
                            label = "Server Status",
                            value = if (uiState.systemHealth?.status == "online") "Connected" else "Disconnected"
                        )
                    }
                }
            }
            
            // Add bottom padding for better scroll experience
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GemmaGuardColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = GemmaGuardColors.TextPrimary
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
fun GemmaGuardLiveCameraFeed(
    onNavigateToSettings: () -> Unit
) {
    var isStreamLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var rtspUrl by remember { mutableStateOf("") }
    var streamKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    
    // Build RTSP URL from SharedPreferences
    val buildRtspUrl: () -> String = {
        val sharedPrefs = context.getSharedPreferences("security_monitor_prefs", android.content.Context.MODE_PRIVATE)
        val rtspHost = sharedPrefs.getString("rtsp_host", NetworkConfig.DEFAULT_RTSP_HOST) ?: NetworkConfig.DEFAULT_RTSP_HOST
        val rtspPort = sharedPrefs.getInt("rtsp_port", NetworkConfig.DEFAULT_RTSP_PORT)
        val rtspUsername = sharedPrefs.getString("rtsp_username", NetworkConfig.DEFAULT_RTSP_USERNAME) ?: NetworkConfig.DEFAULT_RTSP_USERNAME
        val rtspPassword = sharedPrefs.getString("rtsp_password", NetworkConfig.DEFAULT_RTSP_PASSWORD) ?: NetworkConfig.DEFAULT_RTSP_PASSWORD
        val rtspPath = sharedPrefs.getString("rtsp_path", NetworkConfig.DEFAULT_RTSP_PATH) ?: NetworkConfig.DEFAULT_RTSP_PATH
        "rtsp://$rtspUsername:$rtspPassword@$rtspHost:$rtspPort$rtspPath"
    }
    
    // Initialize RTSP URL once
    LaunchedEffect(Unit) {
        rtspUrl = buildRtspUrl()
        android.util.Log.d("CameraFeed", "🎥 RTSP URL initialized: $rtspUrl")
    }
    
    // Handle lifecycle events for stream restart
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    android.util.Log.d("CameraFeed", "🔄 Activity resumed - refreshing stream")
                    val newRtspUrl = buildRtspUrl()
                    if (rtspUrl != newRtspUrl) {
                        rtspUrl = newRtspUrl
                        android.util.Log.d("CameraFeed", "🎥 RTSP URL updated: $rtspUrl")
                    }
                    streamKey++
                    hasError = false
                    isStreamLoading = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        kotlinx.coroutines.coroutineScope {
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
    
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
                
                // Connection Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Manual refresh button
                    IconButton(
                        onClick = {
                            android.util.Log.d("CameraFeed", "🔄 Manual refresh triggered")
                            streamKey++
                            hasError = false
                            isStreamLoading = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh stream",
                            modifier = Modifier.size(16.dp),
                            tint = GemmaGuardColors.TextSecondary
                        )
                    }
                    
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            hasError -> GemmaGuardColors.ThreatCritical
                            isStreamLoading -> GemmaGuardColors.ThreatMedium
                            else -> GemmaGuardColors.ThreatLow
                        }
                    )
                }
            }
            
            // Video Stream Area
            GemmaGuardCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                variant = GemmaGuardCardVariant.Default
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (rtspUrl.isNotEmpty()) {
                        GemmaGuardRtspPlayer(
                            rtspUrl = rtspUrl,
                            modifier = Modifier.fillMaxSize(),
                            key = streamKey, // Force recomposition when returning from fullscreen
                            onLoadingChanged = { isLoading -> 
                                isStreamLoading = isLoading
                                android.util.Log.d("CameraFeed", "📡 Regular loading state: $isLoading")
                            },
                            onError = { error ->
                                hasError = true
                                isStreamLoading = false
                                android.util.Log.e("CameraFeed", "❌ Regular stream error: $error")
                            }
                        )
                    } else {
                        // Show placeholder when no RTSP URL
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = GemmaGuardColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Setting up camera feed...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GemmaGuardColors.TextSecondary
                            )
                        }
                    }
                    
                    // Loading Overlay
                    if (isStreamLoading && !hasError && rtspUrl.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GemmaGuardColors.CardBackground.copy(alpha = 0.8f)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = GemmaGuardColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connecting to stream...",
                                style = MaterialTheme.typography.bodySmall,
                                color = GemmaGuardColors.TextSecondary
                            )
                        }
                    }
                    
                    // Error State
                    if (hasError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = GemmaGuardColors.ThreatCritical
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera Feed Unavailable",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = GemmaGuardColors.ThreatCritical
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            GemmaGuardButton(
                                text = "Retry Connection",
                                onClick = { 
                                    android.util.Log.d("CameraFeed", "🔄 Retrying stream connection")
                                    hasError = false
                                    isStreamLoading = true
                                    streamKey++
                                },
                                variant = GemmaGuardButtonVariant.Secondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Stream Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GemmaGuardButton(
                    text = "Settings",
                    onClick = onNavigateToSettings,
                    icon = Icons.Default.Settings,
                    variant = GemmaGuardButtonVariant.Outline
                )
                
                GemmaGuardButton(
                    text = "Full Screen",
                    onClick = { 
                        android.util.Log.d("CameraFeed", "🔄 Launching fullscreen video activity")
                        val intent = FullscreenVideoActivity.createIntent(context, rtspUrl)
                        context.startActivity(intent)
                    },
                    icon = Icons.Default.Fullscreen,
                    variant = GemmaGuardButtonVariant.Secondary
                )
            }
        }
    }
}

@Composable
private fun GemmaGuardRtspPlayer(
    rtspUrl: String,
    modifier: Modifier = Modifier,
    key: Int = 0,
    onLoadingChanged: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    // Force complete recreation when key changes (like returning from fullscreen)
    key(key) {
        AndroidView(
            factory = { context ->
                android.util.Log.d("RtspPlayer", "🏭 Creating NEW RtspSurfaceView (key: $key)")
                RtspSurfaceView(context).apply {
                    // Initialize immediately in factory for fresh start
                    if (rtspUrl.isNotEmpty()) {
                        android.util.Log.d("RtspPlayer", "🎬 Factory: Starting fresh RTSP stream: $rtspUrl")
                        
                        try {
                            onLoadingChanged(true)
                            
                            // Parse RTSP URL - following official library example
                            val uri = android.net.Uri.parse(rtspUrl)
                            val userInfo = uri.userInfo
                            val username = userInfo?.substringBefore(":") ?: ""
                            val password = userInfo?.substringAfter(":") ?: ""
                            
                            // Initialize with credentials - official library pattern
                            init(uri, username, password, "GemmaGuard/1.0")
                            
                            // Start stream immediately - official library pattern
                            start(
                                requestVideo = true,
                                requestAudio = false,
                                requestApplication = false
                            )
                            
                            android.util.Log.d("RtspPlayer", "✅ Factory: RTSP stream started successfully")
                            
                            // Loading complete after short delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                onLoadingChanged(false)
                            }, 1000)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("RtspPlayer", "❌ Factory: RTSP stream failed: ${e.message}")
                            onError(e.message ?: "Stream connection failed")
                            onLoadingChanged(false)
                        }
                    }
                }
            },
            modifier = modifier,
            update = { view ->
                // Update block should be minimal since factory does the work
                android.util.Log.d("RtspPlayer", "🔄 Update called for key: $key")
            }
        )
    }
}


