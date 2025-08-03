package com.GemmaGuardian.securitymonitor.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors

@Composable
fun ThreatLevelBadge(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    animated: Boolean = true
) {
    val (color, darkColor, icon, text) = when (threatLevel) {
        ThreatLevel.LOW -> listOf(
            GemmaGuardColors.ThreatLow,
            GemmaGuardColors.ThreatLowDark,
            Icons.Default.CheckCircle,
            "Low"
        )
        ThreatLevel.MEDIUM -> listOf(
            GemmaGuardColors.ThreatMedium,
            GemmaGuardColors.ThreatMediumDark,
            Icons.Default.Warning,
            "Medium"
        )
        ThreatLevel.HIGH -> listOf(
            GemmaGuardColors.ThreatHigh,
            GemmaGuardColors.ThreatHighDark,
            Icons.Default.Error,
            "High"
        )
        ThreatLevel.CRITICAL -> listOf(
            GemmaGuardColors.ThreatCritical,
            GemmaGuardColors.ThreatCriticalDark,
            Icons.Default.ReportProblem,
            "Critical"
        )
    }
    
    val animatedColor by animateColorAsState(
        targetValue = if (animated) color as Color else color as Color,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "threat_color"
    )
    
    val pulseScale by animateFloatAsState(
        targetValue = if (animated && threatLevel == ThreatLevel.CRITICAL) 1.1f else 1f,
        animationSpec = tween(1000),
        label = "pulse_scale"
    )
    
    Row(
        modifier = modifier
            .scale(pulseScale)
            .background(
                color = animatedColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = animatedColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon as ImageVector,
            contentDescription = text as String,
            modifier = Modifier.size(16.dp),
            tint = animatedColor
        )
        
        if (showText) {
            Text(
                text = text as String,
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    size: StatusIndicatorSize = StatusIndicatorSize.Medium
) {
    val (dotSize, textStyle) = when (size) {
        StatusIndicatorSize.Small -> 8.dp to MaterialTheme.typography.labelSmall
        StatusIndicatorSize.Medium -> 12.dp to MaterialTheme.typography.labelMedium
        StatusIndicatorSize.Large -> 16.dp to MaterialTheme.typography.labelLarge
    }
    
    val color = if (isOnline) GemmaGuardColors.Online else MaterialTheme.colorScheme.error
    val text = if (isOnline) "Online" else "Offline"
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
        
        if (showText) {
            Text(
                text = text,
                style = textStyle,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class StatusIndicatorSize {
    Small, Medium, Large
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    gradient: Brush? = null,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        gradient = gradient
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProgressCard(
    title: String,
    progress: Float,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    ModernCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
