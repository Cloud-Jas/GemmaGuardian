"""Security Data API for serving real-time security analysis and video data to mobile app."""

import json
import sqlite3
from datetime import datetime, timedelta
from pathlib import Path
from typing import List, Dict, Any, Optional
from fastapi import FastAPI, HTTPException, Response
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from loguru import logger
import os
import asyncio
import shutil
import threading

from modules.domain.entities import SecurityAnalysis, SecurityThreatLevel


class SecurityDataAPI:
    """API service for providing real security data to mobile applications."""
    
    def __init__(self, data_dir: str = "data", recordings_dir: str = "recordings", 
                 analysis_logs_dir: str = "analysis_logs", frames_dir: str = "frames_analyzed"):
        """Initialize Security Data API.
        
        Args:
            data_dir: Directory containing security databases and JSON files
            recordings_dir: Directory containing video recordings
            analysis_logs_dir: Directory containing detailed analysis logs
            frames_dir: Directory containing analyzed frame images
        """
        self.data_dir = Path(data_dir)
        self.recordings_dir = Path(recordings_dir)
        self.analysis_logs_dir = Path(analysis_logs_dir)
        self.frames_dir = Path(frames_dir)
        
        # Server configuration
        self.host = "0.0.0.0"
        self.port = 8888
        
        # Daily cleanup configuration
        self.cleanup_enabled = True
        self.cleanup_time = "00:00"  # Midnight
        self.last_cleanup_date = None
        self.cleanup_thread = None
        
        # Ensure directories exist
        for dir_path in [self.data_dir, self.recordings_dir, self.analysis_logs_dir, self.frames_dir]:
            dir_path.mkdir(exist_ok=True)
        
        # Initialize FastAPI app
        self.app = FastAPI(
            title="SurveillanceAgent Data API",
            description="Real-time security monitoring data API",
            version="1.0.0"
        )
        
        # Add CORS middleware for mobile app access
        self.app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],  # In production, specify your mobile app domain
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )
        
        # Mount static files for video and image serving
        self.app.mount("/recordings", StaticFiles(directory=str(self.recordings_dir)), name="recordings")
        self.app.mount("/frames", StaticFiles(directory=str(self.frames_dir)), name="frames")
        
        self._setup_routes()
        self._start_cleanup_scheduler()
    
    def _start_cleanup_scheduler(self):
        """Start the daily cleanup scheduler in a background thread."""
        if not self.cleanup_enabled:
            return
        
        def cleanup_scheduler():
            """Background thread to check for daily cleanup."""
            while True:
                try:
                    current_time = datetime.now()
                    current_date = current_time.date()
                    
                    # Check if it's time for cleanup (midnight or later) and we haven't cleaned today
                    if (self.last_cleanup_date != current_date and 
                        current_time.hour == 0 and current_time.minute == 0):
                        
                        logger.info("🧹 Starting daily cleanup of analysis logs and recordings...")
                        asyncio.run(self._perform_daily_cleanup())
                        self.last_cleanup_date = current_date
                        logger.success("✅ Daily cleanup completed successfully")
                    
                    # Sleep for 60 seconds before next check
                    threading.Event().wait(60)
                    
                except Exception as e:
                    logger.error(f"❌ Error in cleanup scheduler: {e}")
                    threading.Event().wait(300)  # Wait 5 minutes on error
        
        # Start scheduler thread
        self.cleanup_thread = threading.Thread(target=cleanup_scheduler, daemon=True)
        self.cleanup_thread.start()
        logger.info("🕐 Daily cleanup scheduler started - will clean at midnight")
    
    async def _perform_daily_cleanup(self):
        """Perform daily cleanup of analysis logs and recordings."""
        try:
            cleanup_stats = {
                "recordings_deleted": 0,
                "analysis_logs_deleted": 0,
                "frames_deleted": 0,
                "security_analyses_cleared": False,
                "storage_freed_mb": 0
            }
            
            # Backup today's data before cleanup (optional)
            backup_dir = self.data_dir / "daily_backups" / datetime.now().strftime("%Y-%m-%d")
            if not backup_dir.exists():
                backup_dir.mkdir(parents=True, exist_ok=True)
            
            # Clear recordings directory
            if self.recordings_dir.exists():
                total_size = 0
                for video_file in self.recordings_dir.glob("*.mp4"):
                    file_size = video_file.stat().st_size
                    total_size += file_size
                    video_file.unlink()
                    cleanup_stats["recordings_deleted"] += 1
                
                cleanup_stats["storage_freed_mb"] += total_size / (1024 * 1024)
                logger.info(f"🗑️ Deleted {cleanup_stats['recordings_deleted']} video recordings")
            
            # Clear analysis logs directory
            if self.analysis_logs_dir.exists():
                for log_file in self.analysis_logs_dir.glob("*.json"):
                    file_size = log_file.stat().st_size
                    cleanup_stats["storage_freed_mb"] += file_size / (1024 * 1024)
                    log_file.unlink()
                    cleanup_stats["analysis_logs_deleted"] += 1
                
                logger.info(f"📄 Deleted {cleanup_stats['analysis_logs_deleted']} analysis log files")
            
            # Clear frames directory
            if self.frames_dir.exists():
                for frame_file in self.frames_dir.glob("*"):
                    if frame_file.is_file():
                        file_size = frame_file.stat().st_size
                        cleanup_stats["storage_freed_mb"] += file_size / (1024 * 1024)
                        frame_file.unlink()
                        cleanup_stats["frames_deleted"] += 1
                
                logger.info(f"🖼️ Deleted {cleanup_stats['frames_deleted']} analyzed frame images")
            
            # Clear security analyses JSON file
            analyses_file = self.data_dir / "security_analyses.json"
            if analyses_file.exists():
                # Backup before clearing
                backup_analyses = backup_dir / "security_analyses.json"
                shutil.copy2(analyses_file, backup_analyses)
                
                # Clear the file (create empty array)
                with open(analyses_file, 'w', encoding='utf-8') as f:
                    json.dump([], f)
                cleanup_stats["security_analyses_cleared"] = True
                logger.info("📋 Cleared security analyses database")
            
            # Log cleanup summary
            logger.success(f"🧹 Daily cleanup completed:")
            logger.success(f"   📹 Recordings deleted: {cleanup_stats['recordings_deleted']}")
            logger.success(f"   📄 Analysis logs deleted: {cleanup_stats['analysis_logs_deleted']}")
            logger.success(f"   🖼️ Frames deleted: {cleanup_stats['frames_deleted']}")
            logger.success(f"   💾 Storage freed: {cleanup_stats['storage_freed_mb']:.2f} MB")
            logger.success(f"   📁 Backup saved to: {backup_dir}")
            
            return cleanup_stats
            
        except Exception as e:
            logger.error(f"❌ Error during daily cleanup: {e}")
            raise
    
    async def _manual_cleanup(self, backup: bool = True) -> Dict[str, Any]:
        """Manually trigger cleanup (for API endpoint)."""
        try:
            if backup:
                logger.info("🧹 Starting manual cleanup with backup...")
                cleanup_stats = await self._perform_daily_cleanup()
            else:
                logger.info("🧹 Starting manual cleanup without backup...")
                cleanup_stats = {
                    "recordings_deleted": 0,
                    "analysis_logs_deleted": 0,
                    "frames_deleted": 0,
                    "security_analyses_cleared": False,
                    "storage_freed_mb": 0
                }
                
                # Clear without backup
                if self.recordings_dir.exists():
                    for video_file in self.recordings_dir.glob("*.mp4"):
                        file_size = video_file.stat().st_size
                        cleanup_stats["storage_freed_mb"] += file_size / (1024 * 1024)
                        video_file.unlink()
                        cleanup_stats["recordings_deleted"] += 1
                
                if self.analysis_logs_dir.exists():
                    for log_file in self.analysis_logs_dir.glob("*.json"):
                        file_size = log_file.stat().st_size
                        cleanup_stats["storage_freed_mb"] += file_size / (1024 * 1024)
                        log_file.unlink()
                        cleanup_stats["analysis_logs_deleted"] += 1
                
                if self.frames_dir.exists():
                    for frame_file in self.frames_dir.glob("*"):
                        if frame_file.is_file():
                            file_size = frame_file.stat().st_size
                            cleanup_stats["storage_freed_mb"] += file_size / (1024 * 1024)
                            frame_file.unlink()
                            cleanup_stats["frames_deleted"] += 1
                
                analyses_file = self.data_dir / "security_analyses.json"
                if analyses_file.exists():
                    with open(analyses_file, 'w', encoding='utf-8') as f:
                        json.dump([], f)
                    cleanup_stats["security_analyses_cleared"] = True
            
            # Update last cleanup date
            self.last_cleanup_date = datetime.now().date()
            
            return {
                "success": True,
                "timestamp": datetime.now().isoformat() + "Z",
                "cleanup_stats": cleanup_stats,
                "message": "Manual cleanup completed successfully"
            }
            
        except Exception as e:
            logger.error(f"❌ Error during manual cleanup: {e}")
            return {
                "success": False,
                "timestamp": datetime.now().isoformat() + "Z",
                "error": str(e),
                "message": "Manual cleanup failed"
            }
    
    def _get_server_ip(self) -> str:
        """Get the server's actual IP address."""
        import socket
        try:
            # Connect to a remote address to determine the local IP
            # This doesn't actually send data, just determines the route
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(("8.8.8.8", 80))
                return s.getsockname()[0]
        except Exception:
            # Fallback to localhost if unable to determine IP
            return "localhost"
    
    def _get_base_url(self) -> str:
        """Get the base URL for this API server."""
        server_ip = self._get_server_ip()
        return f"http://{server_ip}:{self.port}"
    
    def _setup_routes(self):
        """Setup API routes."""
        
        @self.app.get("/health")
        async def health_check():
            """API health check."""
            logger.info("💚 API Request: GET /health - Health check")
            result = {"status": "healthy", "timestamp": datetime.now().isoformat() + "Z"}
            logger.success("✅ API Response: GET /health - Service healthy")
            return result
        
        @self.app.get("/api/security/stats")
        async def get_security_stats():
            """Get real-time security statistics."""
            logger.info("📊 API Request: GET /api/security/stats - Retrieving security statistics")
            try:
                stats = await self._get_security_statistics()
                logger.success(f"✅ API Response: GET /api/security/stats - Returning {stats.get('totalAlerts', 0)} total alerts, {stats.get('recentAlerts24h', 0)} in last 24h")
                return JSONResponse(content=stats)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/stats - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/alerts")
        async def get_security_alerts(limit: int = 50, threat_level: Optional[str] = None):
            """Get recent security alerts with full analysis details."""
            logger.info(f"🚨 API Request: GET /api/security/alerts - limit={limit}, threat_level={threat_level or 'all'}")
            try:
                alerts = await self._get_security_alerts(limit, threat_level)
                logger.success(f"✅ API Response: GET /api/security/alerts - Returning {len(alerts)} alerts")
                return JSONResponse(content=alerts)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/alerts - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/alerts/{alert_id}")
        async def get_alert_details(alert_id: str):
            """Get detailed analysis for specific alert."""
            logger.info(f"🔍 API Request: GET /api/security/alerts/{alert_id} - Retrieving alert details")
            try:
                alert_details = await self._get_alert_details(alert_id)
                if not alert_details:
                    logger.warning(f"⚠️ API Response: GET /api/security/alerts/{alert_id} - Alert not found")
                    raise HTTPException(status_code=404, detail="Alert not found")
                logger.success(f"✅ API Response: GET /api/security/alerts/{alert_id} - Alert details retrieved")
                return JSONResponse(content=alert_details)
            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/alerts/{alert_id} - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/videos")
        async def get_video_recordings(limit: int = 20):
            """Get list of video recordings with metadata."""
            logger.info(f"📹 API Request: GET /api/security/videos - limit={limit}")
            try:
                videos = await self._get_video_recordings(limit)
                logger.success(f"✅ API Response: GET /api/security/videos - Returning {len(videos)} videos")
                return JSONResponse(content=videos)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/videos - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/videos/{video_id}")
        async def get_video_details(video_id: str):
            """Get detailed metadata for specific video."""
            logger.info(f"📹 API Request: GET /api/security/videos/{video_id} - Retrieving video details")
            try:
                video_details = await self._get_video_details(video_id)
                if not video_details:
                    logger.warning(f"⚠️ API Response: GET /api/security/videos/{video_id} - Video not found")
                    raise HTTPException(status_code=404, detail="Video not found")
                logger.success(f"✅ API Response: GET /api/security/videos/{video_id} - Video details retrieved, threat level: {video_details.get('threatLevel', 'N/A')}")
                return JSONResponse(content=video_details)
            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/videos/{video_id} - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/analysis/{session_id}")
        async def get_analysis_session(session_id: str):
            """Get complete analysis session with frame-by-frame details."""
            logger.info(f"🔬 API Request: GET /api/security/analysis/{session_id} - Retrieving analysis session")
            try:
                analysis_session = await self._get_analysis_session(session_id)
                if not analysis_session:
                    logger.warning(f"⚠️ API Response: GET /api/security/analysis/{session_id} - Analysis session not found")
                    raise HTTPException(status_code=404, detail="Analysis session not found")
                logger.success(f"✅ API Response: GET /api/security/analysis/{session_id} - Analysis session retrieved with {len(analysis_session.get('batch_analyses', []))} batch analyses")
                return JSONResponse(content=analysis_session)
            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/analysis/{session_id} - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/cameras")
        async def get_camera_status():
            """Get camera status and system health."""
            logger.info("📷 API Request: GET /api/security/cameras - Retrieving camera status")
            try:
                camera_status = await self._get_camera_status()
                logger.success(f"✅ API Response: GET /api/security/cameras - Returning status for {len(camera_status)} cameras")
                return JSONResponse(content=camera_status)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/cameras - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/security/cameras/config")
        async def get_camera_config():
            """Get camera configuration including RTSP URLs."""
            logger.info("⚙️ API Request: GET /api/security/cameras/config - Retrieving camera configuration")
            try:
                camera_config = await self._get_camera_config()
                logger.success(f"✅ API Response: GET /api/security/cameras/config - Configuration retrieved")
                return JSONResponse(content=camera_config)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/security/cameras/config - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/system/health")
        async def get_system_health():
            """Get overall system health status for mobile app."""
            logger.info("💚 API Request: GET /api/system/health - Retrieving system health")
            try:
                system_health = await self._get_system_health()
                logger.success(f"✅ API Response: GET /api/system/health - System status: {system_health.get('status', 'unknown')}, healthy: {system_health.get('isHealthy', False)}")
                return JSONResponse(content=system_health)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/system/health - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.post("/api/system/cleanup")
        async def trigger_cleanup(backup: bool = True):
            """Manually trigger system cleanup of recordings and analysis logs."""
            logger.info(f"🧹 API Request: POST /api/system/cleanup - backup={backup}")
            try:
                cleanup_result = await self._manual_cleanup(backup=backup)
                if cleanup_result["success"]:
                    logger.success(f"✅ API Response: POST /api/system/cleanup - Cleanup successful, freed {cleanup_result['cleanup_stats']['storage_freed_mb']:.2f} MB")
                else:
                    logger.error(f"❌ API Response: POST /api/system/cleanup - Cleanup failed: {cleanup_result.get('error', 'Unknown error')}")
                return JSONResponse(content=cleanup_result)
            except Exception as e:
                logger.error(f"❌ API Error: POST /api/system/cleanup - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/api/system/cleanup/status")
        async def get_cleanup_status():
            """Get status of daily cleanup system."""
            logger.info("🕐 API Request: GET /api/system/cleanup/status - Retrieving cleanup status")
            try:
                status = {
                    "cleanupEnabled": self.cleanup_enabled,
                    "cleanupTime": self.cleanup_time,
                    "lastCleanupDate": self.last_cleanup_date.isoformat() if self.last_cleanup_date else None,
                    "schedulerRunning": self.cleanup_thread is not None and self.cleanup_thread.is_alive(),
                    "nextCleanup": "Midnight (00:00)" if self.cleanup_enabled else "Disabled",
                    "currentTime": datetime.now().isoformat() + "Z"
                }
                logger.success(f"✅ API Response: GET /api/system/cleanup/status - Cleanup enabled: {status['cleanupEnabled']}, last cleanup: {status['lastCleanupDate'] or 'Never'}")
                return JSONResponse(content=status)
            except Exception as e:
                logger.error(f"❌ API Error: GET /api/system/cleanup/status - {e}")
                raise HTTPException(status_code=500, detail=str(e))
        
        @self.app.get("/video/{filename}")
        async def serve_video_file(filename: str):
            """Serve video files directly for streaming to mobile app."""
            logger.info(f"🎬 API Request: GET /video/{filename} - Serving video file")
            try:
                # Security check: prevent directory traversal
                if ".." in filename or "/" in filename or "\\" in filename:
                    logger.warning(f"⚠️ Security: Rejected potentially malicious filename: {filename}")
                    raise HTTPException(status_code=400, detail="Invalid filename")
                
                # Look for video file in recordings directory
                video_path = self.recordings_dir / filename
                
                if not video_path.exists():
                    # Also check in analysis_logs directory for processed videos
                    video_path = self.analysis_logs_dir / filename
                
                if not video_path.exists():
                    logger.warning(f"⚠️ Video file not found: {filename}")
                    raise HTTPException(status_code=404, detail="Video file not found")
                
                # Check if it's actually a video file
                video_extensions = {'.mp4', '.avi', '.mov', '.mkv', '.webm', '.m4v'}
                if video_path.suffix.lower() not in video_extensions:
                    logger.warning(f"⚠️ Not a video file: {filename}")
                    raise HTTPException(status_code=400, detail="Not a video file")
                
                logger.success(f"✅ Serving video file: {video_path} (size: {video_path.stat().st_size} bytes)")
                
                # Return video file with appropriate headers for streaming
                return FileResponse(
                    path=str(video_path),
                    media_type="video/mp4",
                    headers={
                        "Accept-Ranges": "bytes",
                        "Cache-Control": "no-cache",
                        "Access-Control-Allow-Origin": "*"
                    }
                )
                
            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"❌ API Error: GET /video/{filename} - {e}")
                raise HTTPException(status_code=500, detail=str(e))
    
    async def _get_security_statistics(self) -> Dict[str, Any]:
        """Get real-time security statistics from stored data."""
        try:
            # Load security analyses
            analyses_file = self.data_dir / "security_analyses.json"
            if not analyses_file.exists():
                return self._empty_stats()
            
            with open(analyses_file, 'r', encoding='utf-8') as f:
                analyses = json.load(f)
            
            # Calculate statistics from last 24 hours
            now = datetime.now()
            last_24h = now - timedelta(hours=24)
            
            total_alerts = len(analyses)
            recent_analyses = [a for a in analyses if datetime.fromisoformat(a['timestamp']) > last_24h]
            
            # Count by threat level
            threat_counts = {"low": 0, "medium": 0, "high": 0, "critical": 0}
            for analysis in recent_analyses:
                level = analysis.get('threat_level', 'low').lower()
                threat_counts[level] = threat_counts.get(level, 0) + 1
            
            last_alert_time = None
            if analyses:
                last_alert_time = max(a['timestamp'] for a in analyses)
            
            return {
                "totalAlerts": total_alerts,
                "criticalAlerts": threat_counts["critical"],
                "highAlerts": threat_counts["high"],
                "mediumAlerts": threat_counts["medium"],
                "lowAlerts": threat_counts["low"],
                "lastAlertTime": last_alert_time,
                "recentAlerts24h": len(recent_analyses),
                "systemStatus": "active",
                "lastUpdated": datetime.now().isoformat() + "Z"
            }
            
        except Exception as e:
            logger.error(f"❌ Error calculating security statistics: {e}")
            return self._empty_stats()
    
    def _empty_stats(self) -> Dict[str, Any]:
        """Return empty statistics when no data is available."""
        return {
            "totalAlerts": 0,
            "criticalAlerts": 0,
            "highAlerts": 0,
            "mediumAlerts": 0,
            "lowAlerts": 0,
            "lastAlertTime": None,
            "recentAlerts24h": 0,
            "systemStatus": "active",
            "lastUpdated": datetime.now().isoformat() + "Z"
        }
    
    async def _get_security_alerts(self, limit: int = 50, threat_level: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get security alerts with full analysis details."""
        try:
            # Load security analyses
            analyses_file = self.data_dir / "security_analyses.json"
            if not analyses_file.exists():
                return []
            
            with open(analyses_file, 'r', encoding='utf-8') as f:
                analyses = json.load(f)
            
            # Filter by threat level if specified
            if threat_level:
                analyses = [a for a in analyses if a.get('threat_level', '').lower() == threat_level.lower()]
            
            # Sort by timestamp (most recent first)
            analyses.sort(key=lambda x: x['timestamp'], reverse=True)
            
            # Limit results
            analyses = analyses[:limit]
            
            # Convert to mobile app format
            alerts = []
            for analysis in analyses:
                # Extract video filename from path
                video_path = analysis.get('video_file_path', '')
                video_filename = Path(video_path).name if video_path else None
                
                # Generate alert ID from video filename or timestamp
                alert_id = video_filename.replace('.mp4', '') if video_filename else f"alert_{analysis['timestamp']}"
                
                alert = {
                    "id": alert_id,
                    "timestamp": analysis['timestamp'],
                    "threatLevel": analysis['threat_level'].upper(),
                    "confidence": analysis['confidence'],
                    "isThreatDetected": analysis.get('threat_level', 'low') != 'low',
                    "summary": self._extract_summary(analysis['analysis_text']),
                    "keywords": analysis.get('keywords', []),
                    "description": analysis['analysis_text'],
                    "camera": self._extract_camera_name(video_path),
                    "isAcknowledged": False,  # TODO: Implement acknowledgment system
                    "videoClip": {
                        "id": alert_id,
                        "url": f"{self._get_base_url()}/video/{video_filename}" if video_filename else None,
                        "fileName": video_filename,
                        "filePath": video_path,
                        "thumbnailUrl": None,  # TODO: Generate thumbnails
                        "timestamp": analysis['timestamp'],
                        "duration": "120s"  # Default 2 minutes in Kotlin duration format
                    } if video_filename else None
                }
                alerts.append(alert)
            
            return alerts
            
        except Exception as e:
            logger.error(f"❌ Error retrieving security alerts from data files: {e}")
            return []
    
    async def _get_alert_details(self, alert_id: str) -> Optional[Dict[str, Any]]:
        """Get detailed analysis for specific alert."""
        try:
            # Find corresponding analysis session
            analysis_files = list(self.analysis_logs_dir.glob("*.json"))
            
            for analysis_file in analysis_files:
                try:
                    with open(analysis_file, 'r', encoding='utf-8') as f:
                        session_data = json.load(f)
                    
                    # Check if this session matches the alert
                    video_path = session_data.get('video_path', '')
                    video_filename = Path(video_path).name if video_path else ''
                    session_alert_id = video_filename.replace('.mp4', '') if video_filename else ''
                    
                    if session_alert_id == alert_id:
                        # Found matching session, return detailed analysis
                        return {
                            "alertId": alert_id,
                            "analysisSession": json.dumps(session_data),
                            "batchAnalyses": json.dumps(session_data.get('batch_analyses', [])),
                            "consolidatedAnalysis": session_data.get('consolidated_analysis', ''),
                            "framesAnalyzed": json.dumps(session_data.get('frames_analyzed', {})),
                            "modelUsed": session_data.get('model_used', ''),
                            "processingMethod": session_data.get('processing_method', ''),
                            "analysisPrompt": session_data.get('analysis_prompt', '')
                        }
                        
                except Exception as e:
                    logger.warning(f"⚠️ Error reading analysis file {analysis_file.name}: {e}")
                    continue
            
            return None
            
        except Exception as e:
            logger.error(f"❌ Error retrieving alert details for {alert_id}: {e}")
            return None
    
    async def _get_video_recordings(self, limit: int = 20) -> List[Dict[str, Any]]:
        """Get list of video recordings with metadata."""
        try:
            video_files = list(self.recordings_dir.glob("*.mp4"))
            video_files.sort(key=lambda x: x.stat().st_mtime, reverse=True)  # Sort by modification time
            video_files = video_files[:limit]
            
            videos = []
            for video_file in video_files:
                file_stat = video_file.stat()
                video_id = video_file.stem
                
                # Try to find corresponding analysis
                threat_level = "LOW"  # Default
                confidence = 0.0
                analysis_text = ""
                
                # Load security analyses to find matching video
                analyses_file = self.data_dir / "security_analyses.json"
                if analyses_file.exists():
                    with open(analyses_file, 'r', encoding='utf-8') as f:
                        analyses = json.load(f)
                    
                    for analysis in analyses:
                        if video_file.name in analysis.get('video_file_path', ''):
                            threat_level = analysis.get('threat_level', 'low').upper()
                            confidence = analysis.get('confidence', 0.0)
                            analysis_text = analysis.get('analysis_text', '')
                            break
                
                video_data = {
                    "id": video_id,
                    "fileName": video_file.name,
                    "filePath": str(video_file),
                    "url": f"{self._get_base_url()}/video/{video_file.name}",
                    "timestamp": datetime.fromtimestamp(file_stat.st_mtime).isoformat() + "Z",
                    "fileSize": file_stat.st_size,
                    "threatLevel": threat_level,
                    "confidence": confidence,
                    "description": self._extract_summary(analysis_text) if analysis_text else "Security recording",
                    "camera": self._extract_camera_name(str(video_file)),
                    "duration": "120s",  # Default 2 minutes in Kotlin duration format
                    "resolution": "1920x1080",  # Default, TODO: Get actual resolution
                    "fps": 30  # Default, TODO: Get actual FPS
                }
                videos.append(video_data)
            
            return videos
            
        except Exception as e:
            logger.error(f"❌ Error retrieving video recordings from {self.recordings_dir}: {e}")
            return []
    
    async def _get_video_details(self, video_id: str) -> Optional[Dict[str, Any]]:
        """Get detailed metadata for specific video."""
        try:
            video_file = self.recordings_dir / f"{video_id}.mp4"
            if not video_file.exists():
                return None
            
            file_stat = video_file.stat()
            
            # Find corresponding analysis session
            analysis_session = None
            analysis_files = list(self.analysis_logs_dir.glob("*.json"))
            
            for analysis_file in analysis_files:
                try:
                    with open(analysis_file, 'r', encoding='utf-8') as f:
                        session_data = json.load(f)
                    
                    if video_file.name in session_data.get('video_path', ''):
                        analysis_session = session_data
                        break
                        
                except Exception as e:
                    continue
            
            # Find analysis data
            threat_level = "LOW"
            confidence = 0.0
            analysis_text = ""
            keywords = []
            
            analyses_file = self.data_dir / "security_analyses.json"
            if analyses_file.exists():
                with open(analyses_file, 'r', encoding='utf-8') as f:
                    analyses = json.load(f)
                
                for analysis in analyses:
                    if video_file.name in analysis.get('video_file_path', ''):
                        threat_level = analysis.get('threat_level', 'low').upper()
                        confidence = analysis.get('confidence', 0.0)
                        analysis_text = analysis.get('analysis_text', '')
                        keywords = analysis.get('keywords', [])
                        break
            
            # Get frame analysis details
            frame_analyses = []
            if analysis_session:
                batch_analyses = analysis_session.get('batch_analyses', [])
                for batch in batch_analyses:
                    frame_range = batch.get('frame_range', {})
                    timestamp_range = batch.get('timestamp_range', {})
                    
                    frame_analyses.append({
                        "batchNumber": batch.get('batch_number', 0),
                        "frameRange": frame_range,
                        "timestampRange": timestamp_range,
                        "analysis": batch.get('batch_summary', ''),
                        "framesInBatch": batch.get('frames_in_batch', 0)
                    })
            
            video_details = {
                "id": video_id,
                "fileName": video_file.name,
                "filePath": str(video_file),
                "url": f"{self._get_base_url()}/video/{video_file.name}",
                "timestamp": datetime.fromtimestamp(file_stat.st_mtime).isoformat() + "Z",
                "fileSize": file_stat.st_size,
                "threatLevel": threat_level,
                "confidence": confidence,
                "description": analysis_text,
                "summary": self._extract_summary(analysis_text) if analysis_text else "Security recording",
                "keywords": keywords,
                "camera": self._extract_camera_name(str(video_file)),
                "duration": "120s",  # Default 2 minutes in Kotlin duration format
                "resolution": "1920x1080",  # TODO: Get actual resolution
                "fps": 30,  # TODO: Get actual FPS
                "analysisSession": json.dumps(analysis_session) if analysis_session else "",
                "frameAnalyses": frame_analyses,
                "consolidatedAnalysis": analysis_session.get('consolidated_analysis', '') if analysis_session else ''
            }
            
            return video_details
            
        except Exception as e:
            logger.error(f"❌ Error retrieving video details for {video_id}: {e}")
            return None
    
    async def _get_analysis_session(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Get complete analysis session details."""
        try:
            analysis_file = self.analysis_logs_dir / f"{session_id}.json"
            if analysis_file.exists():
                with open(analysis_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            
            # If exact match not found, search for session containing this ID
            analysis_files = list(self.analysis_logs_dir.glob("*.json"))
            for analysis_file in analysis_files:
                if session_id in analysis_file.stem:
                    with open(analysis_file, 'r', encoding='utf-8') as f:
                        return json.load(f)
            
            return None
            
        except Exception as e:
            logger.error(f"❌ Error retrieving analysis session {session_id}: {e}")
            return None
    
    async def _get_camera_status(self) -> List[Dict[str, Any]]:
        """Get camera status and system health."""
        try:
            # For now, return basic system status
            # TODO: Implement actual camera health monitoring
            return [
                {
                    "id": "camera_001",
                    "name": "Security Camera 1",
                    "location": "Front Door",
                    "status": "active",
                    "isOnline": True,
                    "lastSeen": datetime.now().isoformat() + "Z",
                    "resolution": "1920x1080",
                    "fps": 30,
                    "storageUsed": "45.2 GB",
                    "recordingEnabled": True
                }
            ]
            
        except Exception as e:
            logger.error(f"❌ Error retrieving camera status: {e}")
            return []
    
    async def _get_camera_config(self) -> Dict[str, Any]:
        """Get camera configuration including RTSP URLs."""
        try:
            # Read RTSP URL from environment or .env file
            rtsp_url = os.getenv('RTSP_URL', 'rtsp://admin:admin@192.168.0.100:554/ch0_0.264')
            
            # If no env var, try to read from .env file
            if rtsp_url == 'rtsp://admin:admin@192.168.0.100:554/ch0_0.264':
                env_path = Path('.env')
                if env_path.exists():
                    with open(env_path, 'r') as f:
                        for line in f:
                            if line.startswith('RTSP_URL='):
                                rtsp_url = line.split('=', 1)[1].strip()
                                break
            
            return {
                "cameras": [
                    {
                        "id": "camera_001",
                        "name": "Security Camera 1",
                        "location": "Front Door",
                        "rtspUrl": rtsp_url,
                        "streamFormat": "H.264",
                        "resolution": "1920x1080",
                        "fps": 30,
                        "isActive": True,
                        "supportsAudio": False,
                        "motionDetection": True,
                        "nightVision": True
                    }
                ],
                "streamSettings": {
                    "bufferSize": 1024,
                    "timeout": 5000,
                    "retryAttempts": 3,
                    "qualityPresets": ["low", "medium", "high", "original"]
                }
            }
            
        except Exception as e:
            logger.error(f"❌ Error retrieving camera configuration: {e}")
            return {"cameras": [], "streamSettings": {}}
    
    async def _get_system_health(self) -> Dict[str, Any]:
        """Get overall system health for mobile app."""
        try:
            # Get security stats to determine system status
            stats = await self._get_security_statistics()
            
            # Calculate storage usage
            storage_stats = self._calculate_storage_usage()
            
            # Determine if system is healthy based on various factors
            is_healthy = True
            status = "online"
            
            # Check if we have recent data
            if stats.get("lastUpdated"):
                last_updated = datetime.fromisoformat(stats["lastUpdated"].replace('Z', '+00:00') if 'Z' in stats["lastUpdated"] else stats["lastUpdated"])
                time_since_update = datetime.now() - last_updated.replace(tzinfo=None)
                if time_since_update.total_seconds() > 300:  # 5 minutes
                    is_healthy = False
                    status = "offline"
            
            return {
                "status": status,
                "isHealthy": is_healthy,
                "timestamp": datetime.now().isoformat() + "Z",
                "systemUptime": "running",
                "camerasOnline": 1,
                "totalCameras": 1,
                "recordingStatus": "active",
                "storageUsed": storage_stats["total_mb"],
                "recordingsCount": storage_stats["recordings_count"],
                "analysisLogsCount": storage_stats["analysis_logs_count"],
                "framesCount": storage_stats["frames_count"],
                "lastAlert": stats.get("lastAlertTime"),
                "alertsToday": stats.get("recentAlerts24h", 0),
                "cleanupEnabled": self.cleanup_enabled,
                "lastCleanup": self.last_cleanup_date.isoformat() if self.last_cleanup_date else None
            }
            
        except Exception as e:
            logger.error(f"❌ Error retrieving system health: {e}")
            return {
                "status": "error",
                "isHealthy": False,
                "timestamp": datetime.now().isoformat() + "Z",
                "error": str(e)
            }
    
    def _calculate_storage_usage(self) -> Dict[str, Any]:
        """Calculate current storage usage across all directories."""
        try:
            storage_stats = {
                "total_mb": 0,
                "recordings_mb": 0,
                "analysis_logs_mb": 0,
                "frames_mb": 0,
                "recordings_count": 0,
                "analysis_logs_count": 0,
                "frames_count": 0
            }
            
            # Calculate recordings storage
            if self.recordings_dir.exists():
                for video_file in self.recordings_dir.glob("*.mp4"):
                    file_size_mb = video_file.stat().st_size / (1024 * 1024)
                    storage_stats["recordings_mb"] += file_size_mb
                    storage_stats["recordings_count"] += 1
            
            # Calculate analysis logs storage
            if self.analysis_logs_dir.exists():
                for log_file in self.analysis_logs_dir.glob("*.json"):
                    file_size_mb = log_file.stat().st_size / (1024 * 1024)
                    storage_stats["analysis_logs_mb"] += file_size_mb
                    storage_stats["analysis_logs_count"] += 1
            
            # Calculate frames storage
            if self.frames_dir.exists():
                for frame_file in self.frames_dir.glob("*"):
                    if frame_file.is_file():
                        file_size_mb = frame_file.stat().st_size / (1024 * 1024)
                        storage_stats["frames_mb"] += file_size_mb
                        storage_stats["frames_count"] += 1
            
            # Calculate total
            storage_stats["total_mb"] = (
                storage_stats["recordings_mb"] + 
                storage_stats["analysis_logs_mb"] + 
                storage_stats["frames_mb"]
            )
            
            return storage_stats
            
        except Exception as e:
            logger.error(f"❌ Error calculating storage usage: {e}")
            return {
                "total_mb": 0,
                "recordings_mb": 0,
                "analysis_logs_mb": 0,
                "frames_mb": 0,
                "recordings_count": 0,
                "analysis_logs_count": 0,
                "frames_count": 0
            }
    
    def _extract_summary(self, analysis_text: str) -> str:
        """Extract a short summary from analysis text."""
        if not analysis_text:
            return ""
        
        # Take first sentence or first 100 characters
        sentences = analysis_text.split('.')
        if sentences:
            summary = sentences[0].strip()
            if len(summary) > 100:
                summary = summary[:100] + "..."
            return summary
        
        return analysis_text[:100] + "..." if len(analysis_text) > 100 else analysis_text
    
    def _extract_camera_name(self, file_path: str) -> str:
        """Extract camera name from file path."""
        # Default camera name extraction logic
        if "front" in file_path.lower():
            return "Front Door Camera"
        elif "back" in file_path.lower():
            return "Backyard Camera"
        elif "side" in file_path.lower():
            return "Side Gate Camera"
        else:
            return "Security Camera"
    
    async def start_server(self, host: str = "0.0.0.0", port: int = 8888):
        """Start the API server."""
        # Store host and port for URL generation
        self.host = host
        self.port = port
        
        logger.info(f"🚀 Starting Security Data API server on {host}:{port}")
        logger.info(f"📡 API Endpoints available at http://{self._get_server_ip()}:{port}")
        logger.info(f"🔗 Health check: http://{self._get_server_ip()}:{port}/health")
        logger.info(f"📊 Security stats: http://{self._get_server_ip()}:{port}/api/security/stats")
        logger.info(f"🚨 Security alerts: http://{self._get_server_ip()}:{port}/api/security/alerts")
        
        config = uvicorn.Config(
            self.app,
            host=host,
            port=port,
            log_level="info",
            access_log=True
        )
        
        server = uvicorn.Server(config)
        await server.serve()


async def main():
    """Main function to start the API server."""
    api = SecurityDataAPI()
    await api.start_server()


if __name__ == "__main__":
    asyncio.run(main())
