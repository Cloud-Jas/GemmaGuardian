"""Repository implementations for data persistence."""

import json
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import List, Optional
from loguru import logger
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import (
    PersonDetection, VideoClip, SecurityAnalysis, MonitoringSession,
    BoundingBox, SecurityThreatLevel, DetectionStatus
)
from modules.domain.repositories import (
    IPersonDetectionRepository, IVideoClipRepository, 
    ISecurityAnalysisRepository, IMonitoringSessionRepository
)


class SQLitePersonDetectionRepository(IPersonDetectionRepository):
    """SQLite implementation of person detection repository."""
    
    def __init__(self, db_path: str = "data/security_monitor.db"):
        """Initialize the repository with database path.
        
        Args:
            db_path: Path to SQLite database file
        """
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_database()
    
    def _init_database(self) -> None:
        """Initialize the database schema."""
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("""
                    CREATE TABLE IF NOT EXISTS person_detections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        bbox_x INTEGER NOT NULL,
                        bbox_y INTEGER NOT NULL,
                        bbox_width INTEGER NOT NULL,
                        bbox_height INTEGER NOT NULL,
                        bbox_confidence REAL NOT NULL,
                        frame_number INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                conn.commit()
        except Exception as e:
            logger.error(f"Failed to initialize person detection database: {e}")
    
    def save_detection(self, detection: PersonDetection) -> None:
        """Save a person detection event."""
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("""
                    INSERT INTO person_detections 
                    (timestamp, bbox_x, bbox_y, bbox_width, bbox_height, 
                     bbox_confidence, frame_number, confidence)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    detection.timestamp.isoformat(),
                    detection.bounding_box.x,
                    detection.bounding_box.y,
                    detection.bounding_box.width,
                    detection.bounding_box.height,
                    detection.bounding_box.confidence,
                    detection.frame_number,
                    detection.confidence
                ))
                conn.commit()
        except Exception as e:
            logger.error(f"Failed to save person detection: {e}")
    
    def get_detections_by_timeframe(self, start_time, end_time) -> List[PersonDetection]:
        """Retrieve detections within a time frame."""
        detections = []
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.execute("""
                    SELECT timestamp, bbox_x, bbox_y, bbox_width, bbox_height,
                           bbox_confidence, frame_number, confidence
                    FROM person_detections
                    WHERE timestamp BETWEEN ? AND ?
                    ORDER BY timestamp DESC
                """, (start_time.isoformat(), end_time.isoformat()))
                
                for row in cursor.fetchall():
                    bbox = BoundingBox(
                        x=row[1], y=row[2], width=row[3], height=row[4], confidence=row[5]
                    )
                    detection = PersonDetection(
                        timestamp=datetime.fromisoformat(row[0]),
                        bounding_box=bbox,
                        frame_number=row[6],
                        confidence=row[7]
                    )
                    detections.append(detection)
                    
        except Exception as e:
            logger.error(f"Failed to retrieve detections: {e}")
            
        return detections


