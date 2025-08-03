package com.GemmaGuardian.securitymonitor.config

/**
 * Network configuration constants for the Security Monitor app
 */
object NetworkConfig {
    /**
     * Default server IP address - Update this with your laptop's IP address
     * To find your IP address, run: ipconfig | findstr /i "IPv4" (Windows) or ifconfig (Mac/Linux)
     */
    const val DEFAULT_SERVER_HOST = "192.168.0.102"
    
    /**
     * Default server port
     */
    const val DEFAULT_SERVER_PORT = 8888
    
    /**
     * Default RTSP camera IP address
     */
    const val DEFAULT_RTSP_HOST = "192.168.0.100"
    
    /**
     * Default RTSP port
     */
    const val DEFAULT_RTSP_PORT = 554
    
    /**
     * Default RTSP credentials
     */
    const val DEFAULT_RTSP_USERNAME = "admin"
    const val DEFAULT_RTSP_PASSWORD = "admin"
    
    /**
     * Default RTSP stream path
     */
    const val DEFAULT_RTSP_PATH = "/ch0_0.264"
    
    /**
     * Complete base URL for API calls
     */
    const val BASE_URL = "http://$DEFAULT_SERVER_HOST:$DEFAULT_SERVER_PORT/"
    
    /**
     * Default RTSP URL
     */
    const val DEFAULT_RTSP_URL = "rtsp://$DEFAULT_RTSP_USERNAME:$DEFAULT_RTSP_PASSWORD@$DEFAULT_RTSP_HOST:$DEFAULT_RTSP_PORT$DEFAULT_RTSP_PATH"
    
    /**
     * Connection timeout in seconds
     */
    const val CONNECTION_TIMEOUT = 30L
    
    /**
     * Read timeout in seconds
     */
    const val READ_TIMEOUT = 30L
    
    /**
     * Write timeout in seconds
     */
    const val WRITE_TIMEOUT = 30L
}
