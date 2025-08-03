# Network Configuration Guide

## Setting up your Android app to connect to your laptop's API server

### 1. Find your laptop's IP address

**Windows:**
```bash
ipconfig | findstr /i "IPv4"
```

**Mac/Linux:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### 2. Update the Android app configuration

Edit the file: `src/MobileApp/app/src/main/java/com/GemmaGuardian/securitymonitor/config/NetworkConfig.kt`

Update the `DEFAULT_SERVER_HOST` constant with your laptop's IP address:

```kotlin
const val DEFAULT_SERVER_HOST = "192.168.0.102" // Replace with your laptop's IP
```

### 3. Current Configuration

- **Server IP**: `192.168.0.102`
- **Server Port**: `8888`
- **Full URL**: `http://192.168.0.102:8888/`

### 4. Network Requirements

- Both devices (laptop and Android) must be on the **same WiFi network**
- Make sure your laptop's firewall allows connections on port 8888
- Ensure the API server is running on your laptop

### 5. Testing the connection

1. Start the API server on your laptop
2. Build and install the Android app
3. Check if the app can connect to the server

### 6. Troubleshooting

If the connection fails:

1. **Check same network**: Verify both devices are on the same WiFi
2. **Check IP address**: Ensure the IP in NetworkConfig.kt matches your laptop's IP
3. **Check firewall**: Make sure Windows Firewall allows the connection
4. **Check server**: Ensure the API server is running on port 8888
5. **Try ping**: From Android device browser, try accessing `http://YOUR_LAPTOP_IP:8888`

### 7. Common IP ranges

- `192.168.1.x` - Most common home networks
- `192.168.0.x` - Alternative home network range  
- `10.0.0.x` - Some routers use this range

### 8. Windows Firewall Configuration

If needed, allow the port through Windows Firewall:

1. Open Windows Defender Firewall
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Click "Change Settings" then "Allow another app..."
4. Add your API server application or allow port 8888
