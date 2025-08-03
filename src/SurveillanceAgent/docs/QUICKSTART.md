# 🚀 Quick Start Guide

Your RTSP URL is configured: `rtsp://admin:admin@192.168.0.100:554/ch0_0.264`

## Step-by-Step Setup:

### 1. First Time Setup
```bash
# Install dependencies
./install.sh

# Test your setup
python3 test_setup.py
```

### 2. Install Ollama and AI Model
```bash
# Install Ollama (if not already installed)
curl -fsSL https://ollama.ai/install.sh | sh

# Pull the Gemma model
ollama pull gemma3n:e4b

# Start Ollama server (if not running)
ollama serve
```

### 3. Optional: Download Better AI Models
```bash
# For better person detection (optional)
python3 setup_models.py
```

### 4. Run the Security System
```bash
# Start the main security monitoring system
python3 main.py
```

## What Each Script Does:

| Script | Purpose |
|--------|---------|
| `install.sh` | Install Python dependencies |
| `test_setup.py` | **START HERE** - Test if everything works |
| `setup_models.py` | Download MobileNet models (optional) |
| `main.py` | **Main security system** - Run after setup |

## Expected Behavior:

1. **System connects** to your camera at `192.168.0.100`
2. **Detects persons** using AI
3. **Records 20-second clips** when person detected
4. **Analyzes clips** using Gemma 3 for security threats
5. **Logs everything** to console and files

## Troubleshooting:

**Camera not connecting?**
- Check if camera is accessible: `ping 192.168.0.100`
- Verify credentials: admin/admin
- Test RTSP with VLC: Open Network Stream → Your RTSP URL

**Ollama issues?**
- Check if running: `ollama list`
- Start server: `ollama serve`
- Pull model: `ollama pull gemma3n:e4b`

**Python errors?**
- Run: `python3 test_setup.py`
- Install missing packages: `pip3 install -r requirements.txt`

## Quick Test:
```bash
# Test everything at once
python3 start.py
```

Then follow the recommendations!
