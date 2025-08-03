package com.GemmaGuardian.securitymonitor.presentation.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GemmaGuardian.securitymonitor.config.ConfigurationManager
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.domain.model.VideoRecording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val repository: SecurityRepository,
    val configurationManager: ConfigurationManager
) : ViewModel() {
    
    private val _videoDetails = MutableStateFlow<VideoRecording?>(null)
    val videoDetails: StateFlow<VideoRecording?> = _videoDetails.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadVideoDetails(videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val details = repository.getVideoDetails(videoId)
                if (details != null) {
                    _videoDetails.value = details
                } else {
                    _error.value = "Video not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load video details"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
