"""Video recording service implementation."""

import subprocess
import os
from datetime import datetime
from typing import Optional
from pathlib import Path
from loguru import logger
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import VideoClip, PersonDetection
from modules.domain.services import IVideoRecordingService


class FFmpegVideoRecorder(IVideoRecordingService):
    """Video recording service using FFmpeg."""
    
    def __init__(self, rtsp_url: str, output_dir: str = "./recordings"):
        """Initialize the video recorder.
        
        Args:
            rtsp_url: RTSP stream URL
            output_dir: Directory to save recordings
        """
        self.rtsp_url = rtsp_url
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self._active_recording = False
        
    def record_clip(self, duration: float, output_path: str, 
                   trigger_detection: Optional[PersonDetection] = None) -> Optional[VideoClip]:
        """Record a video clip from the RTSP stream.
        
        Args:
            duration: Duration in seconds
            output_path: Output file path
            trigger_detection: The detection that triggered recording
            
        Returns:
            VideoClip object if successful, None otherwise
        """
        if self._active_recording:
            logger.warning("Recording already in progress, skipping new recording")
            return None
        
        try:
            self._active_recording = True
            start_time = datetime.now()
            
            logger.info(f"Starting video recording for {duration} seconds...")
            
            # Ensure output directory exists
            output_path = Path(output_path)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # FFmpeg command to record from RTSP stream
            # Handle audio codec compatibility issues
            command = [
                "ffmpeg", "-y",  # Overwrite output file
                "-i", self.rtsp_url,  # Input RTSP stream
                "-t", str(duration),  # Duration
                "-vcodec", "copy",  # Copy video codec (no re-encoding)
                "-acodec", "aac",  # Convert audio to AAC (MP4 compatible)
                "-b:a", "128k",  # Audio bitrate
                "-avoid_negative_ts", "make_zero",  # Handle timestamp issues
                "-f", "mp4",  # Force MP4 format
                str(output_path)
            ]
            
            # Run FFmpeg with timeout
            result = subprocess.run(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=duration + 30,  # Add buffer time
                check=False
            )
            
            if result.returncode == 0 and output_path.exists():
                # Get video information
                video_info = self._get_video_info(str(output_path))
                
                # Create VideoClip object
                video_clip = VideoClip(
                    file_path=str(output_path),
                    start_time=start_time,
                    duration=duration,
                    frame_count=video_info.get('frame_count', 0),
                    resolution=video_info.get('resolution', (0, 0)),
                    trigger_detection=trigger_detection
                )
                
                logger.success(f"Video recording completed: {output_path}")
                return video_clip
                
            else:
                logger.error(f"FFmpeg failed with return code {result.returncode}")
                logger.error(f"FFmpeg stderr: {result.stderr.decode()}")
                return None
                
        except subprocess.TimeoutExpired:
            logger.error("FFmpeg recording timed out")
            return None
        except Exception as e:
            logger.error(f"Error during video recording: {e}")
            return None
        finally:
            self._active_recording = False
    
    def _get_video_info(self, video_path: str) -> dict:
        """Get information about a video file using ffprobe.
        
        Args:
            video_path: Path to the video file
            
        Returns:
            Dictionary with video information
        """
        try:
            # Use ffprobe to get video information
            command = [
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_format", "-show_streams",
                video_path
            ]
            
            result = subprocess.run(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=True
            )
            
            import json
            probe_data = json.loads(result.stdout.decode())
            
            # Extract video stream information
            video_stream = None
            for stream in probe_data.get('streams', []):
                if stream.get('codec_type') == 'video':
                    video_stream = stream
                    break
            
            if video_stream:
                return {
                    'frame_count': int(video_stream.get('nb_frames', 0)),
                    'resolution': (
                        int(video_stream.get('width', 0)),
                        int(video_stream.get('height', 0))
                    ),
                    'duration': float(video_stream.get('duration', 0)),
                    'fps': eval(video_stream.get('r_frame_rate', '0/1'))
                }
            
        except Exception as e:
            logger.warning(f"Failed to get video info: {e}")
        
        return {'frame_count': 0, 'resolution': (0, 0), 'duration': 0, 'fps': 0}
    
    def is_recording_active(self) -> bool:
        """Check if recording is currently active.
        
        Returns:
            True if recording is active, False otherwise
        """
        return self._active_recording
    
    def generate_output_path(self, detection: PersonDetection) -> str:
        """Generate a unique output path for a recording.
        
        Args:
            detection: The detection that triggered recording
            
        Returns:
            Path for the output file
        """
        timestamp = detection.timestamp.strftime("%Y%m%d_%H%M%S_%f")[:-3]
        filename = f"security_clip_{timestamp}.mp4"
        return str(self.output_dir / filename)
    
    def cleanup_old_recordings(self, max_age_days: int = 7) -> None:
        """Clean up old recording files.
        
        Args:
            max_age_days: Maximum age of files to keep
        """
        try:
            import time
            current_time = time.time()
            max_age_seconds = max_age_days * 24 * 3600
            
            for file_path in self.output_dir.glob("*.mp4"):
                file_age = current_time - file_path.stat().st_mtime
                if file_age > max_age_seconds:
                    file_path.unlink()
                    logger.info(f"Deleted old recording: {file_path}")
                    
        except Exception as e:
            logger.error(f"Error during cleanup: {e}")
