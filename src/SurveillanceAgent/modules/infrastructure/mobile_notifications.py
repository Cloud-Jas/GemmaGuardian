"""Mobile notification service for security alerts."""

import json
import requests
import socket
from datetime import datetime
from typing import Dict, List, Optional, Any
from loguru import logger
import asyncio
import aiohttp
from pathlib import Path

from modules.domain.entities import SecurityAnalysis, SecurityThreatLevel


class MobileNotificationService:
    """Service for sending notifications to mobile devices on local network."""
    
    def __init__(self, 
                 notification_url: str = None,
                 webhook_urls: List[str] = None,
                 min_threat_level: SecurityThreatLevel = SecurityThreatLevel.MEDIUM):
        """Initialize mobile notification service.
        
        Args:
            notification_url: Primary notification URL (e.g., HTTP server on mobile)
            webhook_urls: List of webhook URLs for notifications
            min_threat_level: Minimum threat level to trigger notifications
        """
        self.notification_url = notification_url
        self.webhook_urls = webhook_urls or []
        self.min_threat_level = min_threat_level
        self.server_ip = self._get_local_ip()
        self.notification_port = 8888
        self.enable_push_notifications = True
        
        # Log initialization details
        logger.info(f"📱 Mobile Notification Service initialized")
        logger.info(f"🌐 Server IP: {self.server_ip}, Port: {self.notification_port}")
        logger.info(f"🔗 HTTP URL: {self.notification_url if self.notification_url else 'Not configured'}")
        logger.info(f"🪝 Webhooks: {len(self.webhook_urls)} configured")
        logger.info(f"⚠️ Min threat level: {self.min_threat_level.value}")
        logger.info(f"🔔 Push notifications: {'Enabled' if self.enable_push_notifications else 'Disabled'}")
        
        # Log available notification methods
        methods = []
        if self.enable_push_notifications:
            methods.append("UDP Broadcast")
        if self.notification_url:
            methods.append("HTTP POST")
        if self.webhook_urls:
            methods.append(f"Webhooks ({len(self.webhook_urls)})")
        
        logger.info(f"📡 Available notification methods: {', '.join(methods) if methods else 'None configured'}")
        
    def _get_local_ip(self) -> str:
        """Get local IP address of the server."""
        try:
            # Connect to a dummy address to get local IP
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            logger.debug(f"🌐 Local IP address detected: {ip}")
            return ip
        except Exception as e:
            logger.warning(f"⚠️ Could not determine local IP address: {e}")
            logger.info("🌐 Using localhost as fallback")
            return "localhost"
    
    def _get_broadcast_addresses(self) -> List[str]:
        """Get all possible broadcast addresses for the network interfaces."""
        import ipaddress
        
        broadcast_addresses = set()
        
        try:
            # Add standard broadcast address
            broadcast_addresses.add("255.255.255.255")
            
            # Try to get network-specific broadcast addresses
            import netifaces
            
            for interface in netifaces.interfaces():
                try:
                    addresses = netifaces.ifaddresses(interface)
                    if netifaces.AF_INET in addresses:
                        for addr_info in addresses[netifaces.AF_INET]:
                            ip = addr_info.get('addr')
                            netmask = addr_info.get('netmask')
                            
                            if ip and netmask and not ip.startswith('127.'):
                                # Calculate broadcast address
                                network = ipaddress.IPv4Network(f"{ip}/{netmask}", strict=False)
                                broadcast_addresses.add(str(network.broadcast_address))
                                logger.debug(f"🌐 Found broadcast address: {network.broadcast_address} for interface {interface}")
                                
                except Exception as iface_error:
                    logger.debug(f"⚠️ Could not process interface {interface}: {iface_error}")
                    
        except ImportError:
            logger.debug("📦 netifaces not available, using basic broadcast addressing")
            # Fallback: calculate broadcast for common network ranges
            local_ip = self.server_ip
            if local_ip != "localhost":
                try:
                    ip_parts = local_ip.split('.')
                    if len(ip_parts) == 4:
                        # Common home network broadcast addresses
                        if ip_parts[0] == '192' and ip_parts[1] == '168':
                            broadcast_addresses.add(f"192.168.{ip_parts[2]}.255")
                        elif ip_parts[0] == '10':
                            broadcast_addresses.add("10.255.255.255")
                        elif ip_parts[0] == '172' and 16 <= int(ip_parts[1]) <= 31:
                            broadcast_addresses.add(f"172.{ip_parts[1]}.255.255")
                except Exception as calc_error:
                    logger.debug(f"⚠️ Could not calculate network broadcast: {calc_error}")
        
        except Exception as e:
            logger.debug(f"⚠️ Error discovering broadcast addresses: {e}")
        
        # Ensure we have at least the global broadcast
        if not broadcast_addresses:
            broadcast_addresses.add("255.255.255.255")
        
        addresses_list = list(broadcast_addresses)
        logger.debug(f"🌐 Discovered broadcast addresses: {addresses_list}")
        return addresses_list
    
    def should_notify(self, analysis: SecurityAnalysis) -> bool:
        """Check if notification should be sent based on threat level.
        
        Args:
            analysis: Security analysis to check
            
        Returns:
            True if notification should be sent
        """
        if not self.enable_push_notifications:
            logger.debug("📵 Push notifications disabled - skipping notification")
            return False
            
        if not analysis.is_threat_detected:
            logger.debug("📵 No threat detected - skipping notification")
            return False
            
        # Check threat level threshold
        threat_levels = {
            SecurityThreatLevel.LOW: 1,
            SecurityThreatLevel.MEDIUM: 2,
            SecurityThreatLevel.HIGH: 3,
            SecurityThreatLevel.CRITICAL: 4
        }
        
        analysis_level = threat_levels.get(analysis.threat_level, 0)
        min_level = threat_levels.get(self.min_threat_level, 2)
        
        should_send = analysis_level >= min_level
        
        if should_send:
            logger.debug(f"✅ Notification threshold met - Analysis: {analysis.threat_level.value} (level {analysis_level}) >= Minimum: {self.min_threat_level.value} (level {min_level})")
        else:
            logger.debug(f"📵 Notification threshold not met - Analysis: {analysis.threat_level.value} (level {analysis_level}) < Minimum: {self.min_threat_level.value} (level {min_level})")
        
        return should_send
    
    async def send_security_alert(self, analysis: SecurityAnalysis, video_path: str = None) -> bool:
        """Send security alert to mobile device.
        
        Args:
            analysis: Security analysis with threat details
            video_path: Path to the recorded video clip
            
        Returns:
            True if notification was sent successfully
        """
        if not self.should_notify(analysis):
            logger.debug(f"📵 Notification skipped - threat level {analysis.threat_level.value} below threshold {self.min_threat_level.value}")
            return False
            
        # Create notification payload optimized for UDP broadcast
        notification = self._create_broadcast_notification_payload(analysis, video_path)
        video_filename = Path(video_path).name if video_path else "N/A"
        
        logger.info(f"📱 Sending security alert notification - Threat: {analysis.threat_level.value.upper()}, Confidence: {analysis.confidence:.2f}, Video: {video_filename}")
        
        success = False
        
        # Try sending via different methods - prioritize broadcast for real-time alerts
        try:
            # Method 1: Local network broadcast (PRIMARY for real-time alerts)
            broadcast_success = await self._send_broadcast_notification(notification)
            success = broadcast_success
            
            # Method 2: Webhook notifications (backup)
            if self.webhook_urls and not success:
                webhook_success = await self._send_webhook_notifications(notification)
                success = success or webhook_success
            
            # Method 3: HTTP POST (fallback only)
            if self.notification_url and not success:
                success = await self._send_http_notification(notification)
            
            if success:
                logger.success(f"✅ Security alert sent successfully - {analysis.threat_level.value.upper()} threat detected, confidence: {analysis.confidence:.2f}")
            else:
                logger.warning(f"⚠️ Failed to send security alert via any method - {analysis.threat_level.value} threat, video: {video_filename}")
                
        except Exception as e:
            logger.error(f"❌ Error sending security alert notification: {e}")
            return False
            
        return success
    
    def _create_broadcast_notification_payload(self, analysis: SecurityAnalysis, video_path: str = None) -> Dict[str, Any]:
        """Create lightweight notification payload optimized for UDP broadcast.
        
        Args:
            analysis: Security analysis
            video_path: Path to video clip
            
        Returns:
            Lightweight notification payload for real-time broadcast
        """
        # Get basic video information
        video_filename = None
        video_id = None
        
        if video_path:
            video_filename = Path(video_path).name
            video_id = Path(video_path).stem
        
        # Extract concise summary for notification
        analysis_summary = self._extract_summary(analysis.analysis_text)
        
        # Create lightweight payload for broadcast (fast transmission)
        payload = {
            "type": "security_alert",
            "id": video_id or f"alert_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
            "timestamp": datetime.now().isoformat() + "Z",
            "threatLevel": analysis.threat_level.value.upper(),
            "confidence": analysis.confidence,
            "summary": analysis_summary,
            "camera": self._extract_camera_name(video_path),
            "keywords": analysis.keywords[:3] if analysis.keywords else [],  # Limit keywords for UDP size
            "video_id": video_id,
            "video_filename": video_filename,
            "is_threat_detected": analysis.is_threat_detected,
            "api_endpoint": f"http://{self.server_ip}:{self.notification_port}/api/security/videos/{video_id}" if video_id else None,
            "server_info": {
                "ip": self.server_ip,
                "port": self.notification_port
            }
        }
        
        return payload
    
    def _create_notification_payload(self, analysis: SecurityAnalysis, video_path: str = None) -> Dict[str, Any]:
        """Create detailed notification payload for HTTP requests (used when user taps notification).
        
        Args:
            analysis: Security analysis
            video_path: Path to video clip
            
        Returns:
            Comprehensive notification payload with full analysis data for HTTP API
        """
        # Get video information
        video_filename = None
        video_url = None
        video_id = None
        
        if video_path:
            video_filename = Path(video_path).name
            video_id = Path(video_path).stem
            video_url = f"http://{self.server_ip}:{self.notification_port}/recordings/{video_filename}"
        
        # Extract summary from analysis text
        analysis_summary = self._extract_summary(analysis.analysis_text)
        
        # Create comprehensive payload
        payload = {
            "type": "security_alert",
            "id": video_id or f"alert_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
            "timestamp": datetime.now().isoformat() + "Z",
            "threatLevel": analysis.threat_level.value.upper(),
            "confidence": analysis.confidence,
            "description": analysis_summary,
            "camera": self._extract_camera_name(video_path),
            "alert": {
                "threat_level": analysis.threat_level.value,
                "confidence": analysis.confidence,
                "is_threat_detected": analysis.is_threat_detected,
                "summary": analysis_summary,
                "full_analysis": analysis.analysis_text,
                "keywords": analysis.keywords,
                "timestamp": analysis.timestamp.isoformat(),
                "camera_location": self._extract_camera_name(video_path)
            },
            "videoClip": {
                "id": video_id,
                "file_path": str(video_path) if video_path else None,
                "url": video_url,
                "fileName": video_filename,
                "timestamp": analysis.timestamp.isoformat(),
                "duration": "PT2M0S",  # Default duration
                "thumbnailUrl": f"http://{self.server_ip}:{self.notification_port}/thumbnails/{video_id}.jpg" if video_id else None
            } if video_path else None,
            "system": {
                "server_ip": self.server_ip,
                "notification_port": self.notification_port,
                "api_base_url": f"http://{self.server_ip}:{self.notification_port}/api",
                "recordings_url": f"http://{self.server_ip}:{self.notification_port}/recordings"
            }
        }
        
        return payload
    
    def _extract_summary(self, analysis_text: str) -> str:
        """Extract a concise summary from analysis text."""
        if not analysis_text:
            return "Security event detected"
        
        # Remove common prefixes and get the essential information
        text = analysis_text.strip()
        
        # Look for key phrases that indicate the core finding
        if "No security incidents detected" in text or "Routine surveillance" in text:
            return "Routine activity - no security concerns"
        
        # Extract first meaningful sentence
        sentences = text.split('.')
        for sentence in sentences:
            sentence = sentence.strip()
            if len(sentence) > 20:  # Skip very short sentences
                # Remove markdown and formatting
                sentence = sentence.replace('**', '').replace('*', '').replace('#', '')
                if len(sentence) > 100:
                    sentence = sentence[:100] + "..."
                return sentence
        
        # Fallback to first 100 characters
        summary = text[:100] + "..." if len(text) > 100 else text
        return summary.replace('**', '').replace('*', '').replace('#', '')
    
    def _extract_camera_name(self, file_path: str) -> str:
        """Extract camera name from file path or use default."""
        if not file_path:
            return "Security Camera"
        
        path_lower = file_path.lower()
        if "front" in path_lower or "door" in path_lower:
            return "Front Door Camera"
        elif "back" in path_lower or "yard" in path_lower:
            return "Backyard Camera"
        elif "side" in path_lower or "gate" in path_lower:
            return "Side Gate Camera"
        elif "garage" in path_lower:
            return "Garage Camera"
        else:
            return "Security Camera"
    
    async def _send_http_notification(self, payload: Dict[str, Any]) -> bool:
        """Send notification via HTTP POST.
        
        Args:
            payload: Notification payload
            
        Returns:
            True if successful
        """
        logger.info(f"🌐 Sending HTTP notification to {self.notification_url}")
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    self.notification_url,
                    json=payload,
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    if response.status == 200:
                        response_text = await response.text()
                        logger.success(f"✅ HTTP notification sent successfully to {self.notification_url} - Response: {response.status}")
                        return True
                    else:
                        logger.warning(f"⚠️ HTTP notification failed - URL: {self.notification_url}, Status: {response.status}")
                        
        except asyncio.TimeoutError:
            logger.warning(f"⏱️ HTTP notification timeout - URL: {self.notification_url}")
        except Exception as e:
            logger.warning(f"❌ HTTP notification error - URL: {self.notification_url}, Error: {e}")
            
        return False
    
    async def _send_webhook_notifications(self, payload: Dict[str, Any]) -> bool:
        """Send notifications to webhook URLs.
        
        Args:
            payload: Notification payload
            
        Returns:
            True if at least one webhook succeeded
        """
        success_count = 0
        logger.info(f"🪝 Sending webhook notifications to {len(self.webhook_urls)} endpoints")
        
        for webhook_url in self.webhook_urls:
            try:
                async with aiohttp.ClientSession() as session:
                    async with session.post(
                        webhook_url,
                        json=payload,
                        timeout=aiohttp.ClientTimeout(total=5)
                    ) as response:
                        if response.status == 200:
                            logger.success(f"✅ Webhook notification sent successfully to {webhook_url}")
                            success_count += 1
                        else:
                            logger.warning(f"⚠️ Webhook notification failed - URL: {webhook_url}, Status: {response.status}")
                            
            except asyncio.TimeoutError:
                logger.warning(f"⏱️ Webhook notification timeout - URL: {webhook_url}")
            except Exception as e:
                logger.warning(f"❌ Webhook notification error - URL: {webhook_url}, Error: {e}")
        
        if success_count > 0:
            logger.success(f"✅ Webhook notifications completed - {success_count}/{len(self.webhook_urls)} successful")
        else:
            logger.warning(f"⚠️ All webhook notifications failed - 0/{len(self.webhook_urls)} successful")
                
        return success_count > 0
    
    async def _send_broadcast_notification(self, payload: Dict[str, Any]) -> bool:
        """Send notification via UDP broadcast for real-time alerts.
        
        Args:
            payload: Lightweight notification payload
            
        Returns:
            True if broadcast sent
        """
        threat_level = payload.get('threatLevel', 'UNKNOWN')
        summary = payload.get('summary', 'Security alert')[:50] + "..." if len(payload.get('summary', '')) > 50 else payload.get('summary', 'Security alert')
        
        logger.info(f"📡 Broadcasting UDP notification - Threat: {threat_level}, Summary: {summary}")
        
        try:
            # Create compact message for UDP transmission
            message = json.dumps(payload, separators=(',', ':')).encode('utf-8')
            
            # Check message size (UDP limit is typically 65507 bytes)
            if len(message) > 1400:  # Conservative limit for network reliability
                logger.warning(f"⚠️ UDP message too large ({len(message)} bytes), creating compact version")
                compact_payload = {
                    "type": payload.get("type"),
                    "id": payload.get("id"),
                    "timestamp": payload.get("timestamp"),
                    "threatLevel": payload.get("threatLevel"),
                    "confidence": payload.get("confidence"),
                    "summary": payload.get("summary", "")[:100],  # Limit summary length
                    "camera": payload.get("camera"),
                    "keywords": payload.get("keywords", [])[:2],  # Limit keywords
                    "server_info": payload.get("server_info")
                }
                message = json.dumps(compact_payload, separators=(',', ':')).encode('utf-8')
                logger.info(f"📦 Compact message size: {len(message)} bytes")
            
            # Get network interfaces for better broadcast coverage
            broadcast_addresses = self._get_broadcast_addresses()
            
            success_count = 0
            ports = [9999, 9998, 10000, 8888]  # Multiple ports for redundancy
            
            for port in ports:
                try:
                    # Create UDP socket for each port
                    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
                    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                    
                    # Set socket timeout to avoid hanging
                    sock.settimeout(1.0)
                    
                    # Broadcast to all discovered addresses
                    for broadcast_addr in broadcast_addresses:
                        try:
                            sock.sendto(message, (broadcast_addr, port))
                            logger.debug(f"📡 Sent to {broadcast_addr}:{port}")
                        except Exception as addr_error:
                            logger.debug(f"⚠️ Failed to send to {broadcast_addr}:{port} - {addr_error}")
                    
                    sock.close()
                    success_count += 1
                    
                except Exception as port_error:
                    logger.debug(f"⚠️ Port {port} broadcast failed: {port_error}")
                    try:
                        sock.close()
                    except:
                        pass
            
            if success_count > 0:
                logger.success(f"✅ UDP broadcast sent successfully - Threat: {threat_level}, Payload size: {len(message)} bytes")
                logger.info(f"📡 Broadcast successful on {success_count}/{len(ports)} ports to {len(broadcast_addresses)} addresses")
                logger.info(f"🌐 Broadcast addresses: {', '.join(broadcast_addresses)}")
                return True
            else:
                logger.error(f"❌ UDP broadcast failed on all ports and addresses")
                return False
            
        except Exception as e:
            logger.error(f"❌ UDP broadcast notification failed: {e}")
            return False
    
    def send_test_notification(self) -> bool:
        """Send a test notification to verify connectivity.
        
        Returns:
            True if test notification was sent
        """
        logger.info(f"🧪 Sending test notification - Server IP: {self.server_ip}, Port: {self.notification_port}")
        
        test_payload = {
            "type": "test_notification",
            "timestamp": datetime.now().isoformat() + "Z",
            "summary": "Security monitoring system test notification",
            "threatLevel": "TEST",
            "server_info": {
                "ip": self.server_ip,
                "port": self.notification_port,
                "status": "active"
            }
        }
        
        try:
            # Run async function synchronously for test
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            
            # Prioritize broadcast for test too
            success = loop.run_until_complete(self._send_broadcast_notification(test_payload))
            
            # Fallback to other methods if needed
            if not success and self.notification_url:
                logger.info("🔄 Trying HTTP notification as fallback for test")
                success = loop.run_until_complete(self._send_http_notification(test_payload))
            
            if not success and self.webhook_urls:
                logger.info("🔄 Trying webhook notifications as fallback for test")
                webhook_success = loop.run_until_complete(self._send_webhook_notifications(test_payload))
                success = success or webhook_success
            
            loop.close()
            
            if success:
                logger.success("✅ Test notification sent successfully via broadcast")
            else:
                logger.warning("⚠️ Test notification failed - check network connectivity and mobile app")
                
            return success
            
        except Exception as e:
            logger.error(f"❌ Test notification error: {e}")
            return False
    
    def test_udp_broadcast_detailed(self) -> Dict[str, Any]:
        """Perform detailed UDP broadcast testing with network diagnostics.
        
        Returns:
            Dictionary with test results and diagnostics
        """
        logger.info("🧪 Starting detailed UDP broadcast test...")
        
        # Get network information
        broadcast_addresses = self._get_broadcast_addresses()
        local_ip = self.server_ip
        
        test_results = {
            "local_ip": local_ip,
            "broadcast_addresses": broadcast_addresses,
            "ports_tested": [9999, 9998, 10000, 8888],
            "test_results": [],
            "overall_success": False
        }
        
        # Create test payload
        test_payload = {
            "type": "udp_test",
            "timestamp": datetime.now().isoformat() + "Z",
            "test_id": f"test_{datetime.now().strftime('%H%M%S')}",
            "summary": "UDP broadcast connectivity test",
            "server_info": {
                "ip": local_ip,
                "port": self.notification_port
            }
        }
        
        message = json.dumps(test_payload, separators=(',', ':')).encode('utf-8')
        logger.info(f"📦 Test message size: {len(message)} bytes")
        
        successful_combinations = 0
        total_combinations = 0
        
        for port in test_results["ports_tested"]:
            for broadcast_addr in broadcast_addresses:
                total_combinations += 1
                test_result = {
                    "port": port,
                    "address": broadcast_addr,
                    "success": False,
                    "error": None
                }
                
                try:
                    # Create socket
                    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
                    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                    sock.settimeout(1.0)
                    
                    # Send test message
                    bytes_sent = sock.sendto(message, (broadcast_addr, port))
                    
                    test_result["success"] = True
                    test_result["bytes_sent"] = bytes_sent
                    successful_combinations += 1
                    
                    logger.success(f"✅ UDP test successful: {broadcast_addr}:{port} ({bytes_sent} bytes)")
                    
                    sock.close()
                    
                except Exception as e:
                    test_result["error"] = str(e)
                    logger.warning(f"⚠️ UDP test failed: {broadcast_addr}:{port} - {e}")
                    try:
                        sock.close()
                    except:
                        pass
                
                test_results["test_results"].append(test_result)
        
        test_results["successful_combinations"] = successful_combinations
        test_results["total_combinations"] = total_combinations
        test_results["success_rate"] = successful_combinations / total_combinations if total_combinations > 0 else 0
        test_results["overall_success"] = successful_combinations > 0
        
        # Log summary
        logger.info(f"🧪 UDP Broadcast Test Summary:")
        logger.info(f"   Local IP: {local_ip}")
        logger.info(f"   Broadcast addresses: {len(broadcast_addresses)} found")
        logger.info(f"   Ports tested: {len(test_results['ports_tested'])}")
        logger.info(f"   Success rate: {successful_combinations}/{total_combinations} ({test_results['success_rate']:.1%})")
        
        if test_results["overall_success"]:
            logger.success(f"✅ UDP broadcast test completed - {successful_combinations} successful combinations")
        else:
            logger.error(f"❌ UDP broadcast test failed - no successful combinations")
            logger.info("💡 Troubleshooting tips:")
            logger.info("   1. Check Windows Firewall settings")
            logger.info("   2. Ensure mobile app is listening on the correct ports")
            logger.info("   3. Verify both devices are on the same network")
            logger.info("   4. Try disabling antivirus temporarily")
        
        return test_results


