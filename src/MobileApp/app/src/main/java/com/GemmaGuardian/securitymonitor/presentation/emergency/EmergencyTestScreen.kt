package com.GemmaGuardian.securitymonitor.presentation.emergency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import kotlinx.datetime.Clock

/**
 * Emergency Test Screen for testing alarm functionality
 * This screen allows developers and users to test the emergency alarm system
 */
@Composable
fun EmergencyTestScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emergencyViewModel: EmergencyViewModel = hiltViewModel()
    
    var isAlarmPlaying by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    // Check alarm status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAlarmPlaying = emergencyViewModel.isEmergencyAlarmPlaying()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Emergency Alarm Test",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Test the emergency alert system for critical security threats",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Status Card
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
                        imageVector = if (isAlarmPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (isAlarmPlaying) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alarm Status: ${if (isAlarmPlaying) "PLAYING" else "STOPPED"}",
                        fontWeight = FontWeight.Medium,
                        color = if (isAlarmPlaying) Color.Red else Color.Gray
                    )
                }
                
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last Test: $testResult",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Test Buttons
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Test Emergency Alarms",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Test Critical Alarm
                Button(
                    onClick = {
                        testResult = "Testing CRITICAL alarm..."
                        emergencyViewModel.triggerEmergencyAlarm(ThreatLevel.CRITICAL)
                        testResult = "CRITICAL alarm test completed - ${Clock.System.now()}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    enabled = !isAlarmPlaying
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST CRITICAL ALARM", fontWeight = FontWeight.Bold)
                }
                
                // Test High Alarm
                Button(
                    onClick = {
                        testResult = "Testing HIGH alarm..."
                        emergencyViewModel.triggerEmergencyAlarm(ThreatLevel.HIGH)
                        testResult = "HIGH alarm test completed - ${Clock.System.now()}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    ),
                    enabled = !isAlarmPlaying
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationImportant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST HIGH ALARM", fontWeight = FontWeight.Bold)
                }
                
                // Quick Test (3 seconds)
                Button(
                    onClick = {
                        testResult = "Running quick test..."
                        emergencyViewModel.testAlarm()
                        testResult = "Quick test completed - ${Clock.System.now()}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = !isAlarmPlaying
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUICK TEST (3 seconds)")
                }
            }
        }
        
        // Control Buttons
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Alarm Control",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stop Alarm
                    Button(
                        onClick = {
                            emergencyViewModel.stopEmergencyAlarm()
                            testResult = "Alarm stopped manually - ${Clock.System.now()}"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = isAlarmPlaying
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP")
                    }
                    
                    // Back Button
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BACK")
                    }
                }
            }
        }
        
        // Test Notification System
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Test Emergency Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Test Emergency Notification
                Button(
                    onClick = {
                        // For now, just test the alarm system
                        // Full notification testing would require access to NotificationHandler
                        testResult = "Use the surveillance system to test emergency notifications - ${Clock.System.now()}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST ALARM SYSTEM ONLY")
                }
            }
        }
        
        // Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Testing Information",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Alarms will automatically stop after 10 seconds\n" +
                            "• Test notifications will trigger the full emergency flow\n" +
                            "• Make sure your device volume is turned up\n" +
                            "• Emergency alarms use maximum volume and vibration\n" +
                            "• Test in a safe environment to avoid disturbing others",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
