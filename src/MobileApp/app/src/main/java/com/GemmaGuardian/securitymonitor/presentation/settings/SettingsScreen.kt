package com.GemmaGuardian.securitymonitor.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.GemmaGuardian.securitymonitor.domain.model.ThreatLevel
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.GemmaGuardCard
import com.GemmaGuardian.securitymonitor.presentation.components.GemmaGuardCardVariant
import com.GemmaGuardian.securitymonitor.presentation.components.ThreatLevelBadge
import com.GemmaGuardian.securitymonitor.presentation.components.StatusIndicator
import com.GemmaGuardian.securitymonitor.data.notification.NotificationPreferences
import com.GemmaGuardian.securitymonitor.config.NetworkConfig

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

sealed class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val type: SettingsItemType
) {
    data class Toggle(
        val settingTitle: String,
        val settingSubtitle: String? = null,
        val settingIcon: ImageVector,
        val isEnabled: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingsItem(settingTitle, settingSubtitle, settingIcon, SettingsItemType.TOGGLE)
    
    data class Navigation(
        val settingTitle: String,
        val settingSubtitle: String? = null,
        val settingIcon: ImageVector,
        val onClick: () -> Unit
    ) : SettingsItem(settingTitle, settingSubtitle, settingIcon, SettingsItemType.NAVIGATION)
    
    data class Selection(
        val settingTitle: String,
        val settingSubtitle: String? = null,
        val settingIcon: ImageVector,
        val options: List<String>,
        val selectedOption: String,
        val onSelectionChange: (String) -> Unit
    ) : SettingsItem(settingTitle, settingSubtitle, settingIcon, SettingsItemType.SELECTION)
    
    data class Slider(
        val settingTitle: String,
        val settingSubtitle: String? = null,
        val settingIcon: ImageVector,
        val value: Float,
        val range: ClosedFloatingPointRange<Float>,
        val steps: Int = 0,
        val onValueChange: (Float) -> Unit
    ) : SettingsItem(settingTitle, settingSubtitle, settingIcon, SettingsItemType.SLIDER)
}

enum class SettingsItemType {
    TOGGLE, NAVIGATION, SELECTION, SLIDER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    notificationPreferences: NotificationPreferences? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Server configuration state
    var serverHost by remember { mutableStateOf("") }
    var serverPort by remember { mutableIntStateOf(8888) }
    var isEditingServer by remember { mutableStateOf(false) }
    var tempServerHost by remember { mutableStateOf("") }
    var tempServerPort by remember { mutableIntStateOf(8888) }
    var isValidIp by remember { mutableStateOf(true) }
    var isValidServerPort by remember { mutableStateOf(true) }
    
    // RTSP configuration state
    var rtspHost by remember { mutableStateOf("") }
    var rtspPort by remember { mutableIntStateOf(554) }
    var rtspUsername by remember { mutableStateOf("") }
    var rtspPassword by remember { mutableStateOf("") }
    var rtspPath by remember { mutableStateOf("") }
    var isEditingRtsp by remember { mutableStateOf(false) }
    var tempRtspHost by remember { mutableStateOf("") }
    var tempRtspPort by remember { mutableIntStateOf(554) }
    var tempRtspUsername by remember { mutableStateOf("") }
    var tempRtspPassword by remember { mutableStateOf("") }
    var tempRtspPath by remember { mutableStateOf("") }
    var isValidRtspIp by remember { mutableStateOf(true) }
    
    // Load current values from SharedPreferences when composable is created/recomposed
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("security_monitor_prefs", android.content.Context.MODE_PRIVATE)
        val currentServerHost = sharedPrefs.getString("server_ip", NetworkConfig.DEFAULT_SERVER_HOST) ?: NetworkConfig.DEFAULT_SERVER_HOST
        val currentServerPort = sharedPrefs.getInt("server_port", NetworkConfig.DEFAULT_SERVER_PORT)
        val currentRtspHost = sharedPrefs.getString("rtsp_host", NetworkConfig.DEFAULT_RTSP_HOST) ?: NetworkConfig.DEFAULT_RTSP_HOST
        val currentRtspPort = sharedPrefs.getInt("rtsp_port", NetworkConfig.DEFAULT_RTSP_PORT)
        val currentRtspUsername = sharedPrefs.getString("rtsp_username", NetworkConfig.DEFAULT_RTSP_USERNAME) ?: NetworkConfig.DEFAULT_RTSP_USERNAME
        val currentRtspPassword = sharedPrefs.getString("rtsp_password", NetworkConfig.DEFAULT_RTSP_PASSWORD) ?: NetworkConfig.DEFAULT_RTSP_PASSWORD
        val currentRtspPath = sharedPrefs.getString("rtsp_path", NetworkConfig.DEFAULT_RTSP_PATH) ?: NetworkConfig.DEFAULT_RTSP_PATH
        
        serverHost = currentServerHost
        serverPort = currentServerPort
        rtspHost = currentRtspHost
        rtspPort = currentRtspPort
        rtspUsername = currentRtspUsername
        rtspPassword = currentRtspPassword
        rtspPath = currentRtspPath
        
        // Also update temp values if not currently editing
        if (!isEditingServer) {
            tempServerHost = currentServerHost
            tempServerPort = currentServerPort
        }
        if (!isEditingRtsp) {
            tempRtspHost = currentRtspHost
            tempRtspPort = currentRtspPort
            tempRtspUsername = currentRtspUsername
            tempRtspPassword = currentRtspPassword
            tempRtspPath = currentRtspPath
        }
        
        android.util.Log.d("SettingsScreen", "📋 Loaded current values - Server: $currentServerHost, RTSP: $currentRtspHost:$currentRtspPort, User: $currentRtspUsername")
    }
    
    // Helper function to validate IP address
    fun isValidIpAddress(ip: String): Boolean {
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
    
    // Function to save server configuration
    fun saveServerConfiguration(newHost: String, newPort: Int = NetworkConfig.DEFAULT_SERVER_PORT) {
        context.getSharedPreferences("security_monitor_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("server_ip", newHost)
            .putInt("server_port", newPort)
            .apply()
        serverHost = newHost
        serverPort = newPort
        isEditingServer = false
        android.util.Log.d("SettingsScreen", "Server configuration saved: $newHost:$newPort")
    }
    
    // Function to save RTSP configuration
    fun saveRtspConfiguration(newHost: String, newPort: Int, newUsername: String, newPassword: String, newPath: String) {
        val editor = context.getSharedPreferences("security_monitor_prefs", android.content.Context.MODE_PRIVATE).edit()
        
        // Save all RTSP configuration values
        editor.putString("rtsp_host", newHost)
        editor.putInt("rtsp_port", newPort)
        editor.putString("rtsp_username", newUsername)
        editor.putString("rtsp_password", newPassword)
        editor.putString("rtsp_path", newPath)
        
        editor.apply()
        
        // Update local state
        rtspHost = newHost
        rtspPort = newPort
        rtspUsername = newUsername
        rtspPassword = newPassword
        rtspPath = newPath
        isEditingRtsp = false
        
        android.util.Log.d("SettingsScreen", "Complete RTSP configuration saved: $newHost:$newPort, User: $newUsername, Path: $newPath")
    }
    
    // Schedule data structure: Day -> List of time ranges
    var schedules by remember { 
        mutableStateOf(
            // Load saved schedules or use defaults
            if (notificationPreferences != null) {
                mapOf(
                    "Monday-Friday" to notificationPreferences.getWeekdaySchedule(),
                    "Saturday-Sunday" to notificationPreferences.getWeekendSchedule()
                )
            } else {
                mapOf(
                    "Monday-Friday" to listOf("12:00 AM - 7:00 AM"),
                    "Saturday-Sunday" to listOf("12:00 AM - 7:00 AM")
                )
            }
        )
    }
    
    // Load preferences on first composition
    LaunchedEffect(notificationPreferences) {
        if (notificationPreferences != null) {
            schedules = mapOf(
                "Monday-Friday" to notificationPreferences.getWeekdaySchedule(),
                "Saturday-Sunday" to notificationPreferences.getWeekendSchedule()
            )
        }
    }
    
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var selectedDayType by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(GemmaGuardColors.PrimaryDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ModernSettingsHeader(
                totalTimeSlots = schedules.values.sumOf { it.size }
            )
        }
        
        // Server Configuration Section
        item {
            ModernSettingsSection(
                title = "Server Configuration",
                icon = Icons.Default.Router
            ) {
                ImprovedServerConfigurationItem(
                    currentServerHost = serverHost,
                    currentServerPort = serverPort,
                    onSaveConfiguration = { newHost, newPort ->
                        if (isValidIpAddress(newHost) && newHost.isNotBlank() && newPort in 1..65535) {
                            saveServerConfiguration(newHost, newPort)
                        }
                    },
                    onResetToDefault = {
                        saveServerConfiguration(NetworkConfig.DEFAULT_SERVER_HOST, NetworkConfig.DEFAULT_SERVER_PORT)
                    }
                )
            }
        }
        
        // RTSP Camera Configuration Section
        item {
            ModernSettingsSection(
                title = "Camera Configuration",
                icon = Icons.Default.Videocam
            ) {
                ImprovedRtspConfigurationItem(
                    currentRtspHost = rtspHost,
                    currentRtspPort = rtspPort,
                    currentRtspUsername = rtspUsername,
                    currentRtspPassword = rtspPassword,
                    currentRtspPath = rtspPath,
                    onSaveConfiguration = { newHost, newPort, newUsername, newPassword, newPath ->
                        if (isValidIpAddress(newHost) && newHost.isNotBlank()) {
                            saveRtspConfiguration(newHost, newPort, newUsername, newPassword, newPath)
                        }
                    },
                    onResetToDefault = {
                        saveRtspConfiguration(
                            NetworkConfig.DEFAULT_RTSP_HOST,
                            NetworkConfig.DEFAULT_RTSP_PORT,
                            NetworkConfig.DEFAULT_RTSP_USERNAME,
                            NetworkConfig.DEFAULT_RTSP_PASSWORD,
                            NetworkConfig.DEFAULT_RTSP_PATH
                        )
                    }
                )
            }
        }
        
        item {
            ModernSettingsSection(
                title = "Alarm Schedule",
                icon = Icons.Default.Schedule
            ) {
                // Show schedule for Monday-Friday
                ScheduleItem(
                    title = "Monday - Friday",
                    timeSlots = schedules["Monday-Friday"] ?: emptyList(),
                    icon = Icons.Default.CalendarToday,
                    onAddTimeSlot = { 
                        selectedDayType = "Monday-Friday"
                        showAddTimeDialog = true 
                    },
                    onRemoveTimeSlot = { timeSlot ->
                        val newTimeSlots = (schedules["Monday-Friday"] ?: emptyList()) - timeSlot
                        schedules = schedules.toMutableMap().apply {
                            this["Monday-Friday"] = newTimeSlots
                        }
                        // Save to preferences
                        notificationPreferences?.setWeekdaySchedule(newTimeSlots)
                        android.util.Log.d("SettingsScreen", "Saved weekday schedule: $newTimeSlots")
                    }
                )
                
                // Show schedule for Saturday-Sunday
                ScheduleItem(
                    title = "Saturday - Sunday",
                    timeSlots = schedules["Saturday-Sunday"] ?: emptyList(),
                    icon = Icons.Default.Weekend,
                    onAddTimeSlot = { 
                        selectedDayType = "Saturday-Sunday"
                        showAddTimeDialog = true 
                    },
                    onRemoveTimeSlot = { timeSlot ->
                        val newTimeSlots = (schedules["Saturday-Sunday"] ?: emptyList()) - timeSlot
                        schedules = schedules.toMutableMap().apply {
                            this["Saturday-Sunday"] = newTimeSlots
                        }
                        // Save to preferences
                        notificationPreferences?.setWeekendSchedule(newTimeSlots)
                        android.util.Log.d("SettingsScreen", "Saved weekend schedule: $newTimeSlots")
                    }
                )
            }
        }
    }
    
    // Add time slot dialog
    if (showAddTimeDialog) {
        AddTimeSlotDialog(
            dayType = selectedDayType,
            onDismiss = { showAddTimeDialog = false },
            onAddTimeSlot = { timeSlot ->
                val newTimeSlots = (schedules[selectedDayType] ?: emptyList()) + timeSlot
                schedules = schedules.toMutableMap().apply {
                    this[selectedDayType] = newTimeSlots
                }
                // Save to preferences
                when (selectedDayType) {
                    "Monday-Friday" -> {
                        notificationPreferences?.setWeekdaySchedule(newTimeSlots)
                        android.util.Log.d("SettingsScreen", "Added weekday time slot: $timeSlot, new schedule: $newTimeSlots")
                    }
                    "Saturday-Sunday" -> {
                        notificationPreferences?.setWeekendSchedule(newTimeSlots)
                        android.util.Log.d("SettingsScreen", "Added weekend time slot: $timeSlot, new schedule: $newTimeSlots")
                    }
                }
                showAddTimeDialog = false
            }
        )
    }
}

@Composable
private fun ModernSettingsHeader(
    totalTimeSlots: Int = 0
) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure your GemmaGuard security system",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Server Config",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Camera Config",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$totalTimeSlots Time Slots",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Default
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            content()
        }
    }
}

