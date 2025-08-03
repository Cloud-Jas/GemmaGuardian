"""Use cases for the security monitoring system."""

import time
from datetime import datetime, timedelta
from typing import List, Optional
from loguru import logger
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import (
    PersonDetection, VideoClip, SecurityAnalysis, 
    MonitoringSession, DetectionStatus, SecurityThreatLevel
)
from modules.domain.services import (
    IPersonDetectionService, IVideoRecordingService, ISecurityAnalysisService
)
from modules.domain.repositories import (
    IPersonDetectionRepository, IVideoClipRepository, 
    ISecurityAnalysisRepository, IMonitoringSessionRepository
)


class PersonDetectionUseCase:
    """Use case for person detection operations."""
    
    def __init__(self, 
                 detection_service: IPersonDetectionService,
                 detection_repository: IPersonDetectionRepository):
        """Initialize the person detection use case.
        
        Args:
            detection_service: Person detection service
            detection_repository: Detection data repository
        """
        self.detection_service = detection_service
        self.detection_repository = detection_repository
        self.last_detection_time: Optional[datetime] = None
        self.detection_cooldown = timedelta(seconds=5)  # Prevent duplicate detections
    
    def process_frame(self, frame) -> List[PersonDetection]:
        """Process a frame for person detection.
        
        Args:
            frame: Video frame to process
            
        Returns:
            List of valid person detections
        """
        try:
            # Detect persons in frame
            detections = self.detection_service.detect_persons(frame)
            
            # Filter valid detections
            valid_detections = [
                detection for detection in detections
                if self.detection_service.is_detection_valid(detection)
            ]
            
            # Apply cooldown logic to prevent spam
            if valid_detections and self._should_process_detection():
                # Save detections
                for detection in valid_detections:
                    self.detection_repository.save_detection(detection)
                
                self.last_detection_time = datetime.now()
                logger.info(f"Processed {len(valid_detections)} person detections")
                
                return valid_detections
            
            return []
            
        except Exception as e:
            logger.error(f"Error processing frame for person detection: {e}")
            return []
    
    def _should_process_detection(self) -> bool:
        """Check if enough time has passed since last detection.
        
        Returns:
            True if detection should be processed, False otherwise
        """
        if self.last_detection_time is None:
            return True
        
        time_since_last = datetime.now() - self.last_detection_time
        return time_since_last >= self.detection_cooldown
    
    def get_recent_detections(self, minutes: int = 60) -> List[PersonDetection]:
        """Get recent person detections.
        
        Args:
            minutes: Number of minutes to look back
            
        Returns:
            List of recent detections
        """
        end_time = datetime.now()
        start_time = end_time - timedelta(minutes=minutes)
        
        return self.detection_repository.get_detections_by_timeframe(
            start_time, end_time
        )


class VideoRecordingUseCase:
    """Use case for video recording operations."""
    
    def __init__(self,
                 recording_service: IVideoRecordingService,
                 video_repository: IVideoClipRepository):
        """Initialize the video recording use case.
        
        Args:
            recording_service: Video recording service
            video_repository: Video clip repository
        """
        self.recording_service = recording_service
        self.video_repository = video_repository
    
    def record_security_clip(self, 
                           trigger_detection: PersonDetection,
                           duration: float = 60.0) -> Optional[VideoClip]:
        """Record a security clip triggered by person detection.
        
        Args:
            trigger_detection: The detection that triggered recording
            duration: Duration of the clip in seconds
            
        Returns:
            VideoClip if successful, None otherwise
        """
        try:
            if self.recording_service.is_recording_active():
                logger.warning("Recording already active, skipping new recording")
                return None
            
            # Generate output path
            timestamp = trigger_detection.timestamp.strftime("%Y%m%d_%H%M%S_%f")[:-3]
            output_path = f"./recordings/security_clip_{timestamp}.mp4"
            
            logger.info(f"Starting security recording triggered by detection at {trigger_detection.timestamp}")
            
            # Record the clip
            video_clip = self.recording_service.record_clip(
                duration=duration,
                output_path=output_path
            )
            
            if video_clip:
                # Update clip with trigger detection
                video_clip.trigger_detection = trigger_detection
                
                # Save to repository
                self.video_repository.save_clip(video_clip)
                
                logger.success(f"Security clip recorded: {video_clip.file_path}")
                return video_clip
            else:
                logger.error("Failed to record security clip")
                return None
                
        except Exception as e:
            logger.error(f"Error recording security clip: {e}")
            return None
    
    def cleanup_old_clips(self, max_age_days: int = 7) -> None:
        """Clean up old video clips.
        
        Args:
            max_age_days: Maximum age of clips to keep
        """
        try:
            # This would typically involve querying the repository
            # and calling the recording service cleanup method
            logger.info(f"Cleaning up clips older than {max_age_days} days")
            # Implementation would depend on repository interface
            
        except Exception as e:
            logger.error(f"Error during clip cleanup: {e}")


