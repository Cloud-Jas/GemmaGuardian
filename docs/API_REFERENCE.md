# 📡 GemmaGuardian API Documentation

Complete API reference for integrating with the GemmaGuardian surveillance system.

## 🌐 REST API Overview

The GemmaGuardian system provides a comprehensive REST API for mobile app integration and external system connectivity. The API server runs on port 8888 by default.

**Base URL**: `http://your-server-ip:8888/api`

## 🔐 Authentication

Currently, the API uses network-based security (same network requirement). Future versions will include token-based authentication.

## 📚 API Endpoints

### System Status

#### GET /api/status
Get current system health and statistics.

**Response:**
```json
{
  "status": "running",
  "mode": "ollama",
  "uptime": "2h 45m 32s",
  "cameras_connected": 1,
  "recent_detections": 3,
  "system_load": {
    "cpu": 25.4,
    "memory": 45.2,
    "disk": 78.1
  }
}
```

#### GET /api/system-info
Get detailed system information.

**Response:**
```json
{
  "version": "1.0.0",
  "python_version": "3.11.5",
  "ai_mode": "ollama",
  "models_loaded": ["gemma3:4b"],
  "gpu_available": false,
  "camera_status": "connected",
  "database_size": "45.2 MB"
}
```

### Security Analysis

#### GET /api/recent-analyses
Get recent security analyses.

**Query Parameters:**
- `limit` (optional): Number of results (default: 10, max: 100)
- `threat_level` (optional): Filter by threat level (low, medium, high, critical)

**Response:**
```json
{
  "analyses": [
    {
      "id": "analysis_20240729_185855_457",
      "timestamp": "2024-07-29T18:58:55Z",
      "threat_level": "high",
      "confidence": 0.89,
      "description": "Individual observed in restricted access area...",
      "keywords": ["person", "restricted", "loitering"],
      "video_file": "security_clip_20240729_185725_123.mp4",
      "frames_analyzed": 31
    }
  ],
  "total": 1,
  "page": 1
}
```

#### GET /api/threat-history
Get historical threat data for analytics.

**Query Parameters:**
- `days` (optional): Number of days to look back (default: 7, max: 30)
- `group_by` (optional): Group by hour/day/week (default: day)

**Response:**
```json
{
  "period": "7 days",
  "summary": {
    "total_detections": 45,
    "critical_threats": 2,
    "high_threats": 8,
    "medium_threats": 15,
    "low_threats": 20
  },
  "timeline": [
    {
      "date": "2024-07-29",
      "detections": 7,
      "threat_distribution": {
        "critical": 0,
        "high": 2,
        "medium": 3,
        "low": 2
      }
    }
  ]
}
```

### Live Streaming

#### GET /api/live-feed
Get live camera feed proxy.

**Response:** MJPEG stream or WebRTC (depending on implementation)

**Note:** This endpoint provides a proxy to the RTSP stream suitable for web/mobile consumption.

### Video Management

#### GET /api/recordings
List available video recordings.

**Query Parameters:**
- `limit` (optional): Number of results (default: 20)
- `date` (optional): Filter by date (YYYY-MM-DD)

**Response:**
```json
{
  "recordings": [
    {
      "filename": "security_clip_20240729_185725_123.mp4",
      "timestamp": "2024-07-29T18:57:25Z",
      "duration": 60,
      "size": "125.4 MB",
      "analysis_id": "analysis_20240729_185855_457",
      "person_detected": true
    }
  ],
  "total": 15
}
```

#### GET /api/recordings/{filename}
Download specific video file.

**Response:** Video file (MP4 format)

### Configuration

#### GET /api/settings
Get current system configuration.

**Response:**
```json
{
  "ai_mode": "ollama",
  "detection_threshold": 0.5,
  "threat_threshold": 0.7,
  "clip_duration": 60,
  "log_level": "INFO",
  "camera_url": "rtsp://***masked***"
}
```

#### POST /api/settings
Update system configuration.

**Request Body:**
```json
{
  "detection_threshold": 0.6,
  "threat_threshold": 0.8,
  "log_level": "DEBUG"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Settings updated successfully",
  "restart_required": false
}
```

### Notifications

#### POST /api/notification-test
Test notification system.

**Request Body:**
```json
{
  "message": "Test notification",
  "threat_level": "medium"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Test notification sent successfully"
}
```

## 🔌 WebSocket Events (Future)

The following WebSocket events are planned for real-time updates:

### Connection
```javascript
const ws = new WebSocket('ws://your-server-ip:8888/ws');
```

### Events
- `person_detected` - Real-time person detection
- `analysis_complete` - AI analysis completion
- `threat_alert` - High/Critical threat alerts
- `system_status` - System health updates

