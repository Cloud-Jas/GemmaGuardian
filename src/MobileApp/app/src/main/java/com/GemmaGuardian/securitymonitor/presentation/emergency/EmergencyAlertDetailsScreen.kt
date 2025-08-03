package com.GemmaGuardian.securitymonitor.presentation.emergency

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GemmaGuardian.securitymonitor.domain.model.SecurityAlert
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Enhanced alert details screen specifically for emergency (CRITICAL/HIGH) alerts
 * Features emergency styling, larger UI elements, and quick action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertDetailsScreen(
    alert: SecurityAlert,
    onBackClick: () -> Unit,
    onAcknowledge: () -> Unit,
    onPlayVideo: () -> Unit,
    onStopAlarm: () -> Unit,
    isAlarmPlaying: Boolean,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val alertColor = when (alert.threatLevel) {
        ThreatLevel.CRITICAL -> Color(0xFFDC2626)
        ThreatLevel.HIGH -> Color(0xFFEA580C)
        else -> Color(0xFFDC2626)
    }

    // Pulsing animation for critical alerts
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_details_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        alertColor.copy(alpha = 0.1f),
                        Color.Transparent,
                        alertColor.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        // Emergency Top Bar
        EmergencyTopBar(
            alert = alert,
            onBackClick = onBackClick,
            onStopAlarm = onStopAlarm,
            isAlarmPlaying = isAlarmPlaying,
            alertColor = alertColor
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Emergency Alert Header
                EmergencyAlertHeader(
                    alert = alert,
                    alertColor = alertColor,
                    pulseAlpha = pulseAlpha
                )
            }

            item {
                // Quick Action Buttons
                EmergencyActionButtons(
                    onAcknowledge = onAcknowledge,
                    onPlayVideo = onPlayVideo,
                    onStopAlarm = onStopAlarm,
                    isAlarmPlaying = isAlarmPlaying,
                    hasVideo = alert.videoClip != null,
                    isAcknowledged = alert.isAcknowledged,
                    alertColor = alertColor
                )
            }

            item {
                // Threat Analysis Card
                EmergencyThreatAnalysisCard(
                    alert = alert,
                    alertColor = alertColor
                )
            }

            item {
                // Keywords and Detection Info
                EmergencyKeywordsCard(
                    alert = alert,
                    alertColor = alertColor
                )
            }

            if (alert.videoClip != null) {
                item {
                    // Video Information Card
                    EmergencyVideoCard(
                        videoClip = alert.videoClip!!,
                        onPlayVideo = onPlayVideo,
                        alertColor = alertColor
                    )
                }
            }

            item {
                // Timeline and Status
                EmergencyTimelineCard(
                    alert = alert,
                    alertColor = alertColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyTopBar(
    alert: SecurityAlert,
    onBackClick: () -> Unit,
    onStopAlarm: () -> Unit,
    isAlarmPlaying: Boolean,
    alertColor: Color
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY ALERT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = alertColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (isAlarmPlaying) {
                // Stop Alarm Button
                IconButton(
                    onClick = onStopAlarm,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(alertColor.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Stop Alarm",
                        tint = alertColor
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}

@Composable
private fun EmergencyAlertHeader(
    alert: SecurityAlert,
    alertColor: Color,
    pulseAlpha: Float
) {
    GemmaGuardCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = alertColor.copy(alpha = pulseAlpha),
                shape = RoundedCornerShape(16.dp)
            ),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Threat Level Badge
            GemmaGuardCard(
                variant = GemmaGuardCardVariant.Default
            ) {
                Text(
                    text = "${alert.threatLevel.name} THREAT",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confidence Score
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confidence: ${(alert.confidence * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = alertColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time elapsed since detection
            val timeElapsed = Clock.System.now() - alert.timestamp
            Text(
                text = "Detected ${formatTimeElapsed(timeElapsed)} ago",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmergencyActionButtons(
    onAcknowledge: () -> Unit,
    onPlayVideo: () -> Unit,
    onStopAlarm: () -> Unit,
    isAlarmPlaying: Boolean,
    hasVideo: Boolean,
    isAcknowledged: Boolean,
    alertColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Emergency Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stop Alarm Button (if playing)
                if (isAlarmPlaying) {
                    Button(
                        onClick = onStopAlarm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = alertColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP ALARM", fontWeight = FontWeight.Bold)
                    }
                }

                // Acknowledge Button
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier.weight(1f),
                    enabled = !isAcknowledged,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAcknowledged) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            alertColor
                    )
                ) {
                    Icon(
                        imageVector = if (isAcknowledged) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAcknowledged) "ACKNOWLEDGED" else "ACKNOWLEDGE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Play Video Button (if available)
            if (hasVideo) {
                Button(
                    onClick = onPlayVideo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY SECURITY VIDEO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmergencyThreatAnalysisCard(
    alert: SecurityAlert,
    alertColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Threat Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = alert.summary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmergencyKeywordsCard(
    alert: SecurityAlert,
    alertColor: Color
) {
    if (alert.keywords.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detection Keywords",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keywords as chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alert.keywords.take(6).forEach { keyword ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = keyword,
                                    fontSize = 12.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = alertColor.copy(alpha = 0.1f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyVideoCard(
    videoClip: com.GemmaGuardian.securitymonitor.domain.model.VideoClip,
    onPlayVideo: () -> Unit,
    alertColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Recording",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Duration: ${videoClip.duration.inWholeSeconds}s",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = videoClip.fileName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onPlayVideo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = alertColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY")
                }
            }
        }
    }
}

@Composable
private fun EmergencyTimelineCard(
    alert: SecurityAlert,
    alertColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alert Timeline",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineItem(
                    title = "Threat Detected",
                    time = alert.timestamp,
                    icon = Icons.Default.Warning,
                    color = alertColor
                )
                
                if (alert.isAcknowledged) {
                    TimelineItem(
                        title = "Alert Acknowledged",
                        time = Clock.System.now(), // This should be actual acknowledgment time
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    time: Instant,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatInstant(time),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeElapsed(duration: kotlin.time.Duration): String {
    val totalSeconds = duration.inWholeSeconds
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds < 3600 -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
        else -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
    }
}

private fun formatInstant(instant: Instant): String {
    return try {
        // This is a simplified format - you might want to use actual date formatting
        val now = Clock.System.now()
        val diff = now - instant
        when {
            diff.inWholeMinutes < 1 -> "Just now"
            diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes}m ago"
            diff.inWholeHours < 24 -> "${diff.inWholeHours}h ago"
            else -> "${diff.inWholeDays}d ago"
        }
    } catch (e: Exception) {
        "Unknown time"
    }
}
