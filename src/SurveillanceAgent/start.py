#!/usr/bin/env python3
"""Simple startup script for the security monitoring system."""

import sys
import os
from pathlib import Path

# Add project root to path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

def print_banner():
    """Print startup banner."""
    print("🔒 Security Monitoring System")
    print("=" * 40)
    print("1. setup_models.py  - Download AI models (optional)")
    print("2. test_setup.py    - Test your installation")
    print("3. main.py          - Run the security system")
    print("=" * 40)

def check_env_file():
    """Check if .env file exists and is configured."""
    env_file = Path(".env")
    if not env_file.exists():
        print("⚠️ .env file not found. Creating from template...")
        import shutil
        shutil.copy(".env.example", ".env")
        print("✅ Created .env file")
        return False
    return True

def update_rtsp_url():
    """Update the RTSP URL in .env file."""
    rtsp_url = "rtsp://admin:admin@192.168.0.100:554/ch0_0.264"
    
    try:
        # Read current .env file
        with open(".env", "r") as f:
            lines = f.readlines()
        
        # Update RTSP_URL line
        updated = False
        for i, line in enumerate(lines):
            if line.startswith("RTSP_URL="):
                lines[i] = f"RTSP_URL={rtsp_url}\n"
                updated = True
                break
        
        # If RTSP_URL not found, add it
        if not updated:
            lines.append(f"RTSP_URL={rtsp_url}\n")
        
        # Write back to file
        with open(".env", "w") as f:
            f.writelines(lines)
        
        print(f"✅ Updated RTSP URL to: {rtsp_url}")
        return True
        
    except Exception as e:
        print(f"❌ Error updating RTSP URL: {e}")
        return False

def main():
    """Main startup function."""
    print_banner()
    
    # Check and create .env file
    env_exists = check_env_file()
    
    # Update RTSP URL
    update_rtsp_url()
    
    print("\n🚀 Quick Start Options:")
    print("1. Test everything is working:")
    print("   python3 test_setup.py")
    print()
    print("2. Download AI models (optional, for better detection):")
    print("   python3 setup_models.py")
    print()
    print("3. Start the security system:")
    print("   python3 main.py")
    print()
    print("💡 Recommendation: Run test_setup.py first!")

if __name__ == "__main__":
    main()
