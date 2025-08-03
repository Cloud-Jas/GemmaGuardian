package com.GemmaGuardian.securitymonitor.presentation.emergency

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.delay

/**
 * Emergency splash screen that appears for CRITICAL and HIGH threat alerts
 * Shows for 3 seconds with alarm animation before navigating to alert details
 */
@Composable
fun EmergencyScreen(
    alert: SecurityAlert,
    onNavigateToDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-navigate after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onNavigateToDetails()
    }

    // Pulsing animation for emergency effect
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Color animation for alert background
    val alertColor = when (alert.threatLevel) {
        ThreatLevel.CRITICAL -> Color(0xFFDC2626) // Red
        ThreatLevel.HIGH -> Color(0xFFEA580C) // Orange-Red
        else -> Color(0xFFDC2626) // Default to red for emergency
    }

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            alertColor.copy(alpha = 0.9f),
            alertColor.copy(alpha = 0.7f),
            Color.Black.copy(alpha = 0.95f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large warning icon with pulse animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Alert",
                    modifier = Modifier.size(80.dp),
                    tint = alertColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Emergency title
            Text(
                text = when (alert.threatLevel) {
                    ThreatLevel.CRITICAL -> "CRITICAL SECURITY ALERT"
                    ThreatLevel.HIGH -> "HIGH PRIORITY ALERT"
                    else -> "SECURITY EMERGENCY"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Threat level badge
            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = "${alert.threatLevel.name} THREAT",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = alertColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brief summary
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = alert.summary.take(100) + if (alert.summary.length > 100) "..." else "",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading alert details...",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Tap anywhere to continue (optional)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            TextButton(
                onClick = onNavigateToDetails,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Tap to continue",
                    fontSize = 12.sp
                )
            }
        }
    }
}
