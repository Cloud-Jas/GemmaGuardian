package com.GemmaGuardian.securitymonitor.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for handling server settings and app preferences
 */
class ConfigurationManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "security_monitor_prefs"
        private const val KEY_SERVER_HOST = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }
    
    /**
     * Get the configured server host
     */
    fun getServerHost(): String {
        return prefs.getString(KEY_SERVER_HOST, NetworkConfig.DEFAULT_SERVER_HOST) 
            ?: NetworkConfig.DEFAULT_SERVER_HOST
    }
    
    /**
     * Set the server host
     */
    fun setServerHost(host: String) {
        prefs.edit()
            .putString(KEY_SERVER_HOST, host)
            .apply()
    }
    
    /**
     * Get the configured server port
     */
    fun getServerPort(): Int {
        return prefs.getInt(KEY_SERVER_PORT, NetworkConfig.DEFAULT_SERVER_PORT)
    }
    
    /**
     * Set the server port
     */
    fun setServerPort(port: Int) {
        prefs.edit()
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }
    
    /**
     * Get the complete base URL for API calls
     * Always constructs URL dynamically from current server settings
     */
    fun getBaseUrl(): String {
        return "http://${getServerHost()}:${getServerPort()}/"
    }
    
    /**
     * Check if this is the first app launch
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }
    
    /**
     * Mark that the app has been launched before
     */
    fun setFirstLaunchCompleted() {
        prefs.edit()
            .putBoolean(KEY_IS_FIRST_LAUNCH, false)
            .apply()
    }
    
    /**
     * Get video streaming URL for a given filename
     */
    fun getVideoUrl(filename: String): String {
        return "${getBaseUrl()}video/$filename"
    }
    
    /**
     * Get RTSP URL for live streaming (if configured)
     */
    fun getRtspUrl(): String {
        // This could be configurable in the future
        return "rtsp://admin:admin@${getServerHost()}:554/ch0_0.264"
    }
    
    /**
     * Reset configuration to defaults
     */
    fun resetToDefaults() {
        prefs.edit()
            .clear()
            .putString(KEY_SERVER_HOST, NetworkConfig.DEFAULT_SERVER_HOST)
            .putInt(KEY_SERVER_PORT, NetworkConfig.DEFAULT_SERVER_PORT)
            .putBoolean(KEY_IS_FIRST_LAUNCH, false)
            .apply()
    }
    
    /**
     * Test if the server is reachable (basic validation)
     */
    fun isValidServerConfig(): Boolean {
        val host = getServerHost()
        val port = getServerPort()
        
        // Basic validation
        return host.isNotBlank() && 
               isValidIpAddress(host) && 
               port in 1..65535
    }
    
    private fun isValidIpAddress(ip: String): Boolean {
        if (ip.isBlank()) return false
        
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
}
