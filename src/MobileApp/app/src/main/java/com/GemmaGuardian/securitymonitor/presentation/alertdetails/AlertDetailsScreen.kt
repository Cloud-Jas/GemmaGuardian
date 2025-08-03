package com.GemmaGuardian.securitymonitor.presentation.alertdetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GemmaGuardian.securitymonitor.domain.model.SecurityAlert
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Composable
private fun MarkdownText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    val annotatedString = buildAnnotatedString {
        var processedText = text
        
        // Replace bullet points
        val bulletRegex = """^- (.*)$""".toRegex(RegexOption.MULTILINE)
        processedText = bulletRegex.replace(processedText) { matchResult ->
            "• ${matchResult.groupValues[1]}"
        }
        
        // Process bold formatting first (** before *)
        val boldRegex = """\*\*(.*?)\*\*""".toRegex()
        val boldMatches = boldRegex.findAll(processedText).toList()
        
        // Create a list to track processed ranges to avoid conflicts
        val processedRanges = mutableListOf<IntRange>()
        
        // Process italic formatting, but exclude ranges already used by bold
        val italicRegex = """\*([^*]+?)\*""".toRegex() // Changed to avoid matching ** patterns
        val italicMatches = italicRegex.findAll(processedText).toList().filter { italicMatch ->
            // Only include italic matches that don't overlap with bold matches
            boldMatches.none { boldMatch ->
                italicMatch.range.first >= boldMatch.range.first && italicMatch.range.last <= boldMatch.range.last
            }
        }
        
        if (boldMatches.isEmpty() && italicMatches.isEmpty()) {
            append(processedText)
        } else {
            var lastIndex = 0
            val allMatches = (boldMatches.map { it to "bold" } + italicMatches.map { it to "italic" })
                .sortedBy { it.first.range.first }
            
            allMatches.forEach { (match, type) ->
                if (match.range.first > lastIndex) {
                    append(processedText.substring(lastIndex, match.range.first))
                }
                
                when (type) {
                    "bold" -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(match.groupValues[1])
                        }
                    }
                    "italic" -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(match.groupValues[1])
                        }
                    }
                }
                
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < processedText.length) {
                append(processedText.substring(lastIndex))
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        color = color,
        modifier = modifier,
        lineHeight = lineHeight
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsScreen(
    alert: SecurityAlert,
    onBackClick: () -> Unit,
    onAcknowledge: () -> Unit,
    onPlayVideo: () -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    // GemmaGuard themed background
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom App Bar with GemmaGuard theme
            GemmaGuardAlertDetailsTopBar(
                alert = alert,
                onBackClick = onBackClick,
                onAcknowledge = onAcknowledge
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp + innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GemmaGuardThreatOverviewCard(alert = alert)
                }
                
                if (alert.videoClip != null) {
                    item {
                        GemmaGuardVideoClipCard(
                            alert = alert,
                            onPlayVideo = onPlayVideo
                        )
                    }
                }
                
                item {
                    GemmaGuardTechnicalDetailsCard(alert = alert)
                }
                
                item {
                    GemmaGuardKeywordsCard(alert = alert)
                }
                
                if (alert.description.isNotBlank() && alert.description != alert.summary) {
                    item {
                        GemmaGuardDetailedAnalysisCard(alert = alert)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GemmaGuardAlertDetailsTopBar(
    alert: SecurityAlert,
    onBackClick: () -> Unit,
    onAcknowledge: () -> Unit
) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = false,
        variant = GemmaGuardCardVariant.Glassmorphism
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(
                            GemmaGuardColors.Primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = GemmaGuardColors.Primary
                    )
                }
                
                Column {
                    Text(
                        text = "Alert Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GemmaGuardColors.TextPrimary
                    )
                    Text(
                        text = "ID: ${alert.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = GemmaGuardColors.TextSecondary
                    )
                }
            }
            
            if (!alert.isAcknowledged) {
                GemmaGuardButton(
                    text = "Acknowledge",
                    onClick = onAcknowledge,
                    icon = Icons.Default.Check,
                    variant = GemmaGuardButtonVariant.Primary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Acknowledged",
                        modifier = Modifier.size(16.dp),
                        tint = GemmaGuardColors.ThreatLow
                    )
                    Text(
                        text = "Acknowledged",
                        style = MaterialTheme.typography.labelMedium,
                        color = GemmaGuardColors.ThreatLow,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GemmaGuardThreatOverviewCard(alert: SecurityAlert) {
    GemmaGuardAlertCard(
        modifier = Modifier.fillMaxWidth(),
        threatColor = getThreatLevelColor(alert.threatLevel)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Threat Level Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                getThreatLevelColor(alert.threatLevel),
                                CircleShape
                            )
                    )
                    Text(
                        text = "${alert.threatLevel.name} THREAT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getThreatLevelColor(alert.threatLevel)
                    )
                }
                
                if (alert.isAcknowledged) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Acknowledged",
                            modifier = Modifier.size(18.dp),
                            tint = GemmaGuardColors.ThreatLow
                        )
                        Text(
                            text = "Resolved",
                            style = MaterialTheme.typography.labelMedium,
                            color = GemmaGuardColors.ThreatLow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Key Information Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GemmaGuardInfoChip(
                    icon = Icons.Default.Psychology,
                    label = "AI Confidence",
                    value = "${(alert.confidence * 100).toInt()}%",
                    modifier = Modifier.weight(1f)
                )
                GemmaGuardInfoChip(
                    icon = Icons.Default.Videocam,
                    label = "Camera",
                    value = alert.camera,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GemmaGuardInfoChip(
                    icon = Icons.Default.AccessTime,
                    label = "Detected",
                    value = formatTimestamp(alert.timestamp),
                    modifier = Modifier.weight(1f)
                )
                GemmaGuardInfoChip(
                    icon = Icons.Default.Timeline,
                    label = "Duration",
                    value = formatDuration(alert.timestamp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GemmaGuardAlertSummaryCard(alert: SecurityAlert) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = "Alert Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            }
            
            MarkdownText(
                text = alert.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = GemmaGuardColors.TextPrimary,
                lineHeight = 24.sp
            )
            
            if (alert.description.isNotBlank() && alert.description != alert.summary) {
                MarkdownText(
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemmaGuardColors.TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun GemmaGuardVideoClipCard(
    alert: SecurityAlert,
    onPlayVideo: () -> Unit
) {
    val videoClip = alert.videoClip ?: return
    
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = "Video Evidence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            }
            
            // Video Thumbnail/Preview
            GemmaGuardCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                variant = GemmaGuardCardVariant.Glassmorphism
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = GemmaGuardColors.Primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Video Clip Available",
                            style = MaterialTheme.typography.titleMedium,
                            color = GemmaGuardColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Duration: ${videoClip.duration}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = GemmaGuardColors.TextSecondary
                        )
                    }
                }
            }
            
            GemmaGuardButton(
                text = "Play Video",
                onClick = onPlayVideo,
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
                variant = GemmaGuardButtonVariant.Primary
            )
        }
    }
}

@Composable
private fun GemmaGuardTechnicalDetailsCard(alert: SecurityAlert) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = "Technical Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GemmaGuardDetailRow(
                    label = "Alert ID",
                    value = alert.id
                )
                GemmaGuardDetailRow(
                    label = "Timestamp",
                    value = formatFullTimestamp(alert.timestamp)
                )
                GemmaGuardDetailRow(
                    label = "Threat Detected",
                    value = if (alert.isThreatDetected) "Yes" else "No"
                )
                GemmaGuardDetailRow(
                    label = "Confidence",
                    value = "${(alert.confidence * 100).toInt()}%"
                )
                GemmaGuardDetailRow(
                    label = "Camera",
                    value = alert.camera
                )
                GemmaGuardDetailRow(
                    label = "Threat Level",
                    value = alert.threatLevel.name
                )
                if (alert.isAcknowledged) {
                    GemmaGuardDetailRow(
                        label = "Status",
                        value = "Acknowledged"
                    )
                }
            }
        }
    }
}

