package com.GemmaGuardian.securitymonitor.presentation.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.GemmaGuardian.securitymonitor.MainActivity
import com.GemmaGuardian.securitymonitor.R
import com.GemmaGuardian.securitymonitor.presentation.intro.IntroActivity
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen for longer period
        splashScreen.setKeepOnScreenCondition { true }
        
        setContent {
            GemmaGuardTheme(darkTheme = true) { // Use consistent GemmaGuard dark theme
                SplashScreen {
                    navigateToNext()
                }
            }
        }
    }
    
    private fun navigateToNext() {
        // Check if it's first launch (you can use SharedPreferences)
        val isFirstLaunch = getSharedPreferences("security_monitor_prefs", MODE_PRIVATE)
            .getBoolean("is_first_launch", true)
            
        val intent = if (isFirstLaunch) {
            Intent(this, IntroActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animation values
    val alphaAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = EaseInOut),
        label = "alpha"
    )
    
    val scaleAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = EaseOutBack
        ),
        label = "scale"
    )
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(3000) // Show splash for 3 seconds
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E), // Dark blue
                        Color(0xFF16213E), // Darker blue
                        Color(0xFF0F3460)  // Navy blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnimation.value)
                .scale(scaleAnimation.value)
        ) {
            // Logo placeholder - you can replace with actual logo
            Card(
                modifier = Modifier
                    .size(120.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF533483)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 48.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name
            Text(
                text = "GemmaGuard",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "AI-Powered Security Intelligence",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Subtitle
            Text(
                text = "Transform Any Camera into\nIntelligent Surveillance",
                fontSize = 14.sp,
                color = Color(0xFF90A4AE),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFF533483),
                strokeWidth = 3.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Initializing AI Security System...",
                fontSize = 12.sp,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center
            )
        }
        
        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alphaAnimation.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered by Google Gemma AI",
                fontSize = 10.sp,
                color = Color(0xFF607D8B)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "© 2025 GemmaGuard Security",
                fontSize = 10.sp,
                color = Color(0xFF546E7A)
            )
        }
    }
}
