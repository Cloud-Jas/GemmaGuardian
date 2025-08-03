#!/usr/bin/env python3
"""
Test script to send critical notifications from the surveillance backend.
This script simulates security threats and tests the mobile notification system.
"""

import asyncio
import json
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional
import logging

# Import the surveillance agent modules
from modules.domain.entities import (
    SecurityAnalysis, SecurityThreatLevel, VideoClip, 
    PersonDetection, BoundingBox
)
from modules.infrastructure.mobile_notifications import MobileNotificationService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class NotificationTester:
    """Test class for critical notification system."""
    
    def __init__(self):
        """Initialize the notification tester."""
        self.notification_service = MobileNotificationService(
            min_threat_level=SecurityThreatLevel.LOW  # Allow all notifications for testing
        )
        
    def create_test_video_clip(self, filename: str = "security_clip_20250803_224256_757.mp4") -> VideoClip:
        """Create a test video clip for notification testing."""
        # Create a fake bounding box for person detection
        bbox = BoundingBox(
            x=100, y=100, width=200, height=300, confidence=0.95
        )
        
        # Create a person detection event
        detection = PersonDetection(
            timestamp=datetime.now(),
            bounding_box=bbox,
            frame_number=120,
            confidence=0.95
        )
        
        # Create video clip
        video_clip = VideoClip(
            file_path=f"recordings/{filename}",
            start_time=datetime.now() - timedelta(seconds=30),
            duration=30.0,
            frame_count=900,  # 30 fps * 30 seconds
            resolution=(1920, 1080),
            trigger_detection=detection
        )
        
        return video_clip
    
    def create_security_analysis(self, 
                               threat_level: SecurityThreatLevel,
                               analysis_text: str,
                               confidence: float = 0.85,
                               keywords: list = None) -> SecurityAnalysis:
        """Create a security analysis for testing notifications."""
        
        video_clip = self.create_test_video_clip(
            f"test_{threat_level.value}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.mp4"
        )
        
        if keywords is None:
            keywords = self._get_default_keywords(threat_level)
        
        analysis = SecurityAnalysis(
            video_clip=video_clip,
            analysis_text=analysis_text,
            threat_level=threat_level,
            confidence=confidence,
            keywords=keywords,
            timestamp=datetime.now()
        )
        
        return analysis
    
    def _get_default_keywords(self, threat_level: SecurityThreatLevel) -> list:
        """Get default keywords based on threat level."""
        keyword_map = {
            SecurityThreatLevel.CRITICAL: ["intruder", "weapon", "breaking", "unauthorized", "emergency"],
            SecurityThreatLevel.HIGH: ["suspicious", "trespassing", "climbing", "lurking", "prowling"],
            SecurityThreatLevel.MEDIUM: ["person", "movement", "approaching", "loitering"],
            SecurityThreatLevel.LOW: ["visitor", "delivery", "walking", "routine"]
        }
        return keyword_map.get(threat_level, ["unknown"])
    
    async def test_critical_notification(self):
        """Test critical threat notification."""
        logger.info("🚨 Testing CRITICAL threat notification...")
        
        analysis = self.create_security_analysis(
            threat_level=SecurityThreatLevel.CRITICAL,
            analysis_text="""
            CRITICAL SECURITY THREAT DETECTED:
            
            Multiple unauthorized individuals detected attempting to breach perimeter security.
            Person carrying unknown metallic object approaching main entrance.
            Motion detected near restricted access points.
            Potential security breach in progress.
            
            IMMEDIATE ACTION REQUIRED.
            """,
            confidence=0.95,
            keywords=["intruder", "breach", "weapon", "unauthorized", "emergency"]
        )
        
        success = await self.notification_service.send_security_alert(
            analysis, 
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            logger.info("✅ CRITICAL notification sent successfully!")
        else:
            logger.error("❌ Failed to send CRITICAL notification!")
            
        return success
    
    async def test_high_notification(self):
        """Test high threat notification."""
        logger.info("⚠️ Testing HIGH threat notification...")
        
        analysis = self.create_security_analysis(
            threat_level=SecurityThreatLevel.HIGH,
            analysis_text="""
            HIGH SECURITY ALERT:
            
            Suspicious individual detected loitering near property perimeter for extended period.
            Person attempting to look through windows and testing door handles.
            Behavior pattern suggests potential reconnaissance or attempted break-in.
            
            Recommend immediate monitoring and possible intervention.
            """,
            confidence=0.88,
            keywords=["suspicious", "loitering", "reconnaissance", "trespassing"]
        )
        
        success = await self.notification_service.send_security_alert(
            analysis,
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            logger.info("✅ HIGH threat notification sent successfully!")
        else:
            logger.error("❌ Failed to send HIGH threat notification!")
            
        return success
    
    async def test_medium_notification(self):
        """Test medium threat notification."""
        logger.info("📋 Testing MEDIUM threat notification...")
        
        analysis = self.create_security_analysis(
            threat_level=SecurityThreatLevel.MEDIUM,
            analysis_text="""
            MEDIUM SECURITY ALERT:
            
            Unknown person detected on property during unusual hours.
            Individual appears to be looking for something or someone.
            Movement pattern suggests non-routine activity.
            
            Monitoring recommended.
            """,
            confidence=0.72,
            keywords=["unknown", "unusual", "monitoring", "person"]
        )
        
        success = await self.notification_service.send_security_alert(
            analysis,
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            logger.info("✅ MEDIUM threat notification sent successfully!")
        else:
            logger.error("❌ Failed to send MEDIUM threat notification!")
            
        return success
    
    async def test_custom_notification(self, 
                                     threat_level: SecurityThreatLevel,
                                     message: str,
                                     keywords: list = None):
        """Test custom notification with specified parameters."""
        logger.info(f"🧪 Testing {threat_level.value.upper()} custom notification...")
        
        analysis = self.create_security_analysis(
            threat_level=threat_level,
            analysis_text=message,
            keywords=keywords
        )
        
        success = await self.notification_service.send_security_alert(
            analysis,
            video_path=analysis.video_clip.file_path
        )
        
        if success:
            logger.info(f"✅ {threat_level.value.upper()} custom notification sent successfully!")
        else:
            logger.error(f"❌ Failed to send {threat_level.value.upper()} custom notification!")
            
        return success
    
    async def test_emergency_scenarios(self):
        """Test various emergency scenarios."""
        logger.info("🆘 Testing emergency scenarios...")
        
        scenarios = [
            {
                "name": "Armed Intruder",
                "threat_level": SecurityThreatLevel.CRITICAL,
                "message": "CRITICAL EMERGENCY: Armed individual detected on premises. Multiple security zones breached. Immediate law enforcement response required.",
                "keywords": ["armed", "intruder", "weapon", "emergency", "breach"]
            },
            {
                "name": "Fire/Smoke Detection",
                "threat_level": SecurityThreatLevel.CRITICAL,
                "message": "CRITICAL ALERT: Smoke and fire detected in building. Potential fire emergency. Immediate evacuation and fire department response required.",
                "keywords": ["fire", "smoke", "emergency", "evacuation", "danger"]
            },
            {
                "name": "Break-in Attempt",
                "threat_level": SecurityThreatLevel.HIGH,
                "message": "HIGH ALERT: Active break-in attempt detected. Individual forcing entry through rear door. Security system triggered.",
                "keywords": ["break-in", "forcing", "entry", "security", "intrusion"]
            },
            {
                "name": "Vandalism in Progress",
                "threat_level": SecurityThreatLevel.HIGH,
                "message": "HIGH ALERT: Vandalism in progress. Multiple individuals damaging property and vehicles. Ongoing security incident.",
                "keywords": ["vandalism", "damage", "property", "incident", "multiple"]
            }
        ]
        
        results = []
        for scenario in scenarios:
            logger.info(f"📡 Testing scenario: {scenario['name']}")
            success = await self.test_custom_notification(
                threat_level=scenario['threat_level'],
                message=scenario['message'],
                keywords=scenario['keywords']
            )
            results.append({
                "scenario": scenario['name'],
                "success": success
            })
            
            # Wait 2 seconds between notifications to avoid overwhelming
            await asyncio.sleep(2)
        
        return results
    
    async def run_all_tests(self):
        """Run all notification tests."""
        logger.info("🚀 Starting comprehensive notification tests...")
        logger.info("=" * 60)
        
        results = {
            "critical": False,
            "high": False,
            "medium": False,
            "emergency_scenarios": []
        }
        
        try:
            # Test critical notification
            results["critical"] = await self.test_critical_notification()
            await asyncio.sleep(3)
            
            # Test high notification
            results["high"] = await self.test_high_notification()
            await asyncio.sleep(3)
            
            # Test medium notification
            results["medium"] = await self.test_medium_notification()
            await asyncio.sleep(3)
            
            # Test emergency scenarios
            results["emergency_scenarios"] = await self.test_emergency_scenarios()
            
        except Exception as e:
            logger.error(f"❌ Error during testing: {e}")
            return False
        
        # Print summary
        logger.info("=" * 60)
        logger.info("📊 TEST SUMMARY:")
        logger.info(f"✅ Critical: {'PASS' if results['critical'] else 'FAIL'}")
        logger.info(f"✅ High: {'PASS' if results['high'] else 'FAIL'}")
        logger.info(f"✅ Medium: {'PASS' if results['medium'] else 'FAIL'}")
        
        for scenario in results["emergency_scenarios"]:
            status = "PASS" if scenario["success"] else "FAIL"
            logger.info(f"✅ {scenario['scenario']}: {status}")
        
        total_tests = 3 + len(results["emergency_scenarios"])
        passed_tests = sum([
            results["critical"],
            results["high"], 
            results["medium"]
        ]) + sum(s["success"] for s in results["emergency_scenarios"])
        
        logger.info(f"📈 Overall: {passed_tests}/{total_tests} tests passed")
        logger.info("=" * 60)
        
        return passed_tests == total_tests


async def main():
    """Main function to run notification tests."""
    print("🔔 Critical Notification Test Script")
    print("=" * 50)
    print("This script will test the emergency notification system")
    print("Make sure your mobile app is running and connected to the same network")
    print("=" * 50)
    
    # Ask user what to test
    print("\nSelect test mode:")
    print("1. Test CRITICAL notification only")
    print("2. Test HIGH notification only") 
    print("3. Test MEDIUM notification only")
    print("4. Test all emergency scenarios")
    print("5. Run all tests")
    print("6. Custom notification")
    
    try:
        choice = input("\nEnter your choice (1-6): ").strip()
    except KeyboardInterrupt:
        print("\n👋 Test cancelled by user")
        return
    
    tester = NotificationTester()
    
    try:
        if choice == "1":
            await tester.test_critical_notification()
        elif choice == "2":
            await tester.test_high_notification()
        elif choice == "3":
            await tester.test_medium_notification()
        elif choice == "4":
            await tester.test_emergency_scenarios()
        elif choice == "5":
            await tester.run_all_tests()
        elif choice == "6":
            # Custom notification
            print("\nCustom notification setup:")
            levels = ["low", "medium", "high", "critical"]
            print("Threat levels: " + ", ".join(levels))
            
            level_input = input("Enter threat level: ").strip().lower()
            if level_input not in levels:
                print("❌ Invalid threat level")
                return
                
            message = input("Enter alert message: ").strip()
            if not message:
                print("❌ Message cannot be empty")
                return
                
            keywords_input = input("Enter keywords (comma-separated, optional): ").strip()
            keywords = [k.strip() for k in keywords_input.split(",")] if keywords_input else None
            
            threat_level = SecurityThreatLevel(level_input)
            await tester.test_custom_notification(threat_level, message, keywords)
        else:
            print("❌ Invalid choice")
            return
            
    except KeyboardInterrupt:
        print("\n👋 Test interrupted by user")
    except Exception as e:
        logger.error(f"❌ Test failed with error: {e}")
    
    print("\n🏁 Test completed!")


if __name__ == "__main__":
    asyncio.run(main())
