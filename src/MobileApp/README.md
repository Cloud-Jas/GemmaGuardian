# 📱 GemmaGuardian Mobile App

Android companion app for the GemmaGuardian AI surveillance system.

## 🚀 Quick Setup

### Prerequisites
- **Android Studio** (latest version)
- **Android SDK** (API level 21+)
- **GemmaGuardian server** running with API enabled

### Build & Install

1. **Open in Android Studio**
   ```bash
   # Open this directory in Android Studio
   cd src/MobileApp
   ```

2. **Sync Dependencies**
   - Android Studio will automatically prompt to sync
   - Or manually: `Tools > Sync Project with Gradle Files`

3. **Configure Server Connection**
   - Edit `app/src/main/res/values/strings.xml`
   - Update `server_url` to your GemmaGuardian server IP:
   ```xml
   <string name="server_url">http://YOUR_SERVER_IP:8888</string>
   ```

4. **Build & Install**
   - Connect Android device or start emulator
   - Run: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - Install APK on device

## ✨ Features

- 📹 **Live Camera Feed**: View real-time RTSP stream
- 🔔 **Push Notifications**: Instant security alerts
- 📊 **Analysis History**: Browse past detections
- ⚙️ **Remote Configuration**: Adjust system settings
- 🛡️ **Threat Assessment**: View AI security analysis

## 🔧 Configuration

### Server Connection
The app connects to the GemmaGuardian API server on port 8888. Make sure:

1. **GemmaGuardian server is running**:
   ```bash
   cd src/SurveillanceAgent
   python start_system.py --mode ollama  # or transformer
   ```

2. **Firewall allows port 8888**
3. **Network connectivity** between phone and server

### App Permissions
The app requires these permissions:
- **Internet**: Server communication
- **Notifications**: Security alerts
- **Storage**: Cache video thumbnails

## 🏗️ Development

### Project Structure
```
app/src/main/
├── java/com/GemmaGuardian/
│   ├── MainActivity.java          # Main app entry
│   ├── ApiService.java           # Server communication
│   ├── NotificationService.java  # Push notifications
│   └── CameraActivity.java       # Video display
├── res/
│   ├── layout/                   # UI layouts
│   ├── values/                   # Strings, colors, styles
│   └── drawable/                 # Images and icons
└── AndroidManifest.xml           # App configuration
```

### API Endpoints
The app communicates with these GemmaGuardian endpoints:
- `GET /api/status` - System status
- `GET /api/recent-analyses` - Recent detections
- `GET /api/live-feed` - Camera stream
- `POST /api/settings` - Update configuration

## 📝 Notes

- **Testing**: Use Android emulator for development
- **Performance**: Real device recommended for video streaming
- **Network**: Ensure stable WiFi/cellular connection
- **Security**: Consider VPN for remote access

For issues, check the main GemmaGuardian documentation in the root README.md
