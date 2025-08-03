"""Dependency injection container for the security monitoring system."""

from typing import Optional
from loguru import logger

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.domain.services import (
    IPersonDetectionService, IVideoRecordingService, 
    ISecurityAnalysisService, INotificationService
)
from modules.domain.repositories import (
    IPersonDetectionRepository, IVideoClipRepository, 
    ISecurityAnalysisRepository, IMonitoringSessionRepository
)

from modules.infrastructure.person_detector import MobileNetPersonDetector
from modules.infrastructure.video_recorder import FFmpegVideoRecorder
from modules.infrastructure.ollama_client import OllamaSecurityAnalyzer
from modules.infrastructure.gemma_transformer_client import GemmaTransformerSecurityAnalyzer
from modules.infrastructure.mobile_notifications import MobileNotificationService

from modules.interfaces.repositories import (
    SQLitePersonDetectionRepository, FileSystemVideoClipRepository,
    JSONSecurityAnalysisRepository, InMemoryMonitoringSessionRepository
)

from modules.application.use_cases import (
    PersonDetectionUseCase, VideoRecordingUseCase,
    SecurityAnalysisUseCase, MonitoringSessionUseCase
)
from modules.application.security_monitor import SecurityMonitor

from .settings import SecurityMonitorSettings


class DIContainer:
    """Dependency injection container following IoC principles."""
    
    def __init__(self, settings: SecurityMonitorSettings):
        """Initialize the DI container with settings.
        
        Args:
            settings: Configuration settings
        """
        self.settings = settings
        self._repositories = {}
        self._services = {}
        self._use_cases = {}
        
    # Repository factories
    def get_person_detection_repository(self) -> IPersonDetectionRepository:
        """Get person detection repository instance."""
        if 'person_detection' not in self._repositories:
            self._repositories['person_detection'] = SQLitePersonDetectionRepository(
                db_path=self.settings.database_path
            )
            logger.info("Created person detection repository")
        return self._repositories['person_detection']
    
    def get_video_clip_repository(self) -> IVideoClipRepository:
        """Get video clip repository instance."""
        if 'video_clip' not in self._repositories:
            self._repositories['video_clip'] = FileSystemVideoClipRepository(
                data_path=self.settings.video_clips_data_path
            )
            logger.info("Created video clip repository")
        return self._repositories['video_clip']
    
    def get_security_analysis_repository(self) -> ISecurityAnalysisRepository:
        """Get security analysis repository instance."""
        if 'security_analysis' not in self._repositories:
            self._repositories['security_analysis'] = JSONSecurityAnalysisRepository(
                data_path=self.settings.analyses_data_path
            )
            logger.info("Created security analysis repository")
        return self._repositories['security_analysis']
    
    def get_monitoring_session_repository(self) -> IMonitoringSessionRepository:
        """Get monitoring session repository instance."""
        if 'monitoring_session' not in self._repositories:
            self._repositories['monitoring_session'] = InMemoryMonitoringSessionRepository()
            logger.info("Created monitoring session repository")
        return self._repositories['monitoring_session']
    
    # Service factories
    def get_person_detection_service(self) -> IPersonDetectionService:
        """Get person detection service instance."""
        if 'person_detection' not in self._services:
            self._services['person_detection'] = MobileNetPersonDetector(
                confidence_threshold=self.settings.detection_confidence_threshold
            )
            logger.info("Created person detection service")
        return self._services['person_detection']
    
    def get_video_recording_service(self) -> IVideoRecordingService:
        """Get video recording service instance."""
        if 'video_recording' not in self._services:
            self._services['video_recording'] = FFmpegVideoRecorder(
                rtsp_url=self.settings.rtsp_url,
                output_dir=self.settings.clip_output_dir
            )
            logger.info("Created video recording service")
        return self._services['video_recording']
    
    def get_security_analysis_service(self) -> ISecurityAnalysisService:
        """Get security analysis service instance."""
        if 'security_analysis' not in self._services:
            # Choose service based on AI mode setting
            if self.settings.ai_mode == "transformer":
                logger.info("Creating transformer-based security analysis service")
                self._services['security_analysis'] = GemmaTransformerSecurityAnalyzer(
                    model_name=self.settings.transformer_model,
                    analysis_prompt=self.settings.analysis_prompt,
                    device=self.settings.transformer_device,
                    resolution=self.settings.transformer_resolution
                )
                logger.info("Created transformer security analysis service")
            else:
                logger.info("Creating Ollama-based security analysis service")
                self._services['security_analysis'] = OllamaSecurityAnalyzer(
                    ollama_url=self.settings.ollama_url,
                    model_name=self.settings.ollama_model,
                    text_model_name=self.settings.ollama_text_model,
                    analysis_prompt=self.settings.analysis_prompt
                )
                logger.info("Created Ollama security analysis service")
        return self._services['security_analysis']
    
    def get_notification_service(self) -> INotificationService:
        """Get notification service instance."""
        if 'notification' not in self._services:
            # Parse webhook URLs
            webhook_urls = []
            if self.settings.notification_webhook_urls:
                webhook_urls = [
                    url.strip() for url in self.settings.notification_webhook_urls.split(',')
                    if url.strip()
                ]
            
            # Parse threat level
            from modules.domain.entities import SecurityThreatLevel
            try:
                min_threat_level = SecurityThreatLevel[self.settings.notification_min_threat_level.upper()]
            except KeyError:
                min_threat_level = SecurityThreatLevel.MEDIUM
                logger.warning(f"Invalid threat level {self.settings.notification_min_threat_level}, using MEDIUM")
            
            self._services['notification'] = MobileNotificationService(
                notification_url=self.settings.mobile_notification_url,
                webhook_urls=webhook_urls,
                min_threat_level=min_threat_level
            )
            self._services['notification'].enable_push_notifications = self.settings.enable_mobile_notifications
            self._services['notification'].notification_port = self.settings.notification_server_port
            
            logger.info("Created notification service")
        return self._services['notification']
    
    # Use case factories
    def get_person_detection_use_case(self) -> PersonDetectionUseCase:
        """Get person detection use case instance."""
        if 'person_detection' not in self._use_cases:
            self._use_cases['person_detection'] = PersonDetectionUseCase(
                detection_service=self.get_person_detection_service(),
                detection_repository=self.get_person_detection_repository()
            )
            logger.info("Created person detection use case")
        return self._use_cases['person_detection']
    
    def get_video_recording_use_case(self) -> VideoRecordingUseCase:
        """Get video recording use case instance."""
        if 'video_recording' not in self._use_cases:
            self._use_cases['video_recording'] = VideoRecordingUseCase(
                recording_service=self.get_video_recording_service(),
                video_repository=self.get_video_clip_repository()
            )
            logger.info("Created video recording use case")
        return self._use_cases['video_recording']
    
    def get_security_analysis_use_case(self) -> SecurityAnalysisUseCase:
        """Get security analysis use case instance."""
        if 'security_analysis' not in self._use_cases:
            self._use_cases['security_analysis'] = SecurityAnalysisUseCase(
                analysis_service=self.get_security_analysis_service(),
                analysis_repository=self.get_security_analysis_repository()
            )
            logger.info("Created security analysis use case")
        return self._use_cases['security_analysis']
    
    def get_monitoring_session_use_case(self) -> MonitoringSessionUseCase:
        """Get monitoring session use case instance."""
        if 'monitoring_session' not in self._use_cases:
            self._use_cases['monitoring_session'] = MonitoringSessionUseCase(
                session_repository=self.get_monitoring_session_repository()
            )
            logger.info("Created monitoring session use case")
        return self._use_cases['monitoring_session']
    
    # Main application factory
    def get_security_monitor(self, enable_visual_monitor: bool = False) -> SecurityMonitor:
        """Get the main security monitor instance.
        
        Args:
            enable_visual_monitor: Whether to enable live visual preview
        """
        return SecurityMonitor(
            rtsp_url=self.settings.rtsp_url,
            person_detection_use_case=self.get_person_detection_use_case(),
            video_recording_use_case=self.get_video_recording_use_case(),
            security_analysis_use_case=self.get_security_analysis_use_case(),
            session_use_case=self.get_monitoring_session_use_case(),
            notification_service=self.get_notification_service(),
            clip_duration=self.settings.clip_duration,
            enable_visual_monitor=enable_visual_monitor
        )
    
    def cleanup(self) -> None:
        """Cleanup resources."""
        try:
            # Close any open connections, files, etc.
            logger.info("DI container cleanup completed")
        except Exception as e:
            logger.error(f"Error during DI container cleanup: {e}")


# Global container instance
container: Optional[DIContainer] = None


def get_container(settings: Optional[SecurityMonitorSettings] = None) -> DIContainer:
    """Get the global DI container instance.
    
    Args:
        settings: Configuration settings (required for first call)
        
    Returns:
        Global DI container instance
    """
    global container
    if container is None:
        if settings is None:
            raise ValueError("Settings required for first container initialization")
        container = DIContainer(settings)
        logger.info("DI container initialized")
    return container


def cleanup_container() -> None:
    """Cleanup the global container."""
    global container
    if container:
        container.cleanup()
        container = None
