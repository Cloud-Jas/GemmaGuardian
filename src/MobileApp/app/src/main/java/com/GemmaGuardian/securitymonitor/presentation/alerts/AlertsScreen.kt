package com.GemmaGuardian.securitymonitor.presentation.alerts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.GemmaGuardian.securitymonitor.domain.model.*
import com.GemmaGuardian.securitymonitor.presentation.alerts.AlertSortBy
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun MarkdownText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
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
        
        // Process italic formatting, but exclude ranges already used by bold
        val italicRegex = """\*([^*]+?)\*""".toRegex() // Changed to avoid matching ** patterns
        val italicMatches = italicRegex.findAll(processedText).toList().filter { italicMatch ->
            // Only include italic matches that don't overlap with bold matches
            boldMatches.none { boldMatch ->
                italicMatch.range.first >= boldMatch.range.first && italicMatch.range.last <= boldMatch.range.last
            }
        }
        
        if (boldMatches.isEmpty() && italicMatches.isEmpty()) {
            // No markdown formatting, just append the text
            append(processedText)
        } else {
            // Process formatting
            var lastIndex = 0
            
            // Sort all matches by start position
            val allMatches = (boldMatches.map { it to "bold" } + italicMatches.map { it to "italic" })
                .sortedBy { it.first.range.first }
            
            allMatches.forEach { (match, type) ->
                // Add text before the match
                if (match.range.first > lastIndex) {
                    append(processedText.substring(lastIndex, match.range.first))
                }
                
                // Add formatted text
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
            
            // Add remaining text
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
        maxLines = maxLines,
        overflow = overflow
    )
}

// --- MISSING COMPOSABLES & UTILS ---
@Composable
fun ModernLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ModernErrorState(
    error: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error: $error", color = Color.Red)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onNavigateBack) { Text("Back") }
    }
}

fun getThreatLevelColor(level: ThreatLevel): Color = when (level) {
    ThreatLevel.CRITICAL -> GemmaGuardColors.ThreatCritical
    ThreatLevel.HIGH -> GemmaGuardColors.ThreatHigh  
    ThreatLevel.MEDIUM -> GemmaGuardColors.ThreatMedium
    ThreatLevel.LOW -> GemmaGuardColors.ThreatLow
}

