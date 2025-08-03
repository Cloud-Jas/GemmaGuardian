package com.GemmaGuardian.securitymonitor.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.data.notification.NotificationHandler
import com.GemmaGuardian.securitymonitor.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<SecurityAlert> = emptyList(),
    val filteredAlerts: List<SecurityAlert> = emptyList(),
    val selectedFilter: ThreatLevel? = null,
    val sortBy: AlertSortBy = AlertSortBy.TIME_DESC,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

enum class AlertSortBy(val displayName: String) {
    TIME_DESC("Latest First"),
    TIME_ASC("Oldest First"),
    THREAT_LEVEL("Threat Level"),
    CONFIDENCE("Confidence")
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val notificationHandler: NotificationHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()
    
    init {
        loadAlertsOnNavigation() // Load alerts when navigating to alerts page
        observeNewAlerts()        // Listen for real-time alerts via UDP
    }
    
    // Load alerts only when user navigates to alerts page
    private fun loadAlertsOnNavigation() {
        viewModelScope.launch {
            android.util.Log.d("AlertsViewModel", "🚀 Loading alerts on navigation")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                android.util.Log.d("AlertsViewModel", "📡 Calling securityRepository.getAllAlerts()")
                val alerts = securityRepository.getAllAlerts()
                android.util.Log.d("AlertsViewModel", "✅ Repository returned ${alerts.size} alerts")
                updateAlertsState(alerts)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                android.util.Log.e("AlertsViewModel", "💥 Error loading alerts: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load alerts"
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
                    // Add new alert to the existing list
                    val currentAlerts = _uiState.value.alerts.toMutableList()
                    currentAlerts.add(0, newAlert) // Add to beginning
                    updateAlertsState(currentAlerts)
                }
        }
    }
    
    private fun updateAlertsState(alerts: List<SecurityAlert>) {
        val currentState = _uiState.value
        val filteredAndSorted = filterAndSortAlerts(
            alerts = alerts,
            filter = currentState.selectedFilter,
            sortBy = currentState.sortBy
        )
        
        android.util.Log.d("AlertsViewModel", """
            📊 Updating alerts state:
            - Raw alerts: ${alerts.size}
            - After filtering: ${filteredAndSorted.size}
            - Current filter: ${currentState.selectedFilter}
            - Sort by: ${currentState.sortBy}
        """.trimIndent())
        
        _uiState.update {
            it.copy(
                alerts = alerts,
                filteredAlerts = filteredAndSorted
            )
        }
    }
    
    fun setThreatLevelFilter(threatLevel: ThreatLevel?) {
        val currentState = _uiState.value
        val filtered = filterAndSortAlerts(
            alerts = currentState.alerts,
            filter = threatLevel,
            sortBy = currentState.sortBy
        )
        
        _uiState.update {
            it.copy(
                selectedFilter = threatLevel,
                filteredAlerts = filtered
            )
        }
    }
    
    fun setSortBy(sortBy: AlertSortBy) {
        val currentState = _uiState.value
        val sorted = filterAndSortAlerts(
            alerts = currentState.alerts,
            filter = currentState.selectedFilter,
            sortBy = sortBy
        )
        
        _uiState.update {
            it.copy(
                sortBy = sortBy,
                filteredAlerts = sorted
            )
        }
    }
    
    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            try {
                securityRepository.acknowledgeAlert(alertId)
                // Update local state immediately for better UX
                _uiState.update { state ->
                    val updatedAlerts = state.alerts.map { alert ->
                        if (alert.id == alertId) {
                            alert.copy(isAcknowledged = true)
                        } else alert
                    }
                    
                    val filteredAndSorted = filterAndSortAlerts(
                        alerts = updatedAlerts,
                        filter = state.selectedFilter,
                        sortBy = state.sortBy
                    )
                    
                    state.copy(
                        alerts = updatedAlerts,
                        filteredAlerts = filteredAndSorted
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to acknowledge alert")
                }
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            try {
                // Manually fetch fresh alerts from API
                val alerts = securityRepository.getAllAlerts()
                updateAlertsState(alerts)
                _uiState.update { it.copy(isRefreshing = false, error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh alerts"
                    )
                }
            }
        }
    }
    
    // Method to get specific alert details when clicked from notification or alert card
    suspend fun getAlertDetails(alertId: String): SecurityAlert? {
        return try {
            android.util.Log.d("AlertsViewModel", "🔍 Fetching alert details for ID: $alertId")
            // Hit the alert details endpoint to get fresh data
            securityRepository.getAlertDetails(alertId)
        } catch (e: Exception) {
            android.util.Log.e("AlertsViewModel", "❌ Failed to fetch alert details: ${e.message}")
            null
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun filterAndSortAlerts(
        alerts: List<SecurityAlert>,
        filter: ThreatLevel?,
        sortBy: AlertSortBy
    ): List<SecurityAlert> {
        var result = alerts
        
        android.util.Log.d("AlertsViewModel", """
            🔍 Filtering ${alerts.size} alerts:
            - Filter: $filter
            - Sort by: $sortBy
        """.trimIndent())
        
        // Apply filter
        filter?.let { threatLevel ->
            // Filter for exact threat level OR higher severity levels
            result = result.filter { alert ->
                when (threatLevel) {
                    ThreatLevel.LOW -> true // Show all levels
                    ThreatLevel.MEDIUM -> alert.threatLevel in listOf(ThreatLevel.MEDIUM, ThreatLevel.HIGH, ThreatLevel.CRITICAL)
                    ThreatLevel.HIGH -> alert.threatLevel in listOf(ThreatLevel.HIGH, ThreatLevel.CRITICAL)
                    ThreatLevel.CRITICAL -> alert.threatLevel == ThreatLevel.CRITICAL
                }
            }
            android.util.Log.d("AlertsViewModel", "📝 After filtering by $threatLevel and above: ${result.size} alerts")
        }
        
        // Apply sorting
        result = when (sortBy) {
            AlertSortBy.TIME_DESC -> result.sortedByDescending { it.timestamp }
            AlertSortBy.TIME_ASC -> result.sortedBy { it.timestamp }
            AlertSortBy.THREAT_LEVEL -> result.sortedByDescending { it.threatLevel.ordinal }
            AlertSortBy.CONFIDENCE -> result.sortedByDescending { it.confidence }
        }
        
        android.util.Log.d("AlertsViewModel", "✅ Final filtered and sorted alerts: ${result.size}")
        
        return result
    }
}
