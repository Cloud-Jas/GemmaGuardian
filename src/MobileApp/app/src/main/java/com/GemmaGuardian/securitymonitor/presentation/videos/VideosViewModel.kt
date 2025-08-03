package com.GemmaGuardian.securitymonitor.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GemmaGuardian.securitymonitor.data.repository.SecurityRepository
import com.GemmaGuardian.securitymonitor.domain.model.VideoRecording
import com.GemmaGuardian.securitymonitor.data.cache.CachedVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideosViewModel @Inject constructor(
    private val repository: SecurityRepository
) : ViewModel() {
    
    private val _videoRecordings = MutableStateFlow<List<VideoRecording>>(emptyList())
    val videoRecordings: StateFlow<List<VideoRecording>> = _videoRecordings.asStateFlow()
    
    private val _cachedVideos = MutableStateFlow<List<CachedVideo>>(emptyList())
    val cachedVideos: StateFlow<List<CachedVideo>> = _cachedVideos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _showOnlyCached = MutableStateFlow(false)
    val showOnlyCached: StateFlow<Boolean> = _showOnlyCached.asStateFlow()
    
    init {
        loadCachedVideos()
        loadVideoRecordings()
    }
    
    fun loadVideoRecordings(limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val recordings = repository.getVideoRecordings(limit)
                _videoRecordings.value = recordings
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load video recordings"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadCachedVideos() {
        viewModelScope.launch {
            try {
                val cached = repository.getCachedVideos()
                _cachedVideos.value = cached
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cached videos"
            }
        }
    }
    
    fun toggleCachedView() {
        _showOnlyCached.value = !_showOnlyCached.value
    }
    
    fun refreshVideos() {
        loadCachedVideos()
        loadVideoRecordings()
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun isVideoCached(videoId: String): Boolean {
        return repository.isVideoCached(videoId)
    }
}
