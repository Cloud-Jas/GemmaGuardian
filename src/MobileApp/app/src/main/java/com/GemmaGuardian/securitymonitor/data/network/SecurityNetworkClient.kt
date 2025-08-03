package com.GemmaGuardian.securitymonitor.data.network

import android.content.Context
import android.content.SharedPreferences
import com.GemmaGuardian.securitymonitor.config.NetworkConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import java.net.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityNetworkClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SharedPreferences
) {
    
    companion object {
        private const val SERVER_IP_KEY = "server_ip"
        private const val SERVER_PORT_KEY = "server_port"
        private const val DISCOVERY_TIMEOUT = 10000L // Increased to 10 seconds
    }
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()
    
    init {
        // Try to restore previous connection
        val savedIp = preferences.getString(SERVER_IP_KEY, NetworkConfig.DEFAULT_SERVER_HOST)
        val savedPort = preferences.getInt(SERVER_PORT_KEY, NetworkConfig.DEFAULT_SERVER_PORT)
        
        if (savedIp != null) {
            _serverInfo.value = ServerInfo(savedIp, savedPort)
        }
    }
    
    /**
     * Connect to the known surveillance system IP
     */
    suspend fun discoverServer(): ServerInfo? {
        _connectionState.value = ConnectionState.CONNECTING
        
        return try {
            val defaultIp = preferences.getString(SERVER_IP_KEY, NetworkConfig.DEFAULT_SERVER_HOST) ?: NetworkConfig.DEFAULT_SERVER_HOST
            val defaultPort = preferences.getInt(SERVER_PORT_KEY, NetworkConfig.DEFAULT_SERVER_PORT)
            
            android.util.Log.d("SecurityNetworkClient", "Attempting to discover server at $defaultIp:$defaultPort")
            
            // Try to connect to the known server IP
            val connected = connectToServer(defaultIp, defaultPort)
            if (connected) {
                android.util.Log.d("SecurityNetworkClient", "Server discovery successful")
                _serverInfo.value
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                android.util.Log.w("SecurityNetworkClient", "Server discovery failed")
                null
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            android.util.Log.e("SecurityNetworkClient", "Error during server discovery", e)
            null
        }
    }
    
    /**
     * Connect to specific server IP
     */
    suspend fun connectToServer(ip: String, port: Int = preferences.getInt(SERVER_PORT_KEY, NetworkConfig.DEFAULT_SERVER_PORT)): Boolean {
        _connectionState.value = ConnectionState.CONNECTING
        
        return try {
            android.util.Log.d("SecurityNetworkClient", "Attempting to connect to $ip:$port")
            
            // Test connection
            val response = testConnection(ip, port)
            if (response) {
                val serverInfo = ServerInfo(ip, port)
                saveServerInfo(serverInfo)
                _serverInfo.value = serverInfo
                _connectionState.value = ConnectionState.CONNECTED
                android.util.Log.d("SecurityNetworkClient", "Successfully connected to $ip:$port")
                true
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                android.util.Log.w("SecurityNetworkClient", "Failed to connect to $ip:$port")
                false
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            android.util.Log.e("SecurityNetworkClient", "Error connecting to $ip:$port", e)
            false
        }
    }
    
    /**
     * Test if we can reach the surveillance system
     */
    suspend fun testConnection(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Test HTTP connection to health endpoint
                val client = OkHttpClient.Builder()
                    .connectTimeout(DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("http://$ip:$port/health")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                // Log the result for debugging
                android.util.Log.d("SecurityNetworkClient", "testConnection to http://$ip:$port/health - Success: $success, Code: ${response.code}")
                
                success
            } catch (e: Exception) {
                android.util.Log.e("SecurityNetworkClient", "testConnection failed to http://$ip:$port/health", e)
                false
            }
        }
    }
    
    /**
     * Execute API call with error handling
     */
    suspend fun <T> executeApiCall(call: suspend () -> Response<T>): NetworkResult<T> {
        return try {
            android.util.Log.d("SecurityNetworkClient", "executeApiCall - Connection state: ${_connectionState.value}")
            
            if (_connectionState.value != ConnectionState.CONNECTED) {
                android.util.Log.w("SecurityNetworkClient", "executeApiCall failed - Not connected to surveillance system")
                return NetworkResult.Error("Not connected to surveillance system")
            }
            
            android.util.Log.d("SecurityNetworkClient", "executeApiCall - Making API call...")
            val response = call()
            android.util.Log.d("SecurityNetworkClient", "executeApiCall - Response code: ${response.code()}, successful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    android.util.Log.d("SecurityNetworkClient", "executeApiCall - Success with body")
                    NetworkResult.Success(body)
                } ?: {
                    android.util.Log.w("SecurityNetworkClient", "executeApiCall - Success but empty response body")
                    NetworkResult.Error("Empty response")
                }()
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                android.util.Log.e("SecurityNetworkClient", "executeApiCall - HTTP error: $errorMsg")
                NetworkResult.Error(errorMsg)
            }
        } catch (e: SocketTimeoutException) {
            _connectionState.value = ConnectionState.ERROR
            android.util.Log.e("SecurityNetworkClient", "executeApiCall - Socket timeout", e)
            NetworkResult.Error("Connection timeout")
        } catch (e: ConnectException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            android.util.Log.e("SecurityNetworkClient", "executeApiCall - Connection error", e)
            NetworkResult.Error("Cannot connect to server")
        } catch (e: Exception) {
            android.util.Log.e("SecurityNetworkClient", "executeApiCall - Unexpected error", e)
            NetworkResult.Error(e.message ?: "Unknown network error")
        }
    }
    
    private fun saveServerInfo(serverInfo: ServerInfo) {
        preferences.edit()
            .putString(SERVER_IP_KEY, serverInfo.ip)
            .putInt(SERVER_PORT_KEY, serverInfo.port)
            .apply()
    }
    
    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _serverInfo.value = null
    }
    
    /**
     * Get the default port from shared preferences
     */
    fun getDefaultPort(): Int {
        return preferences.getInt(SERVER_PORT_KEY, NetworkConfig.DEFAULT_SERVER_PORT)
    }
    
    /**
     * Get the default IP from shared preferences
     */
    fun getDefaultIp(): String {
        return preferences.getString(SERVER_IP_KEY, NetworkConfig.DEFAULT_SERVER_HOST) ?: NetworkConfig.DEFAULT_SERVER_HOST
    }
    
    /**
     * Get the current server connection status message for UI display
     */
    fun getConnectionStatusMessage(): String {
        val defaultIp = preferences.getString(SERVER_IP_KEY, NetworkConfig.DEFAULT_SERVER_HOST) ?: NetworkConfig.DEFAULT_SERVER_HOST
        val defaultPort = preferences.getInt(SERVER_PORT_KEY, NetworkConfig.DEFAULT_SERVER_PORT)
        
        return when (_connectionState.value) {
            ConnectionState.DISCONNECTED -> "System offline - Check if surveillance server is running on $defaultIp:$defaultPort"
            ConnectionState.CONNECTING -> "Connecting to surveillance system..."
            ConnectionState.CONNECTED -> "Connected to ${_serverInfo.value?.ip ?: defaultIp}"
            ConnectionState.ERROR -> "Connection error - Please check network settings or contact support"
        }
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ServerInfo(
    val ip: String,
    val port: Int,
    val discoveredAt: Instant = kotlinx.datetime.Clock.System.now()
)

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}
