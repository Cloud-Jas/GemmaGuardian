package com.GemmaGuardian.securitymonitor.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PulsingDot(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Float = 12f,
    animationDuration: Int = 1000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .background(
                color = color.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerTranslateAnim - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerTranslateAnim, 200f)
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush)
        ) {
            content()
        }
    }
}

@Composable
fun CountUpAnimation(
    targetValue: Int,
    duration: Int = 1000,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var currentValue by remember { mutableStateOf(0) }
    
    LaunchedEffect(targetValue) {
        val startTime = System.currentTimeMillis()
        while (currentValue < targetValue) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
            currentValue = (targetValue * progress).toInt()
            
            if (progress >= 1f) break
            delay(16) // ~60fps
        }
        currentValue = targetValue
    }
    
    Text(
        text = currentValue.toString(),
        style = textStyle,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun SlideInContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    direction: SlideDirection = SlideDirection.FromBottom,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val slideIn = when (direction) {
        SlideDirection.FromLeft -> slideInHorizontally { -it }
        SlideDirection.FromRight -> slideInHorizontally { it }
        SlideDirection.FromTop -> slideInVertically { -it }
        SlideDirection.FromBottom -> slideInVertically { it }
    }
    
    val slideOut = when (direction) {
        SlideDirection.FromLeft -> slideOutHorizontally { -it }
        SlideDirection.FromRight -> slideOutHorizontally { it }
        SlideDirection.FromTop -> slideOutVertically { -it }
        SlideDirection.FromBottom -> slideOutVertically { it }
    }
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideIn + fadeIn(animationSpec = tween(300)),
        exit = slideOut + fadeOut(animationSpec = tween(300)),
        content = content
    )
}

enum class SlideDirection {
    FromLeft, FromRight, FromTop, FromBottom
}

@Composable
fun FloatingActionCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fab_rotation"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    content = expandedContent
                )
            }
        }
        
        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Toggle menu"
            )
        }
    }
}
