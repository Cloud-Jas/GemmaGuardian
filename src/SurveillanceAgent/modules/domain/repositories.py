"""Repository interfaces for the domain layer."""

from abc import ABC, abstractmethod
from typing import List, Optional
from .entities import PersonDetection, VideoClip, SecurityAnalysis, MonitoringSession


class IPersonDetectionRepository(ABC):
    """Interface for person detection data persistence."""
    
    @abstractmethod
    def save_detection(self, detection: PersonDetection) -> None:
        """Save a person detection event."""
        pass
    
    @abstractmethod
    def get_detections_by_timeframe(self, start_time, end_time) -> List[PersonDetection]:
        """Retrieve detections within a time frame."""
        pass


class IVideoClipRepository(ABC):
    """Interface for video clip data persistence."""
    
    @abstractmethod
    def save_clip(self, clip: VideoClip) -> None:
        """Save a video clip record."""
        pass
    
    @abstractmethod
    def get_clip_by_path(self, file_path: str) -> Optional[VideoClip]:
        """Retrieve a clip by its file path."""
        pass
    
    @abstractmethod
    def delete_clip(self, file_path: str) -> bool:
        """Delete a video clip."""
        pass


class ISecurityAnalysisRepository(ABC):
    """Interface for security analysis data persistence."""
    
    @abstractmethod
    def save_analysis(self, analysis: SecurityAnalysis) -> None:
        """Save a security analysis result."""
        pass
    
    @abstractmethod
    def get_analyses_by_threat_level(self, threat_level) -> List[SecurityAnalysis]:
        """Retrieve analyses by threat level."""
        pass


class IMonitoringSessionRepository(ABC):
    """Interface for monitoring session data persistence."""
    
    @abstractmethod
    def save_session(self, session: MonitoringSession) -> None:
        """Save a monitoring session."""
        pass
    
    @abstractmethod
    def get_active_session(self) -> Optional[MonitoringSession]:
        """Get the currently active monitoring session."""
        pass
    
    @abstractmethod
    def update_session(self, session: MonitoringSession) -> None:
        """Update an existing monitoring session."""
        pass
