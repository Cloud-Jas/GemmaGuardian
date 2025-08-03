package com.GemmaGuardian.securitymonitor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GemmaGuardian.securitymonitor.config.NetworkConfig
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.data.notification.NotificationHandler
import com.GemmaGuardian.securitymonitor.data.network.ConnectionState
import com.GemmaGuardian.securitymonitor.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val securityStats: SecurityStats? = null,
    val recentAlerts: List<SecurityAlert> = emptyList(),
    val cameraStatus: List<CameraInfo> = emptyList(),
    val systemHealth: SystemHealth? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val connectionStatusMessage: String = "Connecting..."
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val notificationHandler: NotificationHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Keep track of all alerts locally for recent alerts display
    private val _allAlerts = MutableStateFlow<List<SecurityAlert>>(emptyList())
    
    init {
        discoverAndConnect()
        observeNewAlerts()
        observeConnectionState()
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            securityRepository.connectionState.collect { connectionState ->
                val statusMessage = securityRepository.getConnectionStatusMessage()
                _uiState.update { it.copy(connectionStatusMessage = statusMessage) }
            }
        }
    }
    
    private fun discoverAndConnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // First try to discover the surveillance system
                val serverInfo = securityRepository.discoverServer()
                if (serverInfo != null) {
                    // Successfully discovered, now load data
                    loadHomeData()
                } else {
                    // Try connecting using stored preferences
                    val connected = securityRepository.discoverServer() != null
                    if (connected) {
                        loadHomeData()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to connect to surveillance system. Please ensure the system is running."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Connection failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val stats = securityRepository.getSecurityStats()
                val alerts = securityRepository.getRecentAlerts(5) // Load initial alerts once
                val cameras = securityRepository.getCameraStatus()
                val health = securityRepository.getSystemHealth()
                
                // Debug logging for system health status
                android.util.Log.d("HomeViewModel", "🔍 System Health Status: ${health.status}")
                android.util.Log.d("HomeViewModel", "🔍 System Health Data: $health")
                
                // Update local alerts cache
                _allAlerts.value = alerts
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        securityStats = stats,
                        recentAlerts = alerts,
                        cameraStatus = cameras,
                        systemHealth = health,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    // Observe real-time alerts from UDP notifications, not polling
    private fun observeNewAlerts() {
        viewModelScope.launch {
            notificationHandler.newAlerts
                .catch { e ->
                    _uiState.update {
                        it.copy(error = "Failed to receive real-time alerts: ${e.message}")
                    }
                }
                .collect { newAlert ->
                    // Add new alert to the beginning of the list and keep only recent ones
                    val currentAlerts = _allAlerts.value.toMutableList()
                    currentAlerts.add(0, newAlert) // Add to beginning
                    val updatedAlerts = currentAlerts.take(10) // Keep only 10 most recent
                    
                    _allAlerts.value = updatedAlerts
                    
                    _uiState.update {
                        it.copy(recentAlerts = updatedAlerts.take(5)) // Show only 5 on home screen
                    }
                }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            try {
                // Check connection first, retry if needed
                val currentConnectionState = securityRepository.connectionState.value
                if (currentConnectionState != ConnectionState.CONNECTED) {
                    // Try to reconnect using stored preferences
                    val serverInfo = securityRepository.discoverServer()
                    if (serverInfo == null) {
                        // If discovery fails, try direct connection with stored settings
                        securityRepository.connectToServer(securityRepository.getDefaultIp())
                    }
                }
                
                // Load fresh data
                val stats = securityRepository.getSecurityStats()
                val alerts = securityRepository.getRecentAlerts(5)
                val cameras = securityRepository.getCameraStatus()
                val health = securityRepository.getSystemHealth()
                
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        securityStats = stats,
                        recentAlerts = alerts,
                        cameraStatus = cameras,
                        systemHealth = health,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh data"
                    )
                }
            }
        }
    }
    
    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            try {
                securityRepository.acknowledgeAlert(alertId)
                // Update local state
                _uiState.update { state ->
                    state.copy(
                        recentAlerts = state.recentAlerts.map { alert ->
                            if (alert.id == alertId) {
                                alert.copy(isAcknowledged = true)
                            } else alert
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to acknowledge alert")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
