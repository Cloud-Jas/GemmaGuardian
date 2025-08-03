package com.GemmaGuardian.securitymonitor.presentation.videoplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.GemmaGuardian.securitymonitor.domain.model.VideoRecording
import com.GemmaGuardian.securitymonitor.presentation.videoplayer.components.LocalVideoPlayer
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import com.GemmaGuardian.securitymonitor.presentation.components.*

@Composable
private fun MarkdownText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
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
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    // Load video details
    LaunchedEffect(videoId) {
        viewModel.loadVideoDetails(videoId)
    }
    
    val videoDetails by viewModel.videoDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = GemmaGuardColors.backgroundGradientColors
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = GemmaGuardColors.Primary
                    )
                    Text(
                        text = "Loading video...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GemmaGuardColors.TextPrimary
                    )
                }
            }
        }
        
        error != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = GemmaGuardColors.backgroundGradientColors
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = GemmaGuardColors.ThreatCritical,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load video",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GemmaGuardColors.ThreatCritical
                )
                Text(
                    text = error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemmaGuardColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GemmaGuardColors.Primary
                    )
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
        
        videoDetails != null -> {
            // Enhanced video player with analysis
            EnhancedVideoPlayerScreen(
                videoDetails = videoDetails!!,
                configurationManager = viewModel.configurationManager,
                onBack = onNavigateBack,
                modifier = modifier
            )
        }
        
        else -> {
            // Fallback state
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = GemmaGuardColors.backgroundGradientColors
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No video data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GemmaGuardColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun EnhancedVideoPlayerScreen(
    videoDetails: VideoRecording,
    configurationManager: com.GemmaGuardian.securitymonitor.config.ConfigurationManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = GemmaGuardColors.backgroundGradientColors
                )
            )
    ) {
        // Video Player Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top bar with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GemmaGuardColors.TextPrimary
                        )
                    }
                    Text(
                        text = "Security Footage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                        color = GemmaGuardColors.TextPrimary
                    )
                }
                
                // Video Player
                GemmaGuardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    variant = GemmaGuardCardVariant.Elevated
                ) {
                    LocalVideoPlayer(
                        videoFileName = videoDetails.fileName,
                        configurationManager = configurationManager,
                        videoUrl = videoDetails.url,
                        onBack = { /* Don't navigate back from video controls */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        }
        
        // Analysis Section
        if (videoDetails.frameAnalyses.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Frame-by-Frame Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = GemmaGuardColors.TextPrimary
                )
                
                Text(
                    text = "AI analysis results during video playback",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemmaGuardColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            items(videoDetails.frameAnalyses) { analysis ->
                FrameAnalysisCard(
                    analysis = analysis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        
        // Consolidated Analysis
        if (videoDetails.consolidatedAnalysis.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                GemmaGuardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    variant = GemmaGuardCardVariant.Elevated
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Analysis",
                                tint = GemmaGuardColors.Primary
                            )
                            Text(
                                text = "AI Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp),
                                color = GemmaGuardColors.TextPrimary
                            )
                        }
                        
                        MarkdownText(
                            text = videoDetails.consolidatedAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GemmaGuardColors.TextPrimary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
        
        // Keywords Section
        if (videoDetails.keywords.isNotEmpty()) {
            item {
                GemmaGuardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    variant = GemmaGuardCardVariant.Glassmorphism
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Detected Keywords",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GemmaGuardColors.TextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Keywords using LazyRow for better performance and design
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(videoDetails.keywords) { keyword ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    GemmaGuardColors.Primary.copy(alpha = 0.2f),
                                                    GemmaGuardColors.Primary.copy(alpha = 0.1f)
                                                )
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = GemmaGuardColors.Primary.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = keyword,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Add bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun FrameAnalysisCard(
    analysis: com.GemmaGuardian.securitymonitor.domain.model.FrameAnalysis,
    modifier: Modifier = Modifier
) {
    GemmaGuardCard(
        modifier = modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Default
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batch ${analysis.batchNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GemmaGuardColors.Primary
                )
                
                Surface(
                    color = GemmaGuardColors.Primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${analysis.framesInBatch} frames",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = GemmaGuardColors.TextPrimary
                    )
                }
            }
            
            if (analysis.timestampRange.isNotEmpty()) {
                Text(
                    text = "Time: ${analysis.timestampRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GemmaGuardColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            MarkdownText(
                text = analysis.analysis,
                style = MaterialTheme.typography.bodyMedium,
                color = GemmaGuardColors.TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
