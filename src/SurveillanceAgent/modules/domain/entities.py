"""Core domain entities for the security monitoring system."""

from dataclasses import dataclass
from datetime import datetime
from typing import List, Optional, Tuple
from enum import Enum
import numpy as np


class SecurityThreatLevel(Enum):
    """Enumeration for security threat levels."""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class DetectionStatus(Enum):
    """Enumeration for detection status."""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"


@dataclass
class BoundingBox:
    """Represents a bounding box for object detection."""
    x: int
    y: int
    width: int
    height: int
    confidence: float
    
    @property
    def center(self) -> Tuple[int, int]:
        """Get the center point of the bounding box."""
        return (self.x + self.width // 2, self.y + self.height // 2)
    
    @property
    def area(self) -> int:
        """Calculate the area of the bounding box."""
        return self.width * self.height


@dataclass
class PersonDetection:
    """Represents a person detection event."""
    timestamp: datetime
    bounding_box: BoundingBox
    frame_number: int
    confidence: float
    
    def __post_init__(self):
        """Validate the detection data."""
        if self.confidence < 0 or self.confidence > 1:
            raise ValueError("Confidence must be between 0 and 1")


@dataclass
class VideoClip:
    """Represents a recorded video clip."""
    file_path: str
    start_time: datetime
    duration: float
    frame_count: int
    resolution: Tuple[int, int]
    trigger_detection: PersonDetection
    
    @property
    def end_time(self) -> datetime:
        """Calculate the end time of the clip."""
        from datetime import timedelta
        return self.start_time + timedelta(seconds=self.duration)


@dataclass
class SecurityAnalysis:
    """Represents the result of AI security analysis."""
    video_clip: VideoClip
    analysis_text: str
    threat_level: SecurityThreatLevel
    confidence: float
    keywords: List[str]
    timestamp: datetime
    
    @property
    def is_threat_detected(self) -> bool:
        """Check if any security threat was detected."""
        return self.threat_level in [SecurityThreatLevel.MEDIUM, 
                                   SecurityThreatLevel.HIGH, 
                                   SecurityThreatLevel.CRITICAL]


@dataclass
class MonitoringSession:
    """Represents a monitoring session."""
    session_id: str
    start_time: datetime
    end_time: Optional[datetime]
    detections: List[PersonDetection]
    video_clips: List[VideoClip]
    analyses: List[SecurityAnalysis]
    status: DetectionStatus
    
    @property
    def duration(self) -> Optional[float]:
        """Calculate session duration in seconds."""
        if self.end_time:
            return (self.end_time - self.start_time).total_seconds()
        return None
    
    @property
    def threat_count(self) -> int:
        """Count the number of detected threats."""
        return sum(1 for analysis in self.analyses if analysis.is_threat_detected)
