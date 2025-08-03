package com.GemmaGuardian.securitymonitor.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors

/**
 * GemmaGuard Modern Card Component with enhanced visual design
 */
@Composable
fun GemmaGuardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Boolean = true,
    variant: GemmaGuardCardVariant = GemmaGuardCardVariant.Default,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier
    
    val (backgroundColor, borderColor, shadowElevation) = when (variant) {
        GemmaGuardCardVariant.Default -> Triple(
            GemmaGuardColors.CardBackgroundElevated,
            GemmaGuardColors.BorderPrimary,
            if (elevation) 12.dp else 0.dp
        )
        GemmaGuardCardVariant.Elevated -> Triple(
            GemmaGuardColors.CardBackgroundElevated,
            GemmaGuardColors.Primary.copy(alpha = 0.4f),
            if (elevation) 16.dp else 0.dp
        )
        GemmaGuardCardVariant.Glassmorphism -> Triple(
            Color.Transparent,
            GemmaGuardColors.BorderPrimary,
            0.dp
        )
    }
    
    Card(
        modifier = cardModifier
            .then(
                if (variant == GemmaGuardCardVariant.Glassmorphism) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GemmaGuardColors.GlassMorphismLight,
                                    GemmaGuardColors.GlassMorphismPurple
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GemmaGuardColors.Primary.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = shadowElevation
        )
    ) {
        Column(
            modifier = if (variant == GemmaGuardCardVariant.Glassmorphism) {
                Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(
                            GemmaGuardColors.Primary.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
            } else Modifier,
            content = content
        )
    }
}

/**
 * Card variant types for different visual styles
 */
enum class GemmaGuardCardVariant {
    Default,        // Standard elevated card with border
    Elevated,       // Higher elevation with stronger border
    Glassmorphism   // Glass morphism effect with gradient background
}

/**
 * Enhanced Alert Card with threat-level aware styling
 */
@Composable
fun GemmaGuardAlertCard(
    modifier: Modifier = Modifier,
    threatColor: Color = GemmaGuardColors.Primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier
    
    Card(
        modifier = cardModifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        threatColor.copy(alpha = 0.1f),
                        Color.Transparent,
                        threatColor.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        threatColor.copy(alpha = 0.6f),
                        threatColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 16.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GemmaGuardColors.CardBackgroundElevated,
                            GemmaGuardColors.CardBackground
                        ),
                        radius = 600f
                    )
                )
        ) {
            Column(content = content)
        }
    }
}

/**
 * GemmaGuard Gradient Background Component
 */
@Composable
fun GemmaGuardGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = GemmaGuardColors.backgroundGradientColors
                )
            )
    ) {
        content()
    }
}

/**
 * Enhanced Status Indicator with animation
 */
@Composable
fun GemmaGuardStatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        Box(
            modifier = Modifier
                .size((size + 4).dp)
                .scale(if (isOnline) scale else 1f)
                .background(
                    color = if (isOnline) GemmaGuardColors.Online.copy(alpha = 0.3f)
                    else GemmaGuardColors.Offline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
        
        // Main indicator
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(
                    color = if (isOnline) GemmaGuardColors.Online else GemmaGuardColors.Offline,
                    shape = CircleShape
                )
        )
    }
}

/**
 * Enhanced Threat Level Badge
 */
@Composable
fun GemmaGuardThreatBadge(
    threatLevel: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (threatLevel.lowercase()) {
        "critical" -> GemmaGuardColors.ThreatCriticalDark to Color.White
        "high" -> GemmaGuardColors.ThreatHighDark to Color.White
        "medium" -> GemmaGuardColors.ThreatMediumDark to Color.Black
        "low" -> GemmaGuardColors.ThreatLowDark to Color.White
        else -> GemmaGuardColors.BorderSecondary to GemmaGuardColors.TextSecondary
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = threatLevel.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Modern Button Component with GemmaGuard styling
 */
@Composable
fun GemmaGuardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: GemmaGuardButtonVariant = GemmaGuardButtonVariant.Primary,
    enabled: Boolean = true
) {
    val containerColor = when (variant) {
        GemmaGuardButtonVariant.Primary -> GemmaGuardColors.ButtonPrimary
        GemmaGuardButtonVariant.Secondary -> GemmaGuardColors.ButtonSecondary
        GemmaGuardButtonVariant.Outline -> Color.Transparent
    }
    
    val contentColor = when (variant) {
        GemmaGuardButtonVariant.Primary -> Color.White
        GemmaGuardButtonVariant.Secondary -> Color.White
        GemmaGuardButtonVariant.Outline -> GemmaGuardColors.Primary
    }
    
    val buttonModifier = if (variant == GemmaGuardButtonVariant.Outline) {
        modifier.border(
            width = 1.dp,
            color = GemmaGuardColors.BorderPrimary,
            shape = RoundedCornerShape(12.dp)
        )
    } else modifier
    
    Button(
        onClick = onClick,
        modifier = buttonModifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = GemmaGuardColors.ButtonDisabled
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

enum class GemmaGuardButtonVariant {
    Primary, Secondary, Outline
}

/**
 * Enhanced Alert Card Component
 */
@Composable
fun GemmaGuardAlertCard(
    title: String,
    subtitle: String,
    timestamp: String,
    threatLevel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GemmaGuardColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GemmaGuardColors.TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                GemmaGuardThreatBadge(threatLevel = threatLevel)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = GemmaGuardColors.TextTertiary
                )
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GemmaGuardColors.TextTertiary
                )
            }
        }
    }
}

/**
 * Header Component with GemmaGuard branding
 */
@Composable
fun GemmaGuardHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier.fillMaxWidth(),
        elevation = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            GemmaGuardColors.Primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GemmaGuardColors.TextSecondary
                    )
                }
            }
            
            Row(content = actions)
        }
    }
}

/**
 * Stats Card Component
 */
@Composable
fun GemmaGuardStatsCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trend: GemmaGuardStatsTrend? = null
) {
    GemmaGuardCard(
        modifier = modifier
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
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = GemmaGuardColors.Primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = GemmaGuardColors.TextSecondary
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GemmaGuardColors.TextPrimary
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = GemmaGuardColors.TextTertiary
                )
                
                trend?.let {
                    val (trendIcon, trendColor) = when (it) {
                        GemmaGuardStatsTrend.Up -> Icons.Default.TrendingUp to GemmaGuardColors.ThreatLowDark
                        GemmaGuardStatsTrend.Down -> Icons.Default.TrendingDown to GemmaGuardColors.ThreatCriticalDark
                        GemmaGuardStatsTrend.Stable -> Icons.Default.TrendingFlat to GemmaGuardColors.TextTertiary
                    }
                    
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = trendColor
                    )
                }
            }
        }
    }
}

enum class GemmaGuardStatsTrend {
    Up, Down, Stable
}