class SecurityAnalysisUseCase:
    """Use case for security analysis operations."""
    
    def __init__(self,
                 analysis_service: ISecurityAnalysisService,
                 analysis_repository: ISecurityAnalysisRepository):
        """Initialize the security analysis use case.
        
        Args:
            analysis_service: Security analysis service
            analysis_repository: Analysis repository
        """
        self.analysis_service = analysis_service
        self.analysis_repository = analysis_repository
    
    def analyze_security_clip(self, video_clip: VideoClip) -> Optional[SecurityAnalysis]:
        """Analyze a video clip for security concerns.
        
        Args:
            video_clip: Video clip to analyze
            
        Returns:
            SecurityAnalysis if successful, None otherwise
        """
        try:
            logger.info(f"Starting security analysis of clip: {video_clip.file_path}")
            
            # Perform AI analysis
            analysis = self.analysis_service.analyze_video(video_clip)
            
            if analysis:
                # Save analysis result
                self.analysis_repository.save_analysis(analysis)
                
                # Log threat level
                if analysis.is_threat_detected:
                    logger.warning(
                        f"Security threat detected! Level: {analysis.threat_level.value}, "
                        f"Confidence: {analysis.confidence:.2f}"
                    )
                else:
                    logger.info(f"No security threats detected. Analysis confidence: {analysis.confidence:.2f}")
                
                return analysis
            else:
                logger.error("Failed to analyze video clip")
                return None
                
        except Exception as e:
            logger.error(f"Error during security analysis: {e}")
            return None
    
    def get_threat_analyses(self, 
                          min_threat_level: SecurityThreatLevel = SecurityThreatLevel.MEDIUM) -> List[SecurityAnalysis]:
        """Get analyses with threat level above threshold.
        
        Args:
            min_threat_level: Minimum threat level to include
            
        Returns:
            List of threat analyses
        """
        try:
            return self.analysis_repository.get_analyses_by_threat_level(min_threat_level)
        except Exception as e:
            logger.error(f"Error retrieving threat analyses: {e}")
            return []


class MonitoringSessionUseCase:
    """Use case for monitoring session management."""
    
    def __init__(self,
                 session_repository: IMonitoringSessionRepository):
        """Initialize the monitoring session use case.
        
        Args:
            session_repository: Session repository
        """
        self.session_repository = session_repository
        self.current_session: Optional[MonitoringSession] = None
    
    def start_monitoring_session(self) -> MonitoringSession:
        """Start a new monitoring session.
        
        Returns:
            New monitoring session
        """
        try:
            # End any existing session
            if self.current_session and not self.current_session.end_time:
                self.end_monitoring_session()
            
            # Create new session
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            
            self.current_session = MonitoringSession(
                session_id=session_id,
                start_time=datetime.now(),
                end_time=None,
                detections=[],
                video_clips=[],
                analyses=[],
                status=DetectionStatus.IN_PROGRESS
            )
            
            # Save session
            self.session_repository.save_session(self.current_session)
            
            logger.info(f"Started monitoring session: {session_id}")
            return self.current_session
            
        except Exception as e:
            logger.error(f"Error starting monitoring session: {e}")
            raise
    
    def end_monitoring_session(self) -> Optional[MonitoringSession]:
        """End the current monitoring session.
        
        Returns:
            Ended session if successful, None otherwise
        """
        try:
            if not self.current_session:
                logger.warning("No active session to end")
                return None
            
            self.current_session.end_time = datetime.now()
            self.current_session.status = DetectionStatus.COMPLETED
            
            # Update session in repository
            self.session_repository.update_session(self.current_session)
            
            logger.info(
                f"Ended monitoring session: {self.current_session.session_id}, "
                f"Duration: {self.current_session.duration:.1f}s, "
                f"Detections: {len(self.current_session.detections)}, "
                f"Threats: {self.current_session.threat_count}"
            )
            
            ended_session = self.current_session
            self.current_session = None
            
            return ended_session
            
        except Exception as e:
            logger.error(f"Error ending monitoring session: {e}")
            return None
    
    def add_detection_to_session(self, detection: PersonDetection) -> None:
        """Add a detection to the current session.
        
        Args:
            detection: Person detection to add
        """
        if self.current_session:
            self.current_session.detections.append(detection)
            self.session_repository.update_session(self.current_session)
    
    def add_clip_to_session(self, video_clip: VideoClip) -> None:
        """Add a video clip to the current session.
        
        Args:
            video_clip: Video clip to add
        """
        if self.current_session:
            self.current_session.video_clips.append(video_clip)
            self.session_repository.update_session(self.current_session)
    
    def add_analysis_to_session(self, analysis: SecurityAnalysis) -> None:
        """Add an analysis to the current session.
        
        Args:
            analysis: Security analysis to add
        """
        if self.current_session:
            self.current_session.analyses.append(analysis)
            self.session_repository.update_session(self.current_session)
    
    def get_current_session(self) -> Optional[MonitoringSession]:
        """Get the current active session.
        
        Returns:
            Current session if active, None otherwise
        """
        return self.current_session