## 📱 Mobile App Integration

### Android App Communication

The Android app uses the following endpoints primarily:

1. **Status Monitoring**: `/api/status` - Real-time system health
2. **Live Viewing**: `/api/live-feed` - Camera stream proxy
3. **Alert History**: `/api/recent-analyses` - Browse past detections
4. **Settings**: `/api/settings` - Remote configuration
5. **Notifications**: Push notifications via FCM (future)

### Example Mobile Integration

```java
// Android example - Check system status
public void checkSystemStatus() {
    String url = "http://" + serverIP + ":8888/api/status";
    
    JsonObjectRequest request = new JsonObjectRequest(
        Request.Method.GET, url, null,
        response -> {
            String status = response.getString("status");
            updateUI(status);
        },
        error -> {
            Log.e("API", "Status check failed: " + error.getMessage());
        }
    );
    
    requestQueue.add(request);
}
```

## 🔗 Third-Party Integration

### Webhook Support (Future)

Configure webhooks for external system integration:

```json
{
  "webhook_url": "https://your-system.com/api/security-alert",
  "events": ["threat_detected", "system_error"],
  "auth_header": "Bearer your-token-here"
}
```

### MQTT Support (Future)

Publish events to MQTT broker:

```json
{
  "mqtt_broker": "mqtt://your-broker:1883",
  "topics": {
    "detections": "GemmaGuardian/detections",
    "threats": "GemmaGuardian/threats",
    "status": "GemmaGuardian/status"
  }
}
```

## 🛠️ Development

### Local API Testing

```bash
# Start the API server
cd src/SurveillanceAgent
python main.py --mode ollama

# Test endpoints with curl
curl http://localhost:8888/api/status
curl http://localhost:8888/api/recent-analyses?limit=5
```

### API Client Libraries

#### Python Client Example
```python
import requests

class GemmaGuardianClient:
    def __init__(self, base_url):
        self.base_url = base_url
    
    def get_status(self):
        response = requests.get(f"{self.base_url}/api/status")
        return response.json()
    
    def get_recent_analyses(self, limit=10):
        response = requests.get(f"{self.base_url}/api/recent-analyses?limit={limit}")
        return response.json()
```

#### JavaScript Client Example
```javascript
class GemmaGuardianClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    async getStatus() {
        const response = await fetch(`${this.baseUrl}/api/status`);
        return response.json();
    }
    
    async getRecentAnalyses(limit = 10) {
        const response = await fetch(`${this.baseUrl}/api/recent-analyses?limit=${limit}`);
        return response.json();
    }
}
```

## 📊 Performance Considerations

### Rate Limiting
- Current: No rate limiting implemented
- Planned: 100 requests per minute per IP

### Caching
- Status data: Cached for 5 seconds
- Analysis data: No caching (real-time)
- Static files: Browser caching enabled

### Optimization Tips
- Use appropriate `limit` parameters for large datasets
- Cache responses on client side when appropriate
- Consider WebSocket for real-time updates (when available)

## 🔒 Security Considerations

### Current Security Model
- Network-based security (same network requirement)
- No sensitive data exposure in responses
- Camera credentials masked in configuration responses

### Future Security Enhancements
- Token-based authentication
- HTTPS/TLS encryption
- Role-based access control
- API key management

## 🐛 Error Handling

### Standard Error Response
```json
{
  "error": true,
  "message": "Error description",
  "code": "ERROR_CODE",
  "details": {
    "field": "Additional error details"
  }
}
```

### Common Error Codes
- `CAMERA_DISCONNECTED` - Camera connection lost
- `AI_SERVICE_UNAVAILABLE` - AI processing service down
- `INVALID_PARAMETERS` - Invalid request parameters
- `RESOURCE_NOT_FOUND` - Requested resource not found
- `INTERNAL_ERROR` - Server internal error

### HTTP Status Codes
- `200` - Success
- `400` - Bad Request
- `404` - Not Found
- `500` - Internal Server Error
- `503` - Service Unavailable

---

## 🎯 Quick Reference

### Essential Endpoints
```bash
# System health
GET /api/status

# Recent security events
GET /api/recent-analyses

# Live camera feed
GET /api/live-feed

# System configuration
GET /api/settings
POST /api/settings
```

### Testing Commands
```bash
# Test API availability
curl http://localhost:8888/api/status

# Get recent analyses
curl "http://localhost:8888/api/recent-analyses?limit=5"

# Test with authentication (future)
curl -H "Authorization: Bearer token" http://localhost:8888/api/status
```

---

**💡 Pro Tip**: The API is designed for mobile app integration but can be used by any system that needs to interact with the surveillance system. Always check the `/api/status` endpoint first to verify connectivity!