@Composable
private fun GemmaGuardKeywordsCard(alert: SecurityAlert) {
    if (alert.keywords.isEmpty()) return
    
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Glassmorphism
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = "Detection Keywords",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alert.keywords) { keyword ->
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        GemmaGuardColors.Primary.copy(alpha = 0.2f),
                                        GemmaGuardColors.Primary.copy(alpha = 0.1f)
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = GemmaGuardColors.Primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = keyword,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GemmaGuardDetailedAnalysisCard(alert: SecurityAlert) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = "Detailed Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            }
            
            MarkdownText(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = GemmaGuardColors.TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

// Helper Components
@Composable
private fun GemmaGuardInfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = GemmaGuardColors.Primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GemmaGuardColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = GemmaGuardColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GemmaGuardDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GemmaGuardColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = GemmaGuardColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// Utility Functions
private fun getThreatLevelColor(threatLevel: ThreatLevel): Color {
    return when (threatLevel) {
        ThreatLevel.LOW -> GemmaGuardColors.ThreatLow
        ThreatLevel.MEDIUM -> GemmaGuardColors.ThreatMedium
        ThreatLevel.HIGH -> GemmaGuardColors.ThreatHigh
        ThreatLevel.CRITICAL -> GemmaGuardColors.ThreatCritical
    }
}

private fun formatTimestamp(timestamp: Instant): String {
    val now = Clock.System.now()
    val duration = now - timestamp
    return when {
        duration.inWholeMinutes < 1 -> "Just now"
        duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes}m ago"
        duration.inWholeHours < 24 -> "${duration.inWholeHours}h ago"
        else -> "${duration.inWholeDays}d ago"
    }
}

private fun formatFullTimestamp(timestamp: Instant): String {
    return timestamp.toString().replace("T", " ").replace("Z", " UTC")
}

private fun formatDuration(timestamp: Instant): String {
    val now = Clock.System.now()
    val duration = now - timestamp
    return when {
        duration.inWholeHours > 0 -> "${duration.inWholeHours}h ${duration.inWholeMinutes % 60}m"
        duration.inWholeMinutes > 0 -> "${duration.inWholeMinutes}m"
        else -> "${duration.inWholeSeconds}s"
    }
}
