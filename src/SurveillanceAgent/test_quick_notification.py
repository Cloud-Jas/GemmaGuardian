#!/usr/bin/env python3
"""
Quick test script to send a critical notification immediately.
Use this for quick testing of the emergency notification system.
"""

import asyncio
import sys
from datetime import datetime, timedelta
from pathlib import Path

# Add the current directory to Python path
sys.path.append(str(Path(__file__).parent))

try:
    from modules.domain.entities import (
        SecurityAnalysis, SecurityThreatLevel, VideoClip, 
        PersonDetection, BoundingBox
    )
    from modules.infrastructure.mobile_notifications import MobileNotificationService
except ImportError as e:
    print(f"❌ Import error: {e}")
    print("Make sure you're running this from the SurveillanceAgent directory")
    sys.exit(1)


async def send_critical_alert():
    """Send a critical security alert immediately."""
    print("🚨 Sending CRITICAL security alert...")
    
    # Initialize notification service
    notification_service = MobileNotificationService(
        min_threat_level=SecurityThreatLevel.LOW  # Allow all notifications
    )
    
    # Create test data
    bbox = BoundingBox(x=150, y=200, width=180, height=280, confidence=0.92)
    detection = PersonDetection(
        timestamp=datetime.now(),
        bounding_box=bbox,
        frame_number=150,
        confidence=0.92
    )
    
    video_clip = VideoClip(
        file_path="recordings/critical_alert_test.mp4",
        start_time=datetime.now() - timedelta(seconds=25),
        duration=25.0,
        frame_count=750,
        resolution=(1920, 1080),
        trigger_detection=detection
    )
    
    # Create critical security analysis
    analysis = SecurityAnalysis(
        video_clip=video_clip,
        analysis_text="""
        🚨 CRITICAL SECURITY EMERGENCY 🚨
        
        IMMEDIATE THREAT DETECTED:
        • Armed intruder breaching main perimeter
        • Multiple unauthorized individuals on property
        • Security system compromised
        • Weapons detected in surveillance footage
        
        ⚠️ EMERGENCY RESPONSE REQUIRED IMMEDIATELY ⚠️
        
        This is a test of the emergency notification system.
        """,
        threat_level=SecurityThreatLevel.CRITICAL,
        confidence=0.95,
        keywords=["intruder", "armed", "breach", "emergency", "weapons", "critical"],
        timestamp=datetime.now()
    )
    
    # Send the alert
    try:
        success = await notification_service.send_security_alert(
            analysis, 
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            print("✅ CRITICAL alert sent successfully!")
            print("📱 Check your mobile app for the emergency notification")
        else:
            print("❌ Failed to send CRITICAL alert")
            print("🔧 Check your network connection and app settings")
            
        return success
        
    except Exception as e:
        print(f"❌ Error sending alert: {e}")
        return False


async def send_high_alert():
    """Send a high priority security alert."""
    print("⚠️ Sending HIGH priority alert...")
    
    notification_service = MobileNotificationService(
        min_threat_level=SecurityThreatLevel.LOW
    )
    
    bbox = BoundingBox(x=120, y=180, width=200, height=300, confidence=0.87)
    detection = PersonDetection(
        timestamp=datetime.now(),
        bounding_box=bbox,
        frame_number=200,
        confidence=0.87
    )
    
    video_clip = VideoClip(
        file_path="recordings/high_alert_test.mp4",
        start_time=datetime.now() - timedelta(seconds=30),
        duration=30.0,
        frame_count=900,
        resolution=(1920, 1080),
        trigger_detection=detection
    )
    
    analysis = SecurityAnalysis(
        video_clip=video_clip,
        analysis_text="""
        ⚠️ HIGH PRIORITY SECURITY ALERT ⚠️
        
        SUSPICIOUS ACTIVITY DETECTED:
        • Unauthorized person attempting entry
        • Individual trying door handles and windows
        • Suspicious behavior near property
        • Potential break-in attempt in progress
        
        This is a test of the high priority notification system.
        """,
        threat_level=SecurityThreatLevel.HIGH,
        confidence=0.87,
        keywords=["suspicious", "unauthorized", "break-in", "entry", "alert"],
        timestamp=datetime.now()
    )
    
    try:
        success = await notification_service.send_security_alert(
            analysis,
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            print("✅ HIGH priority alert sent successfully!")
        else:
            print("❌ Failed to send HIGH priority alert")
            
        return success
        
    except Exception as e:
        print(f"❌ Error sending alert: {e}")
        return False


async def main():
    """Main function with simple menu."""
    print("🔔 Quick Notification Test")
    print("=" * 30)
    print("1. Send CRITICAL alert (Emergency)")
    print("2. Send HIGH priority alert")
    print("3. Send both alerts")
    
    try:
        choice = input("\nEnter choice (1-3): ").strip()
    except KeyboardInterrupt:
        print("\n👋 Cancelled")
        return
    
    if choice == "1":
        await send_critical_alert()
    elif choice == "2":
        await send_high_alert()
    elif choice == "3":
        print("📡 Sending CRITICAL alert first...")
        await send_critical_alert()
        print("\n⏱️ Waiting 3 seconds...")
        await asyncio.sleep(3)
        print("📡 Sending HIGH priority alert...")
        await send_high_alert()
    else:
        print("❌ Invalid choice")
        return
    
    print("\n🏁 Test completed!")
    print("📱 Check your mobile app to see if notifications were received")


if __name__ == "__main__":
    asyncio.run(main())