@Composable
private fun ModernToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun ModernSelectionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: List<String>,
    selectedOption: String,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = selectedOption,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChange(option)
                                    expanded = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (option == selectedOption) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option == selectedOption) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSliderItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = valueFormatter(value),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun ModernNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ScheduleItem(
    title: String,
    timeSlots: List<String>,
    icon: ImageVector,
    onAddTimeSlot: () -> Unit,
    onRemoveTimeSlot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onAddTimeSlot,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add time slot",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (timeSlots.isEmpty()) {
                Text(
                    text = "No time slots configured. Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            } else {
                timeSlots.forEach { timeSlot ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = timeSlot,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = { onRemoveTimeSlot(timeSlot) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove time slot",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTimeSlotDialog(
    dayType: String,
    onDismiss: () -> Unit,
    onAddTimeSlot: (String) -> Unit
) {
    var startHour by remember { mutableStateOf(0) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(7) }
    var endMinute by remember { mutableStateOf(0) }
    
    fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, period)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Time Slot for $dayType")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start time picker
                Text(
                    text = "Start Time: ${formatTime(startHour, startMinute)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour:", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = startHour.toFloat(),
                        onValueChange = { startHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Min:", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = startMinute.toFloat(),
                        onValueChange = { startMinute = (it.toInt() / 15) * 15 },
                        valueRange = 0f..45f,
                        steps = 3,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // End time picker
                Text(
                    text = "End Time: ${formatTime(endHour, endMinute)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour:", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = endHour.toFloat(),
                        onValueChange = { endHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Min:", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = endMinute.toFloat(),
                        onValueChange = { endMinute = (it.toInt() / 15) * 15 },
                        valueRange = 0f..45f,
                        steps = 3,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val timeSlot = "${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}"
                    onAddTimeSlot(timeSlot)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ServerConfigurationItem(
    currentServerHost: String,
    isEditing: Boolean,
    tempServerHost: String,
    isValidIp: Boolean,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onServerHostChange: (String) -> Unit,
    onSaveConfiguration: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Backend Server IP",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEditing) "Enter the IP address of your GemmaGuard server" 
                              else "Current: $currentServerHost",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (!isEditing) {
                    IconButton(
                        onClick = onStartEditing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit server IP",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Edit Mode
            if (isEditing) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempServerHost,
                        onValueChange = onServerHostChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server IP Address") },
                        placeholder = { Text("192.168.0.102") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidIp,
                        supportingText = if (!isValidIp) {
                            {
                                Text(
                                    "Please enter a valid IP address (e.g., 192.168.0.102)",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )

                    // Info Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Make sure your GemmaGuard server is running on this IP address. You can find your server IP in the intro screen or by running ipconfig on Windows.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Quick IP suggestions
                    Text(
                        text = "Common IP ranges:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "192.168.1.102",
                            "192.168.0.102",
                            "10.0.0.102"
                        ).forEach { suggestedIp ->
                            AssistChip(
                                onClick = { onServerHostChange(suggestedIp) },
                                label = {
                                    Text(
                                        text = suggestedIp,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.6f
                                    ),
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelEditing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }

                            OutlinedButton(
                                onClick = onResetToDefault,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Default")
                            }

                            Button(
                                onClick = onSaveConfiguration,
                                enabled = isValidIp && tempServerHost.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }
                }
            } else {
                // Display Mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server configured at $currentServerHost",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RtspConfigurationItem(
    currentRtspHost: String,
    isEditing: Boolean,
    tempRtspHost: String,
    isValidIp: Boolean,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onRtspHostChange: (String) -> Unit,
    onSaveConfiguration: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "RTSP Camera IP",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEditing) "Enter the IP address of your RTSP camera" 
                              else "Current: $currentRtspHost:${NetworkConfig.DEFAULT_RTSP_PORT}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (!isEditing) {
                    IconButton(
                        onClick = onStartEditing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit RTSP IP",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Edit Mode
            if (isEditing) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempRtspHost,
                        onValueChange = onRtspHostChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("RTSP Camera IP Address") },
                        placeholder = { Text("192.168.0.100") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidIp,
                        supportingText = if (!isValidIp) {
                            { Text("Please enter a valid IP address (e.g., 192.168.0.100)", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    
                    // Info Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Make sure your RTSP camera is accessible at this IP address on port ${NetworkConfig.DEFAULT_RTSP_PORT}. Camera will be accessed at rtsp://admin:admin@$tempRtspHost:${NetworkConfig.DEFAULT_RTSP_PORT}/ch0_0.264",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    // Quick IP suggestions
                    Text(
                        text = "Common camera IP ranges:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("192.168.1.100", "192.168.0.100", "10.0.0.100").forEach { suggestedIp ->
                            AssistChip(
                                onClick = { onRtspHostChange(suggestedIp) },
                                label = { 
                                    Text(
                                        text = suggestedIp,
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelEditing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                        
                        OutlinedButton(
                            onClick = onResetToDefault,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                        
                        Button(
                            onClick = onSaveConfiguration,
                            enabled = isValidIp && tempRtspHost.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
                }
            } else {
                // Display Mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Camera configured at $currentRtspHost:${NetworkConfig.DEFAULT_RTSP_PORT}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImprovedServerConfigurationItem(
    currentServerHost: String,
    currentServerPort: Int,
    onSaveConfiguration: (String, Int) -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempServerHost by remember { mutableStateOf(currentServerHost) }
    var tempServerPort by remember { mutableIntStateOf(currentServerPort) }
    var isValidIp by remember { mutableStateOf(true) }
    var isValidPort by remember { mutableStateOf(true) }
    
    // Update temp values when current values change
    LaunchedEffect(currentServerHost, currentServerPort) {
        if (!isEditing) {
            tempServerHost = currentServerHost
            tempServerPort = currentServerPort
        }
    }
    
    // Helper function to validate IP address
    fun isValidIpAddress(ip: String): Boolean {
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
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Backend Server Configuration",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEditing) "Enter the IP address and port of your GemmaGuard server" 
                              else "Current: $currentServerHost:$currentServerPort",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (!isEditing) {
                    IconButton(
                        onClick = { 
                            isEditing = true
                            tempServerHost = currentServerHost
                            tempServerPort = currentServerPort
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit server IP",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Edit Mode
            if (isEditing) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempServerHost,
                        onValueChange = { newHost ->
                            tempServerHost = newHost
                            isValidIp = isValidIpAddress(newHost)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server IP Address") },
                        placeholder = { Text("192.168.0.102") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidIp,
                        supportingText = if (!isValidIp) {
                            { Text("Please enter a valid IP address (e.g., 192.168.0.102)", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    
                    OutlinedTextField(
                        value = tempServerPort.toString(),
                        onValueChange = { newPort ->
                            try {
                                val port = newPort.toIntOrNull()
                                if (port != null && port in 1..65535) {
                                    tempServerPort = port
                                    isValidPort = true
                                } else if (newPort.isEmpty()) {
                                    isValidPort = false
                                } else {
                                    isValidPort = false
                                }
                            } catch (e: Exception) {
                                isValidPort = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server Port") },
                        placeholder = { Text("8888") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidPort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidPort) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidPort,
                        supportingText = if (!isValidPort) {
                            { Text("Port must be between 1 and 65535", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (isValidPort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                isEditing = false
                                tempServerHost = currentServerHost
                                tempServerPort = currentServerPort
                                isValidIp = true
                                isValidPort = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                tempServerHost = NetworkConfig.DEFAULT_SERVER_HOST
                                tempServerPort = NetworkConfig.DEFAULT_SERVER_PORT
                                isValidIp = true
                                isValidPort = true
                                onResetToDefault()
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                        
                        Button(
                            onClick = {
                                if (isValidIp && isValidPort && tempServerHost.isNotBlank()) {
                                    onSaveConfiguration(tempServerHost, tempServerPort)
                                    isEditing = false
                                }
                            },
                            enabled = isValidIp && isValidPort && tempServerHost.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            } else {
                // Display Mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server configured at $currentServerHost",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImprovedRtspConfigurationItem(
    currentRtspHost: String,
    currentRtspPort: Int,
    currentRtspUsername: String,
    currentRtspPassword: String,
    currentRtspPath: String,
    onSaveConfiguration: (String, Int, String, String, String) -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempRtspHost by remember { mutableStateOf(currentRtspHost) }
    var tempRtspPort by remember { mutableStateOf(currentRtspPort.toString()) }
    var tempRtspUsername by remember { mutableStateOf(currentRtspUsername) }
    var tempRtspPassword by remember { mutableStateOf(currentRtspPassword) }
    var tempRtspPath by remember { mutableStateOf(currentRtspPath) }
    var isValidIp by remember { mutableStateOf(true) }
    var isValidPort by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    
    // Update temp values when current values change
    LaunchedEffect(currentRtspHost, currentRtspPort, currentRtspUsername, currentRtspPassword, currentRtspPath) {
        if (!isEditing) {
            tempRtspHost = currentRtspHost
            tempRtspPort = currentRtspPort.toString()
            tempRtspUsername = currentRtspUsername
            tempRtspPassword = currentRtspPassword
            tempRtspPath = currentRtspPath
        }
    }
    
    // Helper function to validate IP address
    fun isValidIpAddress(ip: String): Boolean {
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
    
    // Helper function to validate port
    fun isValidPortNumber(port: String): Boolean {
        return try {
            val portNum = port.toInt()
            portNum in 1..65535
        } catch (e: NumberFormatException) {
            false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "RTSP Camera Configuration",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEditing) "Configure your RTSP camera connection" 
                              else "rtsp://$currentRtspUsername:***@$currentRtspHost:$currentRtspPort$currentRtspPath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (!isEditing) {
                    IconButton(
                        onClick = { 
                            isEditing = true
                            tempRtspHost = currentRtspHost
                            tempRtspPort = currentRtspPort.toString()
                            tempRtspUsername = currentRtspUsername
                            tempRtspPassword = currentRtspPassword
                            tempRtspPath = currentRtspPath
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit RTSP Configuration",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Edit Mode
            if (isEditing) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // IP Address Field
                    OutlinedTextField(
                        value = tempRtspHost,
                        onValueChange = { newHost ->
                            tempRtspHost = newHost
                            isValidIp = isValidIpAddress(newHost)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Camera IP Address") },
                        placeholder = { Text("192.168.0.100") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidIp) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidIp,
                        supportingText = if (!isValidIp) {
                            { Text("Please enter a valid IP address", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isValidIp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    
                    // Port Field
                    OutlinedTextField(
                        value = tempRtspPort,
                        onValueChange = { newPort ->
                            tempRtspPort = newPort
                            isValidPort = isValidPortNumber(newPort)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Port") },
                        placeholder = { Text("554") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidPort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = if (isValidPort) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                        ),
                        isError = !isValidPort,
                        supportingText = if (!isValidPort) {
                            { Text("Port must be between 1-65535", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (isValidPort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    
                    // Username Field
                    OutlinedTextField(
                        value = tempRtspUsername,
                        onValueChange = { tempRtspUsername = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        placeholder = { Text("admin") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    
                    // Password Field
                    OutlinedTextField(
                        value = tempRtspPassword,
                        onValueChange = { tempRtspPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    
                    // Path Field
                    OutlinedTextField(
                        value = tempRtspPath,
                        onValueChange = { tempRtspPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Stream Path") },
                        placeholder = { Text("/stream/channel1") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    
                    // Preview URL
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Preview URL:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "rtsp://$tempRtspUsername:${if (tempRtspPassword.isNotEmpty()) "***" else ""}@$tempRtspHost:$tempRtspPort$tempRtspPath",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                isEditing = false
                                tempRtspHost = currentRtspHost
                                tempRtspPort = currentRtspPort.toString()
                                tempRtspUsername = currentRtspUsername
                                tempRtspPassword = currentRtspPassword
                                tempRtspPath = currentRtspPath
                                isValidIp = true
                                isValidPort = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                tempRtspHost = NetworkConfig.DEFAULT_RTSP_HOST
                                tempRtspPort = NetworkConfig.DEFAULT_RTSP_PORT.toString()
                                tempRtspUsername = NetworkConfig.DEFAULT_RTSP_USERNAME
                                tempRtspPassword = NetworkConfig.DEFAULT_RTSP_PASSWORD
                                tempRtspPath = NetworkConfig.DEFAULT_RTSP_PATH
                                isValidIp = true
                                isValidPort = true
                                onResetToDefault()
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                        
                        Button(
                            onClick = {
                                if (isValidIp && isValidPort && tempRtspHost.isNotBlank()) {
                                    val portNum = tempRtspPort.toIntOrNull() ?: NetworkConfig.DEFAULT_RTSP_PORT
                                    onSaveConfiguration(tempRtspHost, portNum, tempRtspUsername, tempRtspPassword, tempRtspPath)
                                    isEditing = false
                                }
                            },
                            enabled = isValidIp && isValidPort && tempRtspHost.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            } else {
                // Display Mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RTSP Camera Configured",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Host: $currentRtspHost:$currentRtspPort",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "User: $currentRtspUsername",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Path: $currentRtspPath",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