class NotificationServer:
    """Simple HTTP server to serve video files and status to mobile devices."""
    
    def __init__(self, port: int = 8888, video_dir: str = "./recordings"):
        """Initialize notification server.
        
        Args:
            port: Port to serve on
            video_dir: Directory containing video files
        """
        self.port = port
        self.video_dir = Path(video_dir)
        self.is_running = False
        
    async def start_server(self):
        """Start the notification server."""
        from aiohttp import web
        
        app = web.Application()
        
        # Add routes
        app.router.add_get('/status', self._handle_status)
        app.router.add_get('/videos/{filename}', self._handle_video)
        app.router.add_get('/recent-alerts', self._handle_recent_alerts)
        
        # Add static route for videos
        app.router.add_static('/videos/', self.video_dir, name='videos')
        
        runner = web.AppRunner(app)
        await runner.setup()
        
        site = web.TCPSite(runner, '0.0.0.0', self.port)
        await site.start()
        
        self.is_running = True
        logger.success(f"Notification server started on port {self.port}")
        
    async def _handle_status(self, request):
        """Handle status requests from mobile devices."""
        from aiohttp import web
        
        status = {
            "status": "active",
            "timestamp": datetime.now().isoformat() + "Z",
            "server_info": {
                "port": self.port,
                "video_directory": str(self.video_dir)
            }
        }
        
        return web.json_response(status)
    
    async def _handle_video(self, request):
        """Handle video file requests."""
        from aiohttp import web
        
        filename = request.match_info['filename']
        video_path = self.video_dir / filename
        
        if video_path.exists():
            return web.FileResponse(video_path)
        else:
            return web.Response(status=404, text="Video not found")
    
    async def _handle_recent_alerts(self, request):
        """Handle recent alerts requests."""
        from aiohttp import web
        
        # This would typically query the database for recent alerts
        # For now, return a placeholder response
        alerts = {
            "recent_alerts": [],
            "timestamp": datetime.now().isoformat()
        }
        
        return web.json_response(alerts)
