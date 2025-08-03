package com.GemmaGuardian.securitymonitor.data.cache

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Manages local video caching for security footage
 */
class VideoCacheManager(private val context: Context) {
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, "security_videos").apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "VideoCacheManager"
        private const val MAX_CACHE_SIZE_MB = 500L // 500 MB cache limit
        private const val MAX_CACHE_AGE_DAYS = 7L // Keep videos for 7 days
    }
    
    /**
     * Get cached video URI or download if not cached
     */
    suspend fun getCachedVideoUri(videoUrl: String, videoId: String): Uri = withContext(Dispatchers.IO) {
        try {
            val cachedFile = getCachedFile(videoId)
            
            if (cachedFile.exists() && cachedFile.length() > 0) {
                Log.d(TAG, "✅ Video found in cache: $videoId")
                // Update access time
                cachedFile.setLastModified(System.currentTimeMillis())
                return@withContext Uri.fromFile(cachedFile)
            }
            
            Log.d(TAG, "🔄 Downloading video to cache: $videoId from $videoUrl")
            val downloadedFile = downloadVideo(videoUrl, videoId)
            
            if (downloadedFile != null && downloadedFile.exists()) {
                Log.d(TAG, "✅ Video downloaded and cached: $videoId (${downloadedFile.length()} bytes)")
                // Clean up old cache entries after successful download
                cleanupOldCache()
                Uri.fromFile(downloadedFile)
            } else {
                Log.e(TAG, "❌ Failed to download video: $videoId")
                // Return original URL as fallback
                Uri.parse(videoUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in video cache: ${e.message}")
            // Return original URL as fallback
            Uri.parse(videoUrl)
        }
    }
    
    /**
     * Check if video is cached locally
     */
    fun isVideoCached(videoId: String): Boolean {
        val cachedFile = getCachedFile(videoId)
        return cachedFile.exists() && cachedFile.length() > 0
    }
    
    /**
     * Get cached video file
     */
    private fun getCachedFile(videoId: String): File {
        val fileName = "${hashString(videoId)}.mp4"
        return File(cacheDir, fileName)
    }
    
    /**
     * Download video from server
     */
    private suspend fun downloadVideo(videoUrl: String, videoId: String): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(videoUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to download video: HTTP ${response.code}")
                return@withContext null
            }
            
            val cachedFile = getCachedFile(videoId)
            val inputStream = response.body?.byteStream()
            val outputStream = FileOutputStream(cachedFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            response.close()
            
            if (cachedFile.length() > 0) {
                Log.d(TAG, "✅ Video download completed: ${cachedFile.length()} bytes")
                cachedFile
            } else {
                Log.e(TAG, "❌ Downloaded file is empty")
                cachedFile.delete()
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Download error: ${e.message}")
            null
        }
    }
    
    /**
     * Clean up old cached videos
     */
    private fun cleanupOldCache() {
        try {
            val currentTime = System.currentTimeMillis()
            val maxAge = MAX_CACHE_AGE_DAYS * 24 * 60 * 60 * 1000 // Convert to milliseconds
            val maxCacheSize = MAX_CACHE_SIZE_MB * 1024 * 1024 // Convert to bytes
            
            val files = cacheDir.listFiles() ?: return
            
            // Remove old files
            val filesToDelete = files.filter { file ->
                currentTime - file.lastModified() > maxAge
            }
            
            filesToDelete.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "🗑️ Deleted old cached video: ${file.name}")
                }
            }
            
            // Check total cache size and remove oldest files if needed
            val remainingFiles = files.filter { it.exists() }
            val totalSize = remainingFiles.sumOf { it.length() }
            
            if (totalSize > maxCacheSize) {
                val sortedByAge = remainingFiles.sortedBy { it.lastModified() }
                var currentSize = totalSize
                
                for (file in sortedByAge) {
                    if (currentSize <= maxCacheSize) break
                    
                    currentSize -= file.length()
                    if (file.delete()) {
                        Log.d(TAG, "🗑️ Deleted cached video to free space: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "🧹 Cache cleanup completed. Files: ${cacheDir.listFiles()?.size ?: 0}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cache cleanup error: ${e.message}")
        }
    }
    
    /**
     * Get cache status information
     */
    fun getCacheInfo(): CacheInfo {
        val files = cacheDir.listFiles() ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        val count = files.size
        
        return CacheInfo(
            fileCount = count,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / (1024 * 1024),
            availableSpaceBytes = cacheDir.freeSpace
        )
    }
    
    /**
     * Get all cached video files with their metadata
     */
    fun getCachedVideos(): List<CachedVideo> {
        return try {
            val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp4") } ?: emptyList()
            
            files.map { file ->
                CachedVideo(
                    videoId = file.nameWithoutExtension.substringAfterLast("_"),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    lastModified = file.lastModified(),
                    uri = Uri.fromFile(file)
                )
            }.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting cached videos: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get cached video URI without downloading (returns null if not cached)
     */
    fun getCachedVideoUriIfExists(videoId: String): Uri? {
        return if (isVideoCached(videoId)) {
            Uri.fromFile(getCachedFile(videoId))
        } else {
            null
        }
    }

    /**
     * Clear all cached videos
     */
    fun clearCache(): Boolean {
        return try {
            val files = cacheDir.listFiles() ?: return true
            var success = true
            
            files.forEach { file ->
                if (!file.delete()) {
                    success = false
                    Log.e(TAG, "❌ Failed to delete cached file: ${file.name}")
                } else {
                    Log.d(TAG, "🗑️ Deleted cached file: ${file.name}")
                }
            }
            
            Log.d(TAG, if (success) "✅ Cache cleared successfully" else "⚠️ Some files could not be deleted")
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing cache: ${e.message}")
            false
        }
    }
    
    /**
     * Generate hash for consistent file naming
     */
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

/**
 * Cache information data class
 */
data class CacheInfo(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val totalSizeMB: Long,
    val availableSpaceBytes: Long
)

/**
 * Cached video metadata
 */
data class CachedVideo(
    val videoId: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val lastModified: Long,
    val uri: Uri
)
