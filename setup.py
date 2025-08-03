#!/usr/bin/env python3
"""
GemmaGuardian Unified Setup & Launch Script
Automatically sets up the environment, downloads models, and launches the system
"""

import os
import sys
import subprocess
import platform
import urllib.request
from pathlib import Path

def print_banner():
    """Print the setup banner"""
    print("=" * 70)
    print("🔒 GemmaGuardian AI Surveillance System - Complete Setup & Launch")
    print("=" * 70)
    print("This script will:")
    print("✅ Set up your environment")
    print("✅ Download all required models")
    print("✅ Configure your AI mode")
    print("✅ Launch the surveillance system")
    print("")
    print("Choose your AI mode:")
    print("1. 🌐 Ollama Mode (Server-based AI, production-ready)")
    print("2. 🔥 Transformer Mode (Direct GPU/CPU inference, edge deployment)")
    print("3. 🚀 Both Modes (Full setup)")
    print("4. 🚪 Exit")
    print("=" * 70)

def check_python_version():
    """Check if Python version is compatible"""
    if sys.version_info < (3, 8):
        print("❌ Error: Python 3.8 or higher is required")
        print(f"Current version: {sys.version}")
        sys.exit(1)
    print(f"✅ Python version: {sys.version.split()[0]}")

def check_system_requirements():
    """Check system requirements"""
    print("\n🔍 Checking system requirements...")
    
    # Check if we're in the right directory
    if not Path("src/SurveillanceAgent").exists():
        print("❌ Error: Please run this script from the GemmaGuardian root directory")
        sys.exit(1)
    
    # Check for virtual environment
    venv_path = Path("src/SurveillanceAgent/venv")
    if not venv_path.exists():
        print("⚠️ Virtual environment not found. Creating one...")
        create_virtual_environment()
    else:
        print("✅ Virtual environment found")

def create_virtual_environment():
    """Create virtual environment"""
    print("📦 Creating virtual environment...")
    venv_path = Path("src/SurveillanceAgent/venv")
    
    try:
        subprocess.run([sys.executable, "-m", "venv", str(venv_path)], check=True)
        print("✅ Virtual environment created successfully")
    except subprocess.CalledProcessError:
        print("❌ Failed to create virtual environment")
        sys.exit(1)

def get_user_choice():
    """Get user's choice for setup mode"""
    while True:
        print("\n🎯 Choose your setup mode:")
        print("1. Ollama Mode (Recommended for production)")
        print("2. Transformer Mode (Recommended for development/GPU)")
        print("3. Both Modes (Complete setup)")
        print("4. Exit")
        
        choice = input("\nEnter your choice (1-4): ").strip()
        
        if choice in ['1', '2', '3', '4']:
            return choice
        else:
            print("❌ Invalid choice. Please enter 1, 2, 3, or 4.")

def install_base_requirements():
    """Install base requirements"""
    print("\n📚 Installing base requirements...")
    
    # Determine Python executable in venv - using absolute paths
    if platform.system() == "Windows":
        python_exe = str(Path("src/SurveillanceAgent/venv/Scripts/python.exe").resolve())
        pip_exe = str(Path("src/SurveillanceAgent/venv/Scripts/pip.exe").resolve())
    else:
        python_exe = str(Path("src/SurveillanceAgent/venv/bin/python").resolve())
        pip_exe = str(Path("src/SurveillanceAgent/venv/bin/pip").resolve())
    
    try:
        # Upgrade pip first
        subprocess.run([pip_exe, "install", "--upgrade", "pip"], check=True)
        
        # Install base requirements
        subprocess.run([pip_exe, "install", "-r", "requirements.txt"], check=True)
        print("✅ Base requirements installed successfully")
        return python_exe, pip_exe
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to install base requirements: {e}")
        sys.exit(1)

