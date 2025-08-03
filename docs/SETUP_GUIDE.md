# 🔒 GemmaGuardian Setup Guide

Complete installation and configuration guide for the GemmaGuardian AI Surveillance System.

## 🏗️ Understanding the System

Before setup, review our system architecture to understand how GemmaGuardian works:

📊 **[System Architecture with Official Diagram](ARCHITECTURE.md)** - See the complete technical flow from camera input to mobile notifications.

## 🚀 Quick Setup (Recommended)

**The easiest way to get started is using the automated setup script:**

```bash
python setup.py
```

### What the Setup Script Does:

1. **Environment Setup**: Creates virtual environment and installs all dependencies
2. **Model Downloads**: 
   - Downloads MobileNet SSD models via `download_mobilenet_models()`
   - Downloads Gemma models via `setup_ollama_models()` (Ollama mode)
   - Handles Hugging Face authentication for Transformer mode
3. **AI Mode Configuration**: Configures Ollama or Transformer mode based on your choice
4. **Firewall Configuration**: Can optionally configure Windows firewall rules
5. **System Testing**: Runs comprehensive tests to verify installation
6. **Ready to Launch**: Optionally launches the surveillance system immediately

**This single command handles everything - no manual configuration needed!**

## 📋 System Requirements

### Hardware Requirements
- **CPU**: Multi-core processor (Intel i5/AMD Ryzen 5 or better)
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB+ free space for recordings and models
- **Camera**: Any RTSP-enabled IP camera, NVR, or mobile app
- **GPU**: Optional (NVIDIA GPU for Transformer mode acceleration)

### Software Requirements
- **Python 3.8+**
- **Windows 10/11, macOS 10.15+, or Linux Ubuntu 18.04+**
- **Network**: Internet connection for initial setup and Ollama mode

## 🛠️ Manual Installation (Advanced Users)

### Step 1: Clone Repository
```bash
git clone https://github.com/Cloud-Jas/GemmaGuardian.git
cd GemmaGuardian
```

### Step 2: Virtual Environment
```bash
# Create virtual environment
python -m venv src/SurveillanceAgent/venv

# Activate (Windows)
src\SurveillanceAgent\venv\Scripts\activate

# Activate (Linux/macOS)
source src/SurveillanceAgent/venv/bin/activate
```

### Step 3: Install Dependencies
```bash
cd src/SurveillanceAgent
pip install --upgrade pip
pip install -r requirements.txt
```

### Step 4: AI Mode Setup

#### Option A: Ollama Mode (Recommended for Production)
```bash
# Install Ollama
# Windows: Download from https://ollama.ai/download
# macOS: brew install ollama
# Linux: curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama server
ollama serve

# Download models
ollama pull gemma3:4b
```

#### Option B: Transformer Mode (GPU/Local Processing)
```bash
# Install PyTorch with GPU support
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118

# Install transformer dependencies
pip install transformers timm av

# Login to Hugging Face (for Gemma model access)
huggingface-cli login
```

### Step 5: Configuration
```bash
# Copy example configuration
cp .env.example .env

# Edit configuration
nano .env  # or use your preferred editor
```

**Note**: Model downloads are automatically handled by the `setup.py` script:
- **MobileNet SSD models**: Downloaded automatically for person detection
- **Gemma AI models**: Downloaded based on selected AI mode (Ollama/Transformer)
- **Fallback behavior**: If MobileNet models aren't available, system automatically falls back to Haar Cascade detection

## ⚙️ Configuration

### Environment Variables (.env file)

```bash
# AI Mode Selection
AI_MODE=ollama  # or 'transformer'

# Camera Configuration
RTSP_URL=rtsp://admin:password@192.168.1.100:554/stream

# Ollama Settings
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=gemma3:4b

# Transformer Settings
TRANSFORMER_MODEL=google/gemma-3n-e2b-it
TRANSFORMER_DEVICE=auto

# Detection Settings
DETECTION_CONFIDENCE_THRESHOLD=0.5
THREAT_CONFIDENCE_THRESHOLD=0.7

# System Settings
LOG_LEVEL=INFO
CLIP_DURATION=60
MAX_WORKERS=4
```

### Common RTSP URL Formats

```bash
# Generic format
rtsp://username:password@camera-ip:554/stream

# Common brands
# Hikvision
rtsp://admin:password@192.168.1.100:554/Streaming/Channels/101

# Dahua
rtsp://admin:password@192.168.1.100:554/cam/realmonitor?channel=1&subtype=0

# Axis
rtsp://root:password@192.168.1.100/axis-media/media.amp

# Generic IP cameras
rtsp://admin:password@192.168.1.100:554/h264_stream
```

## 🔥 Firewall Configuration

### Automated Firewall Setup (Windows)
```powershell
# Run the provided PowerShell script as Administrator
cd src/SurveillanceAgent
.\setup_firewall.ps1
```

### Manual Firewall Rules

#### Windows PowerShell (Run as Administrator)
```powershell
# Allow GemmaGuardian API port for mobile app
New-NetFirewallRule -DisplayName "GemmaGuardian API" -Direction Inbound -Protocol TCP -LocalPort 8888 -Action Allow

# Allow RTSP camera access
New-NetFirewallRule -DisplayName "RTSP Cameras" -Direction Inbound -Protocol TCP -LocalPort 554 -Action Allow

# Allow Ollama server (if using Ollama mode)
New-NetFirewallRule -DisplayName "Ollama Server" -Direction Inbound -Protocol TCP -LocalPort 11434 -Action Allow
```