fun formatTimeAgo(timestamp: Instant): String {
    // Simple fallback: show date and time
    val local = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.monthNumber}/${local.dayOfMonth} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onNavigateToAlertDetails: (SecurityAlert) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = hiltViewModel(),
    initialAlertId: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    
    // Handle initial alert ID for notification navigation
    LaunchedEffect(initialAlertId) {
        if (initialAlertId != null) {
            android.util.Log.d("AlertsScreen", "📱 Loading alert details for ID: $initialAlertId")
            viewModel.getAlertDetails(initialAlertId)
        }
    }
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
    
    // Debug logging to understand data flow
    LaunchedEffect(uiState) {
        android.util.Log.d("AlertsScreen", """
            🔍 AlertsScreen UI State Debug:
            - isLoading: ${uiState.isLoading}
            - isRefreshing: ${uiState.isRefreshing}
            - Total alerts: ${uiState.alerts.size}
            - Filtered alerts: ${uiState.filteredAlerts.size}
            - Selected filter: ${uiState.selectedFilter}
            - Sort by: ${uiState.sortBy}
            - Error: ${uiState.error}
            - First few alerts: ${uiState.alerts.take(3).map { "${it.id}: ${it.summary}" }}
        """.trimIndent())
    }
    
    // Auto-refresh on screen load to ensure data is fresh
    LaunchedEffect(Unit) {
        android.util.Log.d("AlertsScreen", "🔄 Auto-refreshing alerts on screen load")
        viewModel.refresh()
    }

    // GemmaGuard dark theme background with gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = GemmaGuardColors.backgroundGradientColors
                )
            )
    ) {
        when {
            uiState.isLoading && !uiState.isRefreshing -> {
                ModernLoadingState(modifier = Modifier.padding(innerPadding))
            }
            
            uiState.error != null && !uiState.isRefreshing -> {
                ModernErrorState(
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() },
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            
            else -> {
                ModernAlertsContent(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onToggleFilters = { showFilters = !showFilters },
                    showFilters = showFilters,
                    onFilterChange = { viewModel.setThreatLevelFilter(it) },
                    onSortChange = { viewModel.setSortBy(it) },
                    onRefresh = { viewModel.refresh() },
                    onAlertClick = onNavigateToAlertDetails,
                    onAcknowledgeAlert = { alertId -> viewModel.acknowledgeAlert(alertId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ModernAlertsContent(
    uiState: AlertsUiState,
    onNavigateBack: () -> Unit,
    onToggleFilters: () -> Unit,
    showFilters: Boolean,
    onFilterChange: (ThreatLevel?) -> Unit,
    onSortChange: (AlertSortBy) -> Unit,
    onRefresh: () -> Unit,
    onAlertClick: (SecurityAlert) -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Header
        item {
            ModernAlertsHeader(
                onNavigateBack = onNavigateBack,
                onToggleFilters = onToggleFilters,
                alertCount = uiState.filteredAlerts.size,
                onRefresh = onRefresh,
                isRefreshing = uiState.isRefreshing
            )
        }
        
        // Alert Statistics
        item {
            AlertStatisticsSection(
                alerts = uiState.alerts,
                selectedFilter = uiState.selectedFilter,
                onFilterChange = onFilterChange
            )
        }
        
        // Animated Filters
        if (showFilters) {
            item {
                ModernFiltersSection(
                    selectedFilter = uiState.selectedFilter,
                    selectedSort = uiState.sortBy,
                    onFilterChange = onFilterChange,
                    onSortChange = onSortChange
                )
            }
        }
        
        // Alerts List
        if (uiState.filteredAlerts.isEmpty() && !uiState.isLoading) {
            item {
                ModernEmptyState(selectedFilter = uiState.selectedFilter)
            }
        } else {
            items(
                items = uiState.filteredAlerts,
                key = { it.id }
            ) { alert ->
                ModernAlertCard(
                    alert = alert,
                    onClick = { onAlertClick(alert) },
                    onAcknowledge = { onAcknowledgeAlert(alert.id) }
                )
            }
        }
    }
}

@Composable
private fun ModernAlertsHeader(
    onNavigateBack: () -> Unit,
    onToggleFilters: () -> Unit,
    alertCount: Int,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            android.util.Log.d("AlertsScreen", "🔙 Back button clicked, navigating back")
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .background(
                                GemmaGuardColors.Primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GemmaGuardColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Security Alerts",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GemmaGuardColors.TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$alertCount ${if (alertCount == 1) "alert" else "alerts"} found",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = GemmaGuardColors.TextSecondary
                            )
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFilters,
                        modifier = Modifier
                            .background(
                                GemmaGuardColors.Primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = GemmaGuardColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .background(
                                GemmaGuardColors.Primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = GemmaGuardColors.Primary,
                            modifier = if (isRefreshing) {
                                Modifier
                                    .size(24.dp)
                            } else {
                                Modifier.size(24.dp)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertStatisticsSection(
    alerts: List<SecurityAlert>,
    selectedFilter: ThreatLevel?,
    onFilterChange: (ThreatLevel?) -> Unit
) {
    val criticalCount = alerts.count { it.threatLevel == ThreatLevel.CRITICAL }
    val highCount = alerts.count { it.threatLevel == ThreatLevel.HIGH }
    val mediumCount = alerts.count { it.threatLevel == ThreatLevel.MEDIUM }
    val lowCount = alerts.count { it.threatLevel == ThreatLevel.LOW }
    val unacknowledgedCount = alerts.count { !it.isAcknowledged }
    
    Column {
        Text(
            text = "Alert Overview",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = GemmaGuardColors.TextPrimary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                AlertStatCard(
                    modifier = Modifier.width(140.dp),
                    title = "Critical",
                    count = criticalCount,
                    icon = Icons.Default.ReportProblem,
                    color = Color(0xFFE53E3E),
                    isSelected = selectedFilter == ThreatLevel.CRITICAL,
                    onClick = { onFilterChange(ThreatLevel.CRITICAL) }
                )
            }
            
            item {
                AlertStatCard(
                    modifier = Modifier.width(140.dp),
                    title = "High",
                    count = highCount,
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800),
                    isSelected = selectedFilter == ThreatLevel.HIGH,
                    onClick = { onFilterChange(ThreatLevel.HIGH) }
                )
            }
            
            item {
                AlertStatCard(
                    modifier = Modifier.width(140.dp),
                    title = "Medium",
                    count = mediumCount,
                    icon = Icons.Default.Info,
                    color = Color(0xFFFFC107),
                    isSelected = selectedFilter == ThreatLevel.MEDIUM,
                    onClick = { onFilterChange(ThreatLevel.MEDIUM) }
                )
            }
            
            item {
                AlertStatCard(
                    modifier = Modifier.width(140.dp),
                    title = "Low",
                    count = lowCount,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    isSelected = selectedFilter == ThreatLevel.LOW,
                    onClick = { onFilterChange(ThreatLevel.LOW) }
                )
            }
            
            item {
                AlertStatCard(
                    modifier = Modifier.width(140.dp),
                    title = "Unread",
                    count = unacknowledgedCount,
                    icon = Icons.Default.MarkAsUnread,
                    color = Color(0xFF2196F3),
                    isSelected = false, // TODO: Add unacknowledged filter state
                    onClick = { 
                        // TODO: Add unacknowledged filter functionality
                        // For now, we'll show all alerts
                        onFilterChange(null)
                    }
                )
            }
        }
    }
}

@Composable
private fun AlertStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    GemmaGuardCard(
        modifier = modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        variant = if (isSelected) GemmaGuardCardVariant.Elevated else GemmaGuardCardVariant.Default
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color.copy(alpha = if (isSelected) 0.2f else 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else GemmaGuardColors.TextPrimary
                )
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) color.copy(alpha = 0.8f) else GemmaGuardColors.TextSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModernFiltersSection(
    selectedFilter: ThreatLevel?,
    selectedSort: AlertSortBy,
    onFilterChange: (ThreatLevel?) -> Unit,
    onSortChange: (AlertSortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = GemmaGuardColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Filters & Sorting",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GemmaGuardColors.TextPrimary
                    )
                )
            }
            
            // Threat Level Filters
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Threat Level",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = GemmaGuardColors.TextSecondary
                    )
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ModernFilterChip(
                            selected = selectedFilter == null,
                            onClick = { onFilterChange(null) },
                            label = "All",
                            color = GemmaGuardColors.TextSecondary
                        )
                    }
                    
                    items(ThreatLevel.values()) { level ->
                        ModernFilterChip(
                            selected = selectedFilter == level,
                            onClick = { onFilterChange(level) },
                            label = level.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = getThreatLevelColor(level)
                        )
                    }
                }
            }
            
            // Sort Options
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = GemmaGuardColors.TextSecondary
                    )
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AlertSortBy.values()) { sortBy ->
                        ModernFilterChip(
                            selected = selectedSort == sortBy,
                            onClick = { onSortChange(sortBy) },
                            label = sortBy.displayName,
                            color = GemmaGuardColors.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) color else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.1f) else GemmaGuardColors.CardBackground
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color else GemmaGuardColors.TextSecondary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ModernAlertCard(
    alert: SecurityAlert,
    onClick: () -> Unit,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        variant = GemmaGuardCardVariant.Default
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with threat level, confidence and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                getThreatLevelColor(alert.threatLevel),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.threatLevel.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = getThreatLevelColor(alert.threatLevel)
                        )
                    )
                    
                    // Confidence indicator
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                GemmaGuardColors.Primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(alert.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = GemmaGuardColors.Primary
                            )
                        )
                    }
                }
                
                Text(
                    text = remember(alert.timestamp) {
                        val now = Clock.System.now()
                        val duration = now - alert.timestamp
                        when {
                            duration.inWholeMinutes < 1 -> "Just now"
                            duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes}m ago"
                            duration.inWholeHours < 24 -> "${duration.inWholeHours}h ago"
                            else -> "${duration.inWholeDays}d ago"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GemmaGuardColors.TextSecondary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Alert summary
            MarkdownText(
                text = alert.summary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                ),
                color = GemmaGuardColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Keywords tags
            if (alert.keywords.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alert.keywords.take(3)) { keyword ->
                        Box(
                            modifier = Modifier
                                .background(
                                    GemmaGuardColors.Primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = GemmaGuardColors.Primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = keyword,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (alert.keywords.size > 3) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(
                                        GemmaGuardColors.TextSecondary.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = GemmaGuardColors.TextSecondary,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+${alert.keywords.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Footer with camera info and video clip indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.camera,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B)
                        )
                    )
                    
                    if (alert.videoClip != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Has video",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF059669)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!alert.isAcknowledged) {
                        OutlinedButton(
                            onClick = onAcknowledge,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, getThreatLevelColor(alert.threatLevel)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = getThreatLevelColor(alert.threatLevel)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "Acknowledge",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Acknowledged",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acknowledged",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View details",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernEmptyState(selectedFilter: ThreatLevel?) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Glassmorphism
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (selectedFilter != null) Icons.Default.FilterAlt else Icons.Default.SecurityUpdateGood,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GemmaGuardColors.ThreatLow
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (selectedFilter != null) "No ${selectedFilter.name.lowercase()} alerts" else "No Security Alerts",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (selectedFilter != null) 
                    "Try adjusting your filters to see more alerts" 
                else 
                    "No security alerts found. Your system is secure.",
                style = MaterialTheme.typography.bodyMedium,
                color = GemmaGuardColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