def setup_transformer_mode(pip_exe):
    """Setup transformer mode dependencies"""
    print("\n🔥 Setting up Transformer Mode...")
    
    # Check for GPU
    try:
        import torch
        gpu_available = torch.cuda.is_available()
        if gpu_available:
            gpu_name = torch.cuda.get_device_name(0)
            print(f"🎮 GPU detected: {gpu_name}")
            use_gpu = input("Do you want to install GPU support? (y/N): ").lower().startswith('y')
        else:
            print("💻 No GPU detected, installing CPU version")
            use_gpu = False
    except ImportError:
        print("🤔 PyTorch not installed yet. Checking for GPU...")
        # Try to detect NVIDIA GPU
        try:
            result = subprocess.run(["nvidia-smi"], capture_output=True, text=True)
            if result.returncode == 0:
                print("🎮 NVIDIA GPU detected")
                use_gpu = input("Do you want to install GPU support? (y/N): ").lower().startswith('y')
            else:
                print("💻 No NVIDIA GPU detected, installing CPU version")
                use_gpu = False
        except:
            print("💻 Installing CPU version")
            use_gpu = False
    
    # Install transformer dependencies
    transformer_deps = [
        "transformers>=4.53.0",
        "timm>=1.0.16",
        "av>=14.4.0"
    ]
    
    if use_gpu:
        # Install GPU version
        torch_deps = [
            "torch>=2.0.0",
            "torchvision>=0.15.0",
            "--index-url", "https://download.pytorch.org/whl/cu118"
        ]
        print("🚀 Installing GPU-enabled PyTorch...")
        subprocess.run([pip_exe, "install"] + torch_deps, check=True)
    else:
        # Install CPU version
        torch_deps = [
            "torch>=2.0.0",
            "torchvision>=0.15.0"
        ]
        print("💻 Installing CPU-only PyTorch...")
        subprocess.run([pip_exe, "install"] + torch_deps, check=True)
    
    # Install other transformer dependencies
    subprocess.run([pip_exe, "install"] + transformer_deps, check=True)
    
    print("✅ Transformer mode setup completed")