#### Linux (Ubuntu/Debian)
```bash
sudo ufw allow 8888/tcp comment "GemmaGuardian API"
sudo ufw allow 554/tcp comment "RTSP Cameras"
sudo ufw allow 11434/tcp comment "Ollama Server"
sudo ufw reload
```

#### macOS
```bash
# Add rules to pfctl or use third-party firewall
# Typically, built-in firewall allows outbound connections by default
```

## 🧪 Testing & Verification

### Quick System Test
```bash
cd src/SurveillanceAgent
python test_setup.py
```

### Individual Component Tests
```bash
# Test AI models
python test_ai_flow.py

# Test camera connection
python test_camera.py

# Test GPU (if using Transformer mode)
python test_gpu_usage.py
```

### Expected Test Results
- ✅ Python environment and dependencies
- ✅ AI models (Ollama/Transformer) accessibility
- ✅ Camera RTSP connection
- ✅ Person detection models
- ✅ File system permissions
- ✅ Database connectivity

## 🚀 Running the System

### Start Surveillance System
```bash
cd src/SurveillanceAgent
python main.py --mode ollama    # or --mode transformer
```

### Launch with Setup Script
```bash
python setup.py
# Follow prompts to configure and launch
```

### Background/Service Mode
```bash
# Linux/macOS (using nohup)
nohup python main.py --mode ollama > surveillance.log 2>&1 &

# Windows (using pythonw for background)
start /B pythonw main.py --mode ollama
```

## 📱 Mobile App Setup

### Android App Installation
1. **Build APK**: Open `src/MobileApp` in Android Studio
2. **Configure Server**: Edit server IP in app settings
3. **Install**: Transfer APK to Android device
4. **Connect**: Ensure device on same network as surveillance system

### Network Requirements
- **Same Network**: Mobile device and server on same WiFi/LAN
- **Port 8888**: Must be accessible from mobile device
- **Firewall**: Allow port 8888 through firewall

## 🔧 Troubleshooting

### Common Issues

#### "Camera Connection Failed"
```bash
# Test RTSP URL directly
ffplay rtsp://your-camera-url

# Check camera accessibility
ping camera-ip-address
telnet camera-ip-address 554
```

#### "Ollama Server Not Found"
```bash
# Check if Ollama is running
curl http://localhost:11434/api/version

# Start Ollama server
ollama serve

# Check process
ps aux | grep ollama  # Linux/macOS
tasklist | findstr ollama  # Windows
```

#### "GPU Not Detected" (Transformer Mode)
```bash
# Check CUDA installation
nvidia-smi

# Verify PyTorch GPU support
python -c "import torch; print(torch.cuda.is_available())"

# Install CUDA-enabled PyTorch
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118
```

#### "Permission Denied" Errors
```bash
# Linux/macOS: Fix permissions
chmod +x src/SurveillanceAgent/*.py
sudo chown -R $USER:$USER src/SurveillanceAgent/

# Windows: Run as Administrator or check antivirus
```

### Log Analysis
```bash
# View real-time logs
tail -f src/SurveillanceAgent/logs/security_monitor_$(date +%Y-%m-%d).log

# Search for errors
grep -i error src/SurveillanceAgent/logs/*.log
grep -i exception src/SurveillanceAgent/logs/*.log
```

### System Resource Monitoring
```bash
# Monitor CPU/Memory usage
top    # Linux/macOS
htop   # Enhanced version

# Monitor GPU usage (if applicable)
nvidia-smi -l 1

# Check disk space
df -h  # Linux/macOS
dir    # Windows
```

## 🔄 Updates & Maintenance

### Updating GemmaGuardian
```bash
# Pull latest changes
git pull origin main

# Update dependencies
pip install -r requirements.txt --upgrade

# Re-run setup if needed
python setup.py
```

### Model Updates
```bash
# Update Ollama models
ollama pull gemma3:4b

# Update Transformer models (cache will refresh automatically)
# Or clear cache: rm -rf ~/.cache/huggingface/transformers/
```

### Database Maintenance
```bash
# Check database size
ls -lh src/SurveillanceAgent/data/security_monitor.db

# Backup database
cp src/SurveillanceAgent/data/security_monitor.db backup_$(date +%Y%m%d).db
```

## 🎯 Quick Reference

### Essential Commands
```bash
# Complete setup and launch
python setup.py

# Manual start
cd src/SurveillanceAgent
python main.py --mode ollama

# Run tests
python test_setup.py

# Configure firewall (Windows)
.\setup_firewall.ps1

# View logs
tail -f logs/security_monitor_*.log
```

### Important Files
- `setup.py` - Main setup and launch script
- `src/SurveillanceAgent/.env` - Configuration file
- `src/SurveillanceAgent/main.py` - Surveillance system entry point
- `src/SurveillanceAgent/setup_firewall.ps1` - Windows firewall configuration
- `src/MobileApp/` - Android companion app source

### Network Ports
- **8888** - Mobile app API server
- **554** - RTSP camera streams
- **11434** - Ollama server (if using Ollama mode)

---

**💡 Pro Tip**: Always use `python setup.py` for the smoothest experience - it handles everything automatically and provides guided setup with error checking!