class FileSystemVideoClipRepository(IVideoClipRepository):
    """File system implementation of video clip repository."""
    
    def __init__(self, data_path: str = "data/video_clips.json"):
        """Initialize the repository with data file path.
        
        Args:
            data_path: Path to JSON data file
        """
        self.data_path = Path(data_path)
        self.data_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Initialize empty data file if it doesn't exist
        if not self.data_path.exists():
            self._save_data([])
    
    def _load_data(self) -> List[dict]:
        """Load data from JSON file."""
        try:
            with open(self.data_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Failed to load video clip data: {e}")
            return []
    
    def _save_data(self, data: List[dict]) -> None:
        """Save data to JSON file."""
        try:
            with open(self.data_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, default=str, ensure_ascii=False)
        except Exception as e:
            logger.error(f"Failed to save video clip data: {e}")
    
    def save_clip(self, clip: VideoClip) -> None:
        """Save a video clip record."""
        try:
            data = self._load_data()
            
            clip_data = {
                'file_path': clip.file_path,
                'start_time': clip.start_time.isoformat(),
                'duration': clip.duration,
                'frame_count': clip.frame_count,
                'resolution': clip.resolution,
                'trigger_detection': {
                    'timestamp': clip.trigger_detection.timestamp.isoformat(),
                    'confidence': clip.trigger_detection.confidence,
                    'frame_number': clip.trigger_detection.frame_number
                } if clip.trigger_detection else None
            }
            
            data.append(clip_data)
            self._save_data(data)
            
        except Exception as e:
            logger.error(f"Failed to save video clip: {e}")
    
    def get_clip_by_path(self, file_path: str) -> Optional[VideoClip]:
        """Retrieve a clip by its file path."""
        try:
            data = self._load_data()
            
            for clip_data in data:
                if clip_data['file_path'] == file_path:
                    # Reconstruct VideoClip object
                    # Note: This is simplified - full implementation would reconstruct trigger_detection
                    return VideoClip(
                        file_path=clip_data['file_path'],
                        start_time=datetime.fromisoformat(clip_data['start_time']),
                        duration=clip_data['duration'],
                        frame_count=clip_data['frame_count'],
                        resolution=tuple(clip_data['resolution']),
                        trigger_detection=None  # Simplified
                    )
            
        except Exception as e:
            logger.error(f"Failed to retrieve video clip: {e}")
            
        return None
    
    def delete_clip(self, file_path: str) -> bool:
        """Delete a video clip."""
        try:
            # Remove from data
            data = self._load_data()
            data = [clip for clip in data if clip['file_path'] != file_path]
            self._save_data(data)
            
            # Remove actual file
            file_path_obj = Path(file_path)
            if file_path_obj.exists():
                file_path_obj.unlink()
                
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete video clip: {e}")
            return False


class JSONSecurityAnalysisRepository(ISecurityAnalysisRepository):
    """JSON file implementation of security analysis repository."""
    
    def __init__(self, data_path: str = "data/security_analyses.json"):
        """Initialize the repository with data file path.
        
        Args:
            data_path: Path to JSON data file
        """
        self.data_path = Path(data_path)
        self.data_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Initialize empty data file if it doesn't exist
        if not self.data_path.exists():
            self._save_data([])
    
    def _load_data(self) -> List[dict]:
        """Load data from JSON file."""
        try:
            with open(self.data_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Failed to load analysis data: {e}")
            return []
    
    def _save_data(self, data: List[dict]) -> None:
        """Save data to JSON file."""
        try:
            with open(self.data_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, default=str, ensure_ascii=False)
        except Exception as e:
            logger.error(f"Failed to save analysis data: {e}")
    
    def save_analysis(self, analysis: SecurityAnalysis) -> None:
        """Save a security analysis result."""
        try:
            data = self._load_data()
            
            analysis_data = {
                'video_file_path': analysis.video_clip.file_path,
                'analysis_text': analysis.analysis_text,
                'threat_level': analysis.threat_level.value,
                'confidence': analysis.confidence,
                'keywords': analysis.keywords,
                'timestamp': analysis.timestamp.isoformat()
            }
            
            data.append(analysis_data)
            self._save_data(data)
            
        except Exception as e:
            logger.error(f"Failed to save security analysis: {e}")
    
    def get_analyses_by_threat_level(self, threat_level) -> List[SecurityAnalysis]:
        """Retrieve analyses by threat level."""
        analyses = []
        try:
            data = self._load_data()
            
            for analysis_data in data:
                if SecurityThreatLevel(analysis_data['threat_level']) >= threat_level:
                    # Note: This is simplified - full implementation would reconstruct full objects
                    # For now, just return basic info
                    pass
                    
        except Exception as e:
            logger.error(f"Failed to retrieve analyses by threat level: {e}")
            
        return analyses


class InMemoryMonitoringSessionRepository(IMonitoringSessionRepository):
    """In-memory implementation of monitoring session repository."""
    
    def __init__(self):
        """Initialize the repository."""
        self.sessions = {}
        self.active_session_id = None
    
    def save_session(self, session: MonitoringSession) -> None:
        """Save a monitoring session."""
        try:
            self.sessions[session.session_id] = session
            if session.status == DetectionStatus.IN_PROGRESS:
                self.active_session_id = session.session_id
        except Exception as e:
            logger.error(f"Failed to save monitoring session: {e}")
    
    def get_active_session(self) -> Optional[MonitoringSession]:
        """Get the currently active monitoring session."""
        try:
            if self.active_session_id and self.active_session_id in self.sessions:
                session = self.sessions[self.active_session_id]
                if session.status == DetectionStatus.IN_PROGRESS:
                    return session
            return None
        except Exception as e:
            logger.error(f"Failed to get active session: {e}")
            return None
    
    def update_session(self, session: MonitoringSession) -> None:
        """Update an existing monitoring session."""
        try:
            self.sessions[session.session_id] = session
            if session.status == DetectionStatus.COMPLETED:
                self.active_session_id = None
        except Exception as e:
            logger.error(f"Failed to update monitoring session: {e}")
