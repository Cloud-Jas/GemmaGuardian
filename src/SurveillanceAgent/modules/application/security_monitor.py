"""Main security monitoring orchestrator."""

import time
import signal
import sys
import asyncio
from threading import Thread, Event
from datetime import datetime
from typing import Optional
from loguru import logger
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.application.use_cases import (
    PersonDetectionUseCase, VideoRecordingUseCase, 
    SecurityAnalysisUseCase, MonitoringSessionUseCase
)
from modules.domain.services import INotificationService
from modules.infrastructure.rtsp_stream import RTSPStreamHandler
from modules.interfaces.visual_monitor import VisualSecurityMonitor


class SecurityMonitor:
    """Main orchestrator for the security monitoring system."""
    
    def __init__(self,
                 rtsp_url: str,
                 person_detection_use_case: PersonDetectionUseCase,
                 video_recording_use_case: VideoRecordingUseCase,
                 security_analysis_use_case: SecurityAnalysisUseCase,
                 session_use_case: MonitoringSessionUseCase,
                 notification_service: Optional[INotificationService] = None,
                 clip_duration: float = 60.0,
                 enable_visual_monitor: bool = False):
        """Initialize the security monitor.
        
        Args:
            rtsp_url: RTSP stream URL
            person_detection_use_case: Person detection use case
            video_recording_use_case: Video recording use case  
            security_analysis_use_case: Security analysis use case
            session_use_case: Monitoring session use case
            notification_service: Mobile notification service
            clip_duration: Duration of security clips in seconds
            enable_visual_monitor: Whether to show live preview window
        """
        self.rtsp_url = rtsp_url
        self.person_detection_use_case = person_detection_use_case
        self.video_recording_use_case = video_recording_use_case
        self.security_analysis_use_case = security_analysis_use_case
        self.session_use_case = session_use_case
        self.notification_service = notification_service
        self.clip_duration = clip_duration
        self.enable_visual_monitor = enable_visual_monitor
        
        # Control flags
        self._stop_event = Event()
        self._monitoring_thread: Optional[Thread] = None
        self._analysis_thread: Optional[Thread] = None
        
        # State tracking for sequential processing
        self._recording_in_progress = False
        self._analysis_in_progress = False
        self._last_detection_time = 0
        self._min_detection_interval = 5.0  # Minimum seconds between detections while processing
        
        # State
        self.stream_handler: Optional[RTSPStreamHandler] = None
        self.pending_analyses = []
        self.visual_monitor: Optional[VisualSecurityMonitor] = None
        self.latest_analysis = "System Ready"
        self.latest_threat_level = None
        
        # Initialize visual monitor if enabled
        if self.enable_visual_monitor:
            self.visual_monitor = VisualSecurityMonitor()
            logger.info("Visual monitoring enabled")
        
        # Setup signal handlers for graceful shutdown
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def start_monitoring(self) -> bool:
        """Start the security monitoring system.
        
        Returns:
            True if started successfully, False otherwise
        """
        try:
            logger.info("Starting Security Monitoring System...")
            
            # Start monitoring session
            session = self.session_use_case.start_monitoring_session()
            
            # Initialize RTSP stream
            self.stream_handler = RTSPStreamHandler(self.rtsp_url)
            if not self.stream_handler.connect():
                logger.error("Failed to connect to RTSP stream")
                return False
            
            # Start monitoring thread
            self._monitoring_thread = Thread(
                target=self._monitoring_loop,
                name="SecurityMonitoring",
                daemon=True
            )
            self._monitoring_thread.start()
            
            # Start analysis thread
            self._analysis_thread = Thread(
                target=self._analysis_loop,
                name="SecurityAnalysis", 
                daemon=True
            )
            self._analysis_thread.start()
            
            logger.success("Security monitoring system started successfully")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start monitoring system: {e}")
            self.stop_monitoring()
            return False
    
    def stop_monitoring(self) -> None:
        """Stop the security monitoring system."""
        logger.info("Stopping Security Monitoring System...")
        
        # Set stop event
        self._stop_event.set()
        
        # Wait for threads to finish
        if self._monitoring_thread and self._monitoring_thread.is_alive():
            self._monitoring_thread.join(timeout=5)
        
        if self._analysis_thread and self._analysis_thread.is_alive():
            self._analysis_thread.join(timeout=5)
        
        # Disconnect stream
        if self.stream_handler:
            self.stream_handler.disconnect()
        
        # End monitoring session
        self.session_use_case.end_monitoring_session()
        
        logger.info("Security monitoring system stopped")
    
    def _monitoring_loop(self) -> None:
        """Main monitoring loop that processes video frames."""
        logger.info("Starting monitoring loop...")
        
        frame_count = 0
        last_log_time = time.time()
        
        try:
            while not self._stop_event.is_set():
                # Get frame from stream
                frame = self.stream_handler.get_frame()
                
                if frame is None:
                    time.sleep(0.1)  # Brief pause if no frame
                    continue
                
                frame_count += 1
                
                # Process frame for person detection
                detections = self.person_detection_use_case.process_frame(frame)
                
                # Update visual monitor if enabled
                if self.enable_visual_monitor and self.visual_monitor:
                    status_info = self.get_status()
                    status_info.update({
                        'recording': self.video_recording_use_case.recording_service.is_recording_active(),
                        'current_analysis': self.latest_analysis,
                        'last_threat_level': self.latest_threat_level
                    })
                    
                    # Display frame with detections
                    if not self.visual_monitor.display_frame(frame, detections, status_info):
                        logger.info("Visual monitor window closed by user")
                        self._stop_event.set()
                        break
                
                # If persons detected, check if we can process them
                if detections:
                    self._handle_person_detections(detections)
                
                # Log status periodically
                current_time = time.time()
                if current_time - last_log_time >= 60:  # Every minute
                    logger.info(f"Monitoring active - Processed {frame_count} frames")
                    last_log_time = current_time
                    frame_count = 0
                
                # Small delay to prevent excessive CPU usage
                time.sleep(0.03)  # ~30 FPS processing
                
        except Exception as e:
            logger.error(f"Error in monitoring loop: {e}")
        finally:
            logger.info("Monitoring loop ended")
    
    def _analysis_loop(self) -> None:
        """Analysis loop that processes recorded clips."""
        logger.info("Starting analysis loop...")
        
        try:
            while not self._stop_event.is_set():
                if self.pending_analyses:
                    # Process pending analysis
                    video_clip = self.pending_analyses.pop(0)
                    
                    logger.info(f"Processing analysis for clip: {video_clip.file_path}")
                    
                    # Perform security analysis
                    analysis = self.security_analysis_use_case.analyze_security_clip(video_clip)
                    
                    if analysis:
                        # Add to session
                        self.session_use_case.add_analysis_to_session(analysis)
                        
                        # Update latest analysis for visual display
                        self.latest_analysis = analysis.analysis_text
                        self.latest_threat_level = analysis.threat_level
                        
                        # Update visual monitor if enabled
                        if self.enable_visual_monitor and self.visual_monitor:
                            self.visual_monitor.update_analysis(
                                analysis.analysis_text, 
                                analysis.threat_level
                            )
                        
                        # Log significant threats
                        if analysis.is_threat_detected:
                            self._handle_security_threat(analysis)
                    
                    # Clear analysis flag - ready for next detection
                    self._analysis_in_progress = False
                    logger.info("Analysis completed - ready for next detection")
                
                else:
                    # No pending analyses, wait a bit
                    time.sleep(1)
                    
        except Exception as e:
            logger.error(f"Error in analysis loop: {e}")
        finally:
            logger.info("Analysis loop ended")
    
    def _handle_person_detections(self, detections) -> None:
        """Handle detected persons by triggering recording.
        
        Args:
            detections: List of person detections
        """
        try:
            current_time = time.time()
            
            # Check if we should skip this detection due to ongoing processing
            if self._recording_in_progress:
                logger.debug("Skipping detection - recording already in progress")
                return
                
            if self._analysis_in_progress:
                logger.debug("Skipping detection - analysis in progress")
                return
                
            # Check minimum interval since last detection
            if current_time - self._last_detection_time < self._min_detection_interval:
                logger.debug(f"Skipping detection - too soon (min interval: {self._min_detection_interval}s)")
                return
            
            # Use the first detection as trigger
            trigger_detection = detections[0]
            
            # Add detection to session
            for detection in detections:
                self.session_use_case.add_detection_to_session(detection)
            
            logger.info(f"Person detected with confidence {trigger_detection.confidence:.2f}")
            logger.info("Starting recording and analysis sequence...")
            
            # Set recording flag and update timestamp
            self._recording_in_progress = True
            self._last_detection_time = current_time
            
            # Record security clip
            video_clip = self.video_recording_use_case.record_security_clip(
                trigger_detection=trigger_detection,
                duration=self.clip_duration
            )
            
            # Clear recording flag
            self._recording_in_progress = False
            
            if video_clip:
                # Add clip to session
                self.session_use_case.add_clip_to_session(video_clip)
                
                # Queue for analysis and set analysis flag
                self._analysis_in_progress = True
                self.pending_analyses.append(video_clip)
                
                logger.info(f"Queued clip for analysis: {video_clip.file_path}")
            else:
                logger.warning("Failed to record security clip, resuming normal monitoring")
            
        except Exception as e:
            logger.error(f"Error handling person detections: {e}")
            # Reset flags on error
            self._recording_in_progress = False
            self._analysis_in_progress = False
    
    def _handle_security_threat(self, analysis) -> None:
        """Handle detected security threats.
        
        Args:
            analysis: Security analysis with detected threat
        """
        try:
            logger.warning(
                f"SECURITY ALERT - Threat Level: {analysis.threat_level.value.upper()}"
            )
            logger.warning(f"Video: {analysis.video_clip.file_path}")
            logger.warning(f"Analysis: {analysis.analysis_text}")
            logger.warning(f"Keywords: {', '.join(analysis.keywords)}")
            logger.warning(f"Confidence: {analysis.confidence:.2f}")
            
            # Send mobile notification if service is available
            if self.notification_service:
                try:
                    # Send notification asynchronously
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    
                    video_path = str(analysis.video_clip.file_path) if analysis.video_clip else None
                    notification_sent = loop.run_until_complete(
                        self.notification_service.send_security_alert(analysis, video_path)
                    )
                    
                    loop.close()
                    
                    if notification_sent:
                        logger.success("📱 Mobile notification sent successfully")
                    else:
                        logger.warning("📱 Failed to send mobile notification")
                        
                except Exception as notification_error:
                    logger.error(f"📱 Mobile notification error: {notification_error}")
            
            # Additional threat handling can be added here:
            # - Save high-priority clips
            # - Trigger physical alarms
            # - Send emails
            # - etc.
            
        except Exception as e:
            logger.error(f"Error handling security threat: {e}")
    
    def _signal_handler(self, signum, frame) -> None:
        """Handle shutdown signals gracefully.
        
        Args:
            signum: Signal number
            frame: Current stack frame
        """
        logger.info(f"Received signal {signum}, shutting down...")
        self.stop_monitoring()
        sys.exit(0)
    
    def is_running(self) -> bool:
        """Check if the monitoring system is running.
        
        Returns:
            True if running, False otherwise
        """
        return (self._monitoring_thread and self._monitoring_thread.is_alive() and
                self._analysis_thread and self._analysis_thread.is_alive() and
                not self._stop_event.is_set())
    
    def get_status(self) -> dict:
        """Get current system status.
        
        Returns:
            Dictionary with system status information
        """
        session = self.session_use_case.get_current_session()
        
        return {
            'running': self.is_running(),
            'rtsp_connected': self.stream_handler.is_connected if self.stream_handler else False,
            'recording_in_progress': self._recording_in_progress,
            'analysis_in_progress': self._analysis_in_progress,
            'current_session': session.session_id if session else None,
            'session_duration': session.duration if session and session.duration else 0,
            'total_detections': len(session.detections) if session else 0,
            'total_clips': len(session.video_clips) if session else 0,
            'total_analyses': len(session.analyses) if session else 0,
            'threat_count': session.threat_count if session else 0,
            'pending_analyses': len(self.pending_analyses)
        }
