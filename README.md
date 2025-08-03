# 🔒 GemmaGuardian AI Surveillance System

<div align="center">

![Python](https://img.shields.io/badge/python8+-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Status](https://img.shields.io/badge/status-active-brightgreen.svg)

**Transform any camera into an intelligent AI-powered surveillance system**

[🚀 Quick Start](#-quick-start) • [📖 Documentation](#-documentation) • [📱Mobile App](#-mobile-app) • [🤝 Contributing](#-contributing)

</div>

## 🌟 Overview

GemmaGuardian converts any RTSP camera into an intelligent surveillance system using Google's Gemma models. It eliminates false positives through AI-powered analysis and provides professional-grade threat assessment with mobile integration.

### 📚 Complete Guides
- **🚀 [Setup Guide](docs/SETUP_GUIDE.md)** - Complete installation and configuration
- **🏗️ [System Architecture](docs/ARCHITECTURE.md)** - Technical deep dive with official architecture diagram
- **🔧 [Network Setup](docs/NETWORK_SETUP.md)** - Firewall, ports, and network configuration

### Key Features

- **🧠 Dual AI Modes**: Ollama server-based or direct transformer processing
- **📹 Universal Camera Support**: Works with any RTSP-enabled camera
- **🚨 Intelligent Alerts**: Context-aware threat classification (Critical/High/Medium/Low)
- **📱 Mobile App**: Complete Android companion with live streaming
- **🔒 Privacy-First**: Local processing, no cloud dependency required
- **⚡ Real-time Processing**: <100ms person detection, 30-60s analysis

## 🏗️ System Architecture

![GemmaGuardian System Architecture](docs/GemmaGuardian.png)

*Complete GemmaGuardian system flow from RTSP input through AI processing to mobile notifications and storage.*

```mermaid
graph TD
    A[📹 RTSP Stream Input] --> B[🔍 Real-time Person Detection MobileNet SSD]
    B --> C{👤 Person Detected?}
    C -->|Yes Confidence > 0.5| D[📹 Record 60s HD Security Clip]
    C -->|No| B
    D --> E[🖼️ Extract Frames Every 2 Seconds]
    E --> F[📐 Resize to 1024x1024 Resolution]
    F --> G[📦 Group into Batches of 4 Frames]
    G --> H[🧠 AI Mode Selection]
    
    H --> I[🌐 Ollama Mode]
    H --> J[🔥 Transformer Mode]
    
    I --> I1[📡 Send to Ollama Server]
    I1 --> I2[🎯 Gemma 2 2B Vision Analysis]
    I2 --> I3[📝 Gemma 3n e2b Consolidation]
    I3 --> K[🛡️ Security Assessment]
    
    J --> J1[💾 Load Model to GPU/CPU]
    J1 --> J2[🚀 Direct PyTorch Inference]
    J2 --> J3[🔧 Memory-Optimized Processing]
    J3 --> K
    
    K --> L[📊 Threat Classification]
    L --> M{⚠️ Threat Level?}
    M -->|Critical/High| N[🚨 Immediate Alert]
    M -->|Medium/Low| O[📋 Activity Log]
    N --> P[📱 Mobile Notification]
    O --> P
    P --> Q[💾 Database Storage]
    Q --> R[📁 Archive & Cleanup]
    R --> B
    
    subgraph "🧠 AI Processing Comparison"
        AI1[Ollama: Server-based Client-Server]
        AI2[Transformer: Direct In-Process]
        AI3[Both: Anti-hallucination Validation]
        H --> AI1
        H --> AI2
        AI1 --> AI3
        AI2 --> AI3
    end
```

## 🚀 Quick Start

### ⭐ **One-Command Complete Setup** (Recommended)

**The `setup.py` script is your gateway to GemmaGuardian - it handles EVERYTHING automatically:**

```bash
python setup.py
```

**🎯 What this powerful script accomplishes:**
1. ✅ **Environment Setup**: Creates virtual environment and installs all dependencies
2. ✅ **Model Downloads**: Downloads MobileNet SSD and Gemma AI models automatically  
3. ✅ **AI Mode Configuration**: Guides you through Ollama or Transformer mode setup
4. ✅ **Network Configuration**: Sets up firewall rules for camera and mobile access
5. ✅ **System Testing**: Runs comprehensive tests to verify everything works
6. ✅ **Camera Setup**: Prompts for your RTSP URL and validates connection
7. ✅ **Auto-Launch**: Optionally starts the surveillance system immediately

**🚀 After running `setup.py`, your surveillance system will be fully operational!**

> **💡 Pro Tip**: The setup script includes intelligent error handling, GPU detection, and guided configuration - making professional surveillance setup accessible to everyone.

📖 **Detailed Setup Guide**: [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md)

### System Requirements

- **Python 3.8+**
- **8GB+ RAM** (16GB recommended)
- **RTSP Camera** (IP camera, NVR, or mobile app)
- **GPU** (optional, for Transformer mode acceleration)

## 📖 Documentation

### 📚 Complete Guides
- **� [Setup Guide](docs/SETUP_GUIDE.md)** - Complete installation and configuration
- **🏗️ [System Architecture](docs/ARCHITECTURE.md)** - Technical deep dive and system design
- **🔧 [Network Setup](docs/NETWORK_SETUP.md)** - Firewall, ports, and network configuration

### 🔌 Integration & Development
- **📡 [API Reference](docs/API_REFERENCE.md)** - Complete REST API documentation
- **📱 [Mobile App Setup](src/MobileApp/README.md)** - Android companion app guide

### ⚡ Quick Actions

#### Firewall Setup (Windows - Run as Administrator)
```powershell
# Automated firewall configuration
cd src/SurveillanceAgent
.\setup_firewall.ps1
```

#### Manual System Start
```bash
cd src/SurveillanceAgent
python main.py --mode ollama    # or --mode transformer
```

## 📱 Mobile App

Professional Android companion app with enterprise-grade features:

- **📹 Live Streaming**: Real-time RTSP feed viewing
- **🔔 Smart Notifications**: Threat-level based alerts only
- **📊 Analysis History**: Browse and search security events
- **⚙️ Remote Control**: Configure system from anywhere
- **🚨 Emergency Alerts**: Critical threat escalation

📱 **Mobile App Setup**: [src/MobileApp/README.md](src/MobileApp/README.md)

## 🔧 Essential Configuration

### 🔥 Automated Firewall Setup (Windows)
```powershell
# Run the provided PowerShell script as Administrator
cd src/SurveillanceAgent
.\setup_firewall.ps1
```

### 🌐 Network Configuration

**Manual Firewall Rules** (if not using automated script):
```powershell
# Windows PowerShell (Run as Administrator)
New-NetFirewallRule -DisplayName "GemmaGuardian API" -Direction Inbound -Protocol TCP -LocalPort 8888 -Action Allow
New-NetFirewallRule -DisplayName "RTSP Cameras" -Direction Inbound -Protocol TCP -LocalPort 554 -Action Allow
```

**Camera RTSP URLs**:
```bash
# Generic format
rtsp://username:password@camera-ip:554/stream

# Common examples
rtsp://admin:password@192.168.1.100:554/h264_stream     # Generic IP camera
rtsp://admin:password@192.168.1.100:554/cam/realmonitor # Dahua
rtsp://admin:password@192.168.1.100:554/Streaming/Channels/101  # Hikvision
```

**Environment Configuration** (`.env` file - created automatically by setup.py):
```bash
RTSP_URL=rtsp://admin:password@192.168.1.100:554/stream
AI_MODE=transformer  # or 'ollama'
DETECTION_CONFIDENCE_THRESHOLD=0.5
LOG_LEVEL=INFO
```

📋 **Complete Configuration Guide**: [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md)

## 🧪 Testing & Validation

```bash
# Quick system test
cd src/SurveillanceAgent
python test_setup.py

# AI model validation
python test_ai_flow.py

```

## 🤝 Contributing

We welcome contributions! Areas of focus:
- 🎯 New AI models and detection algorithms
- 📱 Mobile app enhancements
- 🔔 Additional notification channels
- 📊 Analytics and reporting features
- 🐳 Container and deployment improvements

**Development Setup**: [docs/SETUP_GUIDE.md#manual-installation-advanced-users](docs/SETUP_GUIDE.md#manual-installation-advanced-users)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Google**: Gemma model family
- **Ollama**: Model serving platform
---

**🔗 Links**: [GitHub](https://github.com/Cloud-Jas/GemmaGuardian) • [Issues](https://github.com/Cloud-Jas/GemmaGuardian/issues) • [Discussions](https://github.com/Cloud-Jas/GemmaGuardian/discussions)
