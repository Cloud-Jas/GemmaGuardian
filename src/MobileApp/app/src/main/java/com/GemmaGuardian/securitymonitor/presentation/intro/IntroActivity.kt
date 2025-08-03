package com.GemmaGuardian.securitymonitor.presentation.intro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GemmaGuardian.securitymonitor.MainActivity
import com.GemmaGuardian.securitymonitor.config.NetworkConfig
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardTheme
import kotlinx.coroutines.delay

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GemmaGuardTheme(darkTheme = true) {
                IntroScreen { serverHost, serverPort, rtspHost ->
                    saveServerConfiguration(serverHost, serverPort)
                    saveRtspConfiguration(rtspHost)
                    navigateToMain()
                }
            }
        }
    }
    
    private fun saveServerConfiguration(serverHost: String, serverPort: Int = NetworkConfig.DEFAULT_SERVER_PORT) {
        getSharedPreferences("security_monitor_prefs", MODE_PRIVATE)
            .edit()
            .putString("server_ip", serverHost)
            .putInt("server_port", serverPort)
            .putBoolean("is_first_launch", false)
            .apply()
    }
    
    private fun saveRtspConfiguration(rtspHost: String, rtspPort: Int = NetworkConfig.DEFAULT_RTSP_PORT) {
        getSharedPreferences("security_monitor_prefs", MODE_PRIVATE)
            .edit()
            .putString("rtsp_host", rtspHost)
            .putInt("rtsp_port", rtspPort)
            .putString("rtsp_username", NetworkConfig.DEFAULT_RTSP_USERNAME)
            .putString("rtsp_password", NetworkConfig.DEFAULT_RTSP_PASSWORD)
            .putString("rtsp_path", NetworkConfig.DEFAULT_RTSP_PATH)
            .apply()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun IntroScreen(onContinue: (String, Int, String) -> Unit) {
    var currentStep by remember { mutableStateOf(0) }
    var serverHost by remember { mutableStateOf(NetworkConfig.DEFAULT_SERVER_HOST) }
    var serverPort by remember { mutableStateOf(NetworkConfig.DEFAULT_SERVER_PORT) }
    var rtspHost by remember { mutableStateOf(NetworkConfig.DEFAULT_RTSP_HOST) }
    var animateContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = true) {
        delay(300)
        animateContent = true
    }
    
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animateContent) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnimation.value)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            when (currentStep) {
                0 -> WelcomeStep { currentStep = 1 }
                1 -> FeaturesStep { currentStep = 2 }
                2 -> ServerConfigStep(
                    serverHost = serverHost,
                    serverPort = serverPort,
                    onServerHostChange = { serverHost = it },
                    onServerPortChange = { serverPort = it },
                    onContinue = { currentStep = 3 }
                )
                3 -> RtspConfigStep(
                    rtspHost = rtspHost,
                    onRtspHostChange = { rtspHost = it },
                    onContinue = { onContinue(serverHost, serverPort, rtspHost) }
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo and Title
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
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
        
        Text(
            text = "Welcome to",
            fontSize = 18.sp,
            color = Color(0xFFB0BEC5)
        )
        
        Text(
            text = "GemmaGuard",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Transform Any Camera into\nIntelligent AI-Powered Surveillance",
            fontSize = 16.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Key Benefits
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E3F).copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                BenefitItem(
                    icon = "🚫",
                    title = "Eliminate False Alarms",
                    description = "90%+ reduction in false positives"
                )
                
                BenefitItem(
                    icon = "🧠",
                    title = "AI-Powered Intelligence",
                    description = "Contextual threat assessment with Gemma AI"
                )
                
                BenefitItem(
                    icon = "📱",
                    title = "Real-Time Alerts",
                    description = "Instant mobile notifications with threat details"
                )
                
                BenefitItem(
                    icon = "💰",
                    title = "Zero Cost Solution",
                    description = "Enterprise-grade security without monthly fees"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF533483)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun FeaturesStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Powerful Features",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Experience the next generation of intelligent surveillance",
            fontSize = 16.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Feature Cards
        FeatureCard(
            icon = Icons.Default.Visibility,
            title = "Smart Detection",
            description = "Advanced person detection with behavior analysis",
            color = Color(0xFF2E7D32)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureCard(
            icon = Icons.Default.Security,
            title = "Threat Assessment",
            description = "Real-time security analysis and risk evaluation",
            color = Color(0xFFD32F2F)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureCard(
            icon = Icons.Default.CloudDone,
            title = "Dual Processing",
            description = "Choose between cloud or edge AI processing",
            color = Color(0xFF1976D2)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureCard(
            icon = Icons.Default.VideoLibrary,
            title = "Video Management",
            description = "Automatic recording and intelligent archiving",
            color = Color(0xFF7B1FA2)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF533483)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue Setup",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun ServerConfigStep(
    serverHost: String,
    serverPort: Int,
    onServerHostChange: (String) -> Unit,
    onServerPortChange: (Int) -> Unit,
    onContinue: () -> Unit
) {
    var isValidIp by remember { mutableStateOf(true) }
    var isValidPort by remember { mutableStateOf(true) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Router,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF533483)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Server Configuration",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter your GemmaGuard server IP address and port",
            fontSize = 16.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Server Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E3F).copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Backend Server IP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = serverHost,
                    onValueChange = { 
                        onServerHostChange(it)
                        isValidIp = isValidIpAddress(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            text = "192.168.0.102",
                            color = Color(0xFF78909C)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValidIp) Color(0xFF533483) else Color.Red,
                        unfocusedBorderColor = Color(0xFF455A64),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    isError = !isValidIp,
                    supportingText = if (!isValidIp) {
                        { Text("Please enter a valid IP address", color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Server Port",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = serverPort.toString(),
                    onValueChange = { 
                        try {
                            val port = it.toIntOrNull()
                            if (port != null && port in 1..65535) {
                                onServerPortChange(port)
                                isValidPort = true
                            } else {
                                isValidPort = false
                            }
                        } catch (e: Exception) {
                            isValidPort = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            text = "8888",
                            color = Color(0xFF78909C)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValidPort) Color(0xFF533483) else Color.Red,
                        unfocusedBorderColor = Color(0xFF455A64),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    isError = !isValidPort,
                    supportingText = if (!isValidPort) {
                        { Text("Port must be between 1 and 65535", color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D47A1).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Make sure your GemmaGuard server is running on this IP address and port",
                            fontSize = 12.sp,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick IP Discovery
                Text(
                    text = "Need help finding your server IP?",
                    fontSize = 12.sp,
                    color = Color(0xFF78909C)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("192.168.1.", "192.168.0.", "10.0.0.").forEach { prefix ->
                        AssistChip(
                            onClick = { 
                                if (prefix.endsWith(".")) {
                                    onServerHostChange("${prefix}102")
                                }
                            },
                            label = { 
                                Text(
                                    text = "${prefix}x",
                                    fontSize = 10.sp
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF37474F)
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onContinue,
            enabled = isValidIp && isValidPort && serverHost.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF533483),
                disabledContainerColor = Color(0xFF37474F)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Start GemmaGuard",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { 
                onServerHostChange(NetworkConfig.DEFAULT_SERVER_HOST)
                onServerPortChange(NetworkConfig.DEFAULT_SERVER_PORT)
            }
        ) {
            Text(
                text = "Use Default (${NetworkConfig.DEFAULT_SERVER_HOST}:${NetworkConfig.DEFAULT_SERVER_PORT})",
                color = Color(0xFF78909C)
            )
        }
    }
}

@Composable
fun BenefitItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.size(40.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF90A4AE)
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E3F).copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }
    }
}

private fun isValidIpAddress(ip: String): Boolean {
    if (ip.isBlank()) return false
    
    val parts = ip.split(".")
    if (parts.size != 4) return false
    
    return parts.all { part ->
        try {
            val num = part.toInt()
            num in 0..255
        } catch (e: NumberFormatException) {
            false
        }
    }
}

@Composable
fun RtspConfigStep(
    rtspHost: String,
    onRtspHostChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    var isValidIp by remember { mutableStateOf(true) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Videocam,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF533483)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Camera Configuration",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter your RTSP camera IP address",
            fontSize = 16.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // RTSP Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E3F).copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "RTSP Camera IP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = rtspHost,
                    onValueChange = { 
                        onRtspHostChange(it)
                        isValidIp = isValidIpAddress(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            text = "192.168.0.100",
                            color = Color(0xFF78909C)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValidIp) Color(0xFF533483) else Color.Red,
                        unfocusedBorderColor = Color(0xFF455A64),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    isError = !isValidIp,
                    supportingText = if (!isValidIp) {
                        { Text("Please enter a valid IP address", color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D47A1).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Make sure your RTSP camera is accessible at this IP address on port 554",
                            fontSize = 12.sp,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick IP Discovery
                Text(
                    text = "Need help finding your camera IP?",
                    fontSize = 12.sp,
                    color = Color(0xFF78909C)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("192.168.1.", "192.168.0.", "10.0.0.").forEach { prefix ->
                        AssistChip(
                            onClick = { 
                                if (prefix.endsWith(".")) {
                                    onRtspHostChange("${prefix}100")
                                }
                            },
                            label = { 
                                Text(
                                    text = "${prefix}x",
                                    fontSize = 10.sp
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF533483).copy(alpha = 0.3f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Use Default Button
                OutlinedButton(
                    onClick = { onRtspHostChange(NetworkConfig.DEFAULT_RTSP_HOST) }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use Default (${NetworkConfig.DEFAULT_RTSP_HOST})",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onContinue,
            enabled = isValidIp && rtspHost.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF533483)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Complete Setup",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.CheckCircle, contentDescription = null)
        }
    }
}
