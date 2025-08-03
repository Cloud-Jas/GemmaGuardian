package com.GemmaGuardian.securitymonitor.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: Brush? = null,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = GemmaGuardColors.CardBackground
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        isPressed = true
                        onClick()
                    }
                } else Modifier
            ),
        elevation = elevation,
        colors = colors,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gradient != null) {
                        Modifier.background(gradient)
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.05f)
        )
    )
    
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        gradient = glassBrush,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        content = content
    )
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    trailing: (@Composable () -> Unit)? = null
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        colors = colors
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        GemmaGuardColors.Primary.copy(alpha = 0.3f)
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                icon()
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GemmaGuardColors.TextPrimary
                )
                
                subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GemmaGuardColors.TextSecondary
                    )
                }
            }
            
            trailing?.invoke()
        }
    }
}
