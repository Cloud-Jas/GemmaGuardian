package com.GemmaGuardian.securitymonitor.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GemmaGuardian.securitymonitor.data.cache.CacheInfo
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.presentation.components.*
import com.GemmaGuardian.securitymonitor.presentation.theme.GemmaGuardColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CacheSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCacheInfo()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Video Cache",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cache Status Card
            item {
                CacheStatusCard(
                    cacheInfo = uiState.cacheInfo,
                    isLoading = uiState.isLoading
                )
            }
            
            // Cache Actions Card
            item {
                CacheActionsCard(
                    onClearCache = viewModel::clearCache,
                    onRefreshInfo = viewModel::loadCacheInfo,
                    isLoading = uiState.isLoading
                )
            }
            
            // Cache Information Card
            item {
                CacheInfoCard()
            }
            
            // Show success/error messages
            if (uiState.message.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (uiState.isError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheStatusCard(
    cacheInfo: CacheInfo?,
    isLoading: Boolean
) {
    GemmaGuardCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GemmaGuardCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Cache Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cache Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Loading cache information...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (cacheInfo != null) {
                CacheStatusRow(
                    label = "Cached Videos",
                    value = "${cacheInfo.fileCount}",
                    icon = Icons.Default.VideoLibrary
                )
                
                CacheStatusRow(
                    label = "Cache Size",
                    value = "${cacheInfo.totalSizeMB} MB",
                    icon = Icons.Default.Storage
                )
                
                CacheStatusRow(
                    label = "Available Space",
                    value = "${cacheInfo.availableSpaceBytes / (1024 * 1024)} MB",
                    icon = Icons.Default.Folder
                )
            } else {
                Text(
                    text = "Unable to load cache information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CacheStatusRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CacheActionsCard(
    onClearCache: () -> Unit,
    onRefreshInfo: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cache Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefreshInfo,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                
                Button(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cache")
                }
            }
        }
    }
}

@Composable
private fun CacheInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About Video Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "• Videos are automatically cached when viewed for faster playback\n" +
                        "• Cache limit: 500 MB\n" +
                        "• Videos older than 7 days are automatically removed\n" +
                        "• Cached videos can be viewed offline\n" +
                        "• Cache is stored in app's private storage",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
            )
        }
    }
}

@HiltViewModel
class CacheSettingsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CacheSettingsUiState())
    val uiState: StateFlow<CacheSettingsUiState> = _uiState.asStateFlow()
    
    fun loadCacheInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "", isError = false)
            
            try {
                val cacheInfo = securityRepository.getVideoCacheInfo()
                _uiState.value = _uiState.value.copy(
                    cacheInfo = cacheInfo,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to load cache info: ${e.message}",
                    isError = true
                )
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "", isError = false)
            
            try {
                val success = securityRepository.clearVideoCache()
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        message = "Cache cleared successfully",
                        isError = false
                    )
                    loadCacheInfo() // Refresh cache info
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "Failed to clear cache completely",
                        isError = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error clearing cache: ${e.message}",
                    isError = true
                )
            }
        }
    }
}

data class CacheSettingsUiState(
    val cacheInfo: CacheInfo? = null,
    val isLoading: Boolean = false,
    val message: String = "",
    val isError: Boolean = false
)
