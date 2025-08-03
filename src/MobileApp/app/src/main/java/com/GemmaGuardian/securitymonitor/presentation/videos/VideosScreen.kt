package com.GemmaGuardian.securitymonitor.presentation.videos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.GemmaGuardian.securitymonitor.domain.model.*
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class VideoSortBy(val displayName: String) {
    DATE_DESC("Latest First"),
    DATE_ASC("Oldest First"),
    DURATION_DESC("Longest First"),
    DURATION_ASC("Shortest First"),
    THREAT_LEVEL("Threat Level")
}

enum class VideoFilterBy(val displayName: String) {
    ALL("All Videos"),
    SECURITY_EVENTS("Security Events"),
    MOTION_DETECTED("Motion Detected"),
    PERSON_DETECTED("Person Detected"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    innerPadding: PaddingValues,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideosViewModel = hiltViewModel()
) {
    val videoRecordings by viewModel.videoRecordings.collectAsState()
    val cachedVideos by viewModel.cachedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showOnlyCached by viewModel.showOnlyCached.collectAsState()
    
    var showFilters by remember { mutableStateOf(false) }
    
    // Combine cached and remote videos for display
    val displayVideos = if (showOnlyCached) {
        // Convert cached videos to VideoRecording for display
        cachedVideos.map { cached ->
            VideoRecording(
                id = cached.videoId,
                fileName = cached.fileName,
                filePath = cached.filePath,
                url = cached.uri.toString(),
                timestamp = Instant.fromEpochMilliseconds(cached.lastModified),
                fileSize = cached.fileSize,
                threatLevel = ThreatLevel.LOW, // Default for cached
                confidence = 1.0f,
                description = "Cached video",
                camera = "Unknown",
                duration = kotlin.time.Duration.ZERO,
                resolution = "Unknown",
                fps = 0
            )
        }
    } else {
        videoRecordings
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = GemmaGuardColors.backgroundGradientColors
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Header
            ModernVideosHeader(
                videoCount = displayVideos.size,
                showOnlyCached = showOnlyCached,
                onToggleFilters = { showFilters = !showFilters },
                onToggleCachedView = { viewModel.toggleCachedView() },
                onRefresh = { viewModel.refreshVideos() }
            )
            
            // Error Display
            error?.let { errorMessage ->
                GemmaGuardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    variant = GemmaGuardCardVariant.Default
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = GemmaGuardColors.ThreatCritical
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GemmaGuardColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Text(
                                text = "Dismiss",
                                color = GemmaGuardColors.Primary
                            )
                        }
                    }
                }
            }
            
            // Animated Filters
            AnimatedVisibility(
                visible = showFilters,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                ModernVideoFilters(
                    modifier = Modifier.padding(20.dp)
                )
            }
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GemmaGuardColors.Primary
                    )
                }
            }
            
            // Videos Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayVideos) { video ->
                    ModernVideoCard(
                        video = video,
                        isCached = viewModel.isVideoCached(video.id),
                        onClick = { onNavigateToPlayer(video.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernVideosHeader(
    videoCount: Int,
    showOnlyCached: Boolean,
    onToggleFilters: () -> Unit,
    onToggleCachedView: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (showOnlyCached) "Cached Videos" else "Video Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
                
                Text(
                    text = "$videoCount ${if (showOnlyCached) "cached" else "recordings"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemmaGuardColors.TextSecondary
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cache toggle button
                IconButton(
                    onClick = onToggleCachedView,
                    modifier = Modifier
                        .background(
                            color = if (showOnlyCached) {
                                GemmaGuardColors.Primary.copy(alpha = 0.2f)
                            } else {
                                GemmaGuardColors.Primary.copy(alpha = 0.1f)
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = if (showOnlyCached) "Show all videos" else "Show cached videos",
                        tint = if (showOnlyCached) {
                            GemmaGuardColors.Primary
                        } else {
                            GemmaGuardColors.Primary.copy(alpha = 0.6f)
                        }
                    )
                }
                
                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .background(
                            GemmaGuardColors.Primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = GemmaGuardColors.Primary
                    )
                }
                
                // Filter button
                IconButton(
                    onClick = onToggleFilters,
                    modifier = Modifier
                        .background(
                            GemmaGuardColors.Primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = GemmaGuardColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernVideoFilters(
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier,
        variant = GemmaGuardCardVariant.Default
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filter & Sort",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GemmaGuardColors.TextPrimary
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("All") }
                    )
                }
                
                item {
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text("High Threat") }
                    )
                }
                
                item {
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text("Recent") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernVideoCard(
    video: VideoRecording,
    isCached: Boolean = false,
    onClick: () -> Unit
) {
    GemmaGuardCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        variant = GemmaGuardCardVariant.Default
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        GemmaGuardColors.Primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play video",
                        modifier = Modifier.size(32.dp),
                        tint = GemmaGuardColors.Primary
                    )
                    
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = GemmaGuardColors.TextSecondary
                    )
                }
                
                // Threat level badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    ThreatLevelBadge(
                        threatLevel = video.threatLevel,
                        showText = false
                    )
                }
                
                // Cache indicator
                if (isCached) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                GemmaGuardColors.Primary.copy(alpha = 0.9f),
                                CircleShape
                            )
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Cached",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = video.camera,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GemmaGuardColors.TextPrimary
                )
                
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GemmaGuardColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(video.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = GemmaGuardColors.TextSecondary
                    )
                    
                    if (isCached) {
                        Text(
                            text = "Cached",
                            style = MaterialTheme.typography.labelSmall,
                            color = GemmaGuardColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(duration: kotlin.time.Duration): String {
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

private fun formatTimestamp(timestamp: Instant): String {
    val localDateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber}/${localDateTime.dayOfMonth} ${
        localDateTime.hour.toString().padStart(2, '0')
    }:${localDateTime.minute.toString().padStart(2, '0')}"
}