def setup_ollama_mode():
    """Setup Ollama mode"""
    print("\n🌐 Setting up Ollama Mode...")
    
    # Check if Ollama is installed
    try:
        result = subprocess.run(["ollama", "--version"], capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ Ollama is already installed")
        else:
            print("⚠️ Ollama not found")
            install_ollama()
    except FileNotFoundError:
        print("⚠️ Ollama not found")
        install_ollama()
    
    print("✅ Ollama mode setup completed")

def install_ollama():
    """Install Ollama"""
    print("🔽 Installing Ollama...")
    system = platform.system()
    
    if system == "Windows":
        print("Please download and install Ollama from: https://ollama.ai/download")
        print("After installation, run this script again.")
        input("Press Enter after installing Ollama...")
    elif system == "Darwin":  # macOS
        try:
            subprocess.run(["brew", "install", "ollama"], check=True)
            print("✅ Ollama installed via Homebrew")
        except:
            print("Please install Ollama from: https://ollama.ai/download")
    else:  # Linux
        try:
            subprocess.run(["curl", "-fsSL", "https://ollama.ai/install.sh"], shell=True, check=True)
            print("✅ Ollama installed")
        except:
            print("Please install Ollama from: https://ollama.ai/download")

def setup_huggingface_auth(pip_exe, python_exe):
    """Setup Hugging Face authentication for transformer mode"""
    print("\n🤗 Setting up Hugging Face Authentication...")
    print("For transformer mode, you need a Hugging Face token to download Gemma models.")
    print("Get your token from: https://huggingface.co/settings/tokens")
    
    token = input("Enter your Hugging Face token (or press Enter to skip): ").strip()
    
    if token:
        try:
            # Install huggingface_hub if not already installed
            subprocess.run([pip_exe, "install", "huggingface_hub"], check=True)
            
            # Login using the token
            subprocess.run([python_exe, "-c", f"from huggingface_hub import login; login('{token}')"], check=True)
            print("✅ Hugging Face authentication successful")
        except subprocess.CalledProcessError:
            print("⚠️ Failed to authenticate with Hugging Face")
            print("You can authenticate later using: huggingface-cli login")
    else:
        print("⚠️ Skipped Hugging Face authentication")
        print("You can authenticate later using: huggingface-cli login")

def download_mobilenet_models():
    """Download MobileNet SSD models for person detection"""
    print("\n📥 Downloading MobileNet SSD models...")
    
    # Create models directory
    models_dir = Path("src/SurveillanceAgent/models")
    models_dir.mkdir(exist_ok=True)
    
    # Model URLs
    prototxt_url = "https://raw.githubusercontent.com/chuanqi305/MobileNet-SSD/master/deploy.prototxt"
    model_url = "https://raw.githubusercontent.com/chuanqi305/MobileNet-SSD/master/mobilenet_iter_73000.caffemodel"
    
    prototxt_path = models_dir / "MobileNetSSD_deploy.prototxt.txt"
    model_path = models_dir / "MobileNetSSD_deploy.caffemodel"
    
    try:
        # Download prototxt file
        if not prototxt_path.exists():
            print("Downloading MobileNet SSD prototxt...")
            urllib.request.urlretrieve(prototxt_url, prototxt_path)
            print("✅ Downloaded MobileNet SSD prototxt")
        else:
            print("✅ MobileNet SSD prototxt already exists")
        
        # Download model file
        if not model_path.exists():
            print("Downloading MobileNet SSD model file (this may take a while)...")
            try:
                urllib.request.urlretrieve(model_url, model_path)
                print("✅ Downloaded MobileNet SSD model file")
            except Exception as download_error:
                print(f"⚠️ Failed to download model file: {download_error}")
                print("The system will fall back to Haar Cascade detection")
        else:
            print("✅ MobileNet SSD model already exists")
            
    except Exception as e:
        print(f"⚠️ Error downloading MobileNet models: {e}")
        print("The system will fall back to Haar Cascade detection")

def setup_ollama_models():
    """Setup Ollama models"""
    print("\n🦙 Setting up Ollama models...")
    
    # Check if Ollama server is running
    try:
        import requests
        response = requests.get("http://localhost:11434/api/version", timeout=5)
        if response.status_code != 200:
            print("⚠️ Ollama server not running. Starting Ollama...")
            # Try to start Ollama in background
            if platform.system() == "Windows":
                subprocess.Popen(["ollama", "serve"], creationflags=subprocess.CREATE_NEW_CONSOLE)
            else:
                subprocess.Popen(["ollama", "serve"])
            print("Waiting for Ollama server to start...")
            import time
            time.sleep(5)
    except:
        print("⚠️ Could not check Ollama server status")
    
    # Download required models (matching .env configuration)
    models = ["gemma3:4b"]  # Both OLLAMA_MODEL and OLLAMA_TEXT_MODEL use gemma3:4b
    
    for model in models:
        print(f"📥 Downloading {model}...")
        try:
            # Don't capture output to avoid Unicode issues, just check return code
            result = subprocess.run(["ollama", "pull", model], timeout=600)
            if result.returncode == 0:
                print(f"✅ {model} downloaded successfully")
            else:
                print(f"⚠️ Failed to download {model} (return code: {result.returncode})")
        except subprocess.TimeoutExpired:
            print(f"⚠️ Timeout downloading {model} (this can happen with large models)")
        except FileNotFoundError:
            print("⚠️ Ollama not found. Please install Ollama first.")
            break
        except Exception as e:
            print(f"⚠️ Error downloading {model}: {e}")

def create_env_file():
    """Create .env file if it doesn't exist"""
    env_path = Path("src/SurveillanceAgent/.env")
    
    if not env_path.exists():
        print("\n📝 Creating configuration file...")
        
        env_content = """# GemmaGuardian Configuration

# AI Mode Selection (ollama or transformer)
AI_MODE=ollama

# RTSP Camera Configuration
RTSP_URL=rtsp://admin:password@192.168.1.100:554/stream

# Recording Settings
CLIP_DURATION=60
CLIP_OUTPUT_DIR=./recordings

# Ollama Configuration
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=gemma3:4b
CONSOLIDATION_MODEL=gemma-3n-e2b-it

# Transformer Configuration
TRANSFORMER_MODEL=google/gemma-3n-e2b-it
TRANSFORMER_DEVICE=auto
TRANSFORMER_RESOLUTION=512

# Detection Settings
DETECTION_CONFIDENCE_THRESHOLD=0.5
THREAT_CONFIDENCE_THRESHOLD=0.7

# System Settings
LOG_LEVEL=INFO
MAX_WORKERS=4
TIMEOUT_SECONDS=180
"""
        
        env_path.write_text(env_content)
        print("✅ Configuration file created at src/SurveillanceAgent/.env")
        print("📝 Please edit this file with your camera settings")

def run_tests(python_exe):
    """Run system tests"""
    print("\n🧪 Running system tests...")
    
    try:
        result = subprocess.run([python_exe, "test_setup.py"], 
                              capture_output=True, text=True,
                              cwd="src/SurveillanceAgent")
        print(result.stdout)
        if result.returncode == 0:
            print("✅ All tests passed!")
        else:
            print("⚠️ Some tests failed. Please check the output above.")
    except FileNotFoundError:
        print("⚠️ Test file not found. Skipping tests.")

def launch_surveillance_system(mode, python_exe):
    """Launch the surveillance system"""
    print(f"\n🚀 Launching GemmaGuardian in {mode} mode...")
    
    # Change to the surveillance directory
    surveillance_dir = Path("src/SurveillanceAgent")
    
    # Ask user for RTSP URL
    print("\n📹 Camera Configuration")
    rtsp_url = input("Enter your RTSP camera URL (or press Enter for demo): ").strip()
    
    if rtsp_url:
        # Update .env file with user's RTSP URL
        env_path = surveillance_dir / ".env"
        if env_path.exists():
            with open(env_path, 'r') as f:
                content = f.read()
            
            # Replace RTSP_URL in the content
            import re
            content = re.sub(r'RTSP_URL=.*', f'RTSP_URL={rtsp_url}', content)
            content = re.sub(r'AI_MODE=.*', f'AI_MODE={mode}', content)
            
            with open(env_path, 'w') as f:
                f.write(content)
            print(f"✅ Updated configuration with your RTSP URL")
    
    try:
        # Launch the system
        launch_cmd = [python_exe, "main.py", "--mode", mode]
        
        print(f"Executing: {' '.join(launch_cmd)}")
        print("=" * 70)
        print("🔒 GemmaGuardian AI Surveillance System Starting...")
        print("=" * 70)
        
        # Change to surveillance directory and run
        subprocess.run(launch_cmd, cwd=str(surveillance_dir), check=True)
        
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to launch surveillance system: {e}")
        print("You can start manually with:")
        print(f"cd src/SurveillanceAgent && python main.py --mode {mode}")
    except KeyboardInterrupt:
        print("\n👋 Surveillance system stopped by user")

def main():
    """Main setup function"""
    print_banner()
    check_python_version()
    check_system_requirements()
    
    choice = get_user_choice()
    
    if choice == '4':
        print("👋 Setup cancelled")
        return
    
    # Install base requirements
    python_exe, pip_exe = install_base_requirements()
    
    # Download MobileNet models (needed for both modes)
    download_mobilenet_models()
    
    # Setup based on choice
    selected_mode = None
    if choice in ['1', '3']:  # Ollama or Both
        setup_ollama_mode()
        setup_ollama_models()
        selected_mode = 'ollama'
    
    if choice in ['2', '3']:  # Transformer or Both
        setup_transformer_mode(pip_exe)
        setup_huggingface_auth(pip_exe, python_exe)
        selected_mode = 'transformer'
    
    # For both modes setup, ask which to launch
    if choice == '3':
        print("\n🚀 Both modes are set up! Which mode would you like to launch?")
        print("1. 🌐 Ollama Mode")
        print("2. 🔥 Transformer Mode")
        launch_choice = input("Enter your choice (1-2): ").strip()
        
        if launch_choice == '1':
            selected_mode = 'ollama'
        elif launch_choice == '2':
            selected_mode = 'transformer'
        else:
            print("⚠️ Invalid choice. You can launch manually later.")
            selected_mode = None
    
    # Create configuration file
    create_env_file()
    
    # Run tests
    run_tests(python_exe)
    
    print("\n" + "=" * 70)
    print("🎉 Setup completed successfully!")
    print("=" * 70)
    
    if selected_mode:
        # Ask if user wants to launch immediately
        launch_now = input(f"\n🚀 Launch GemmaGuardian in {selected_mode} mode now? (y/N): ").strip().lower()
        
        if launch_now in ['y', 'yes']:
            launch_surveillance_system(selected_mode, python_exe)
        else:
            print("Manual launch commands:")
            print(f"cd src/SurveillanceAgent")
            print(f"python main.py --mode {selected_mode}")
    else:
        print("Manual launch commands:")
        print("cd src/SurveillanceAgent")
        if choice in ['1', '3']:
            print("python main.py --mode ollama")
        if choice in ['2', '3']:
            print("python main.py --mode transformer")
    
    print("=" * 70)

if __name__ == "__main__":
    main()
