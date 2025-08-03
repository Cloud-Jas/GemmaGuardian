"""RTSP stream handling infrastructure."""

import cv2
import numpy as np
import time
from typing import Optional, Generator
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_exponential


class RTSPStreamHandler:
    """Handles RTSP stream connection and frame capture."""
    
    def __init__(self, rtsp_url: str, buffer_size: int = 1):
        """Initialize RTSP stream handler.
        
        Args:
            rtsp_url: RTSP stream URL
            buffer_size: OpenCV buffer size (1 for minimal latency)
        """
        self.rtsp_url = rtsp_url
        self.buffer_size = buffer_size
        self.cap: Optional[cv2.VideoCapture] = None
        self.is_connected = False
        self.failed_frame_count = 0
        self.max_failed_frames = 30  # Reconnect after 30 consecutive failures
        self.last_successful_read = time.time()
        self.reconnect_threshold = 10.0  # Reconnect if no successful read for 10 seconds
        
    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=4, max=10))
    def connect(self) -> bool:
        """Connect to the RTSP stream with enhanced error handling.
        
        Returns:
            True if connection successful, False otherwise
        """
        try:
            logger.info(f"Connecting to RTSP stream: {self.rtsp_url}")
            
            # Initialize VideoCapture with enhanced options for HEVC
            self.cap = cv2.VideoCapture(self.rtsp_url)
            
            # Set buffer size to minimum for real-time processing
            self.cap.set(cv2.CAP_PROP_BUFFERSIZE, self.buffer_size)
            
            # Set codec preferences and timeouts for better HEVC handling
            self.cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('H', '2', '6', '5'))
            self.cap.set(cv2.CAP_PROP_OPEN_TIMEOUT_MSEC, 10000)  # 10 seconds
            self.cap.set(cv2.CAP_PROP_READ_TIMEOUT_MSEC, 5000)   # 5 seconds
            
            # Test if we can read a frame
            ret, frame = self.cap.read()
            if ret and frame is not None:
                self.is_connected = True
                self.failed_frame_count = 0
                self.last_successful_read = time.time()
                logger.success("Successfully connected to RTSP stream")
                return True
            else:
                logger.error("Failed to read initial frame from RTSP stream")
                self.disconnect()
                return False
                self.disconnect()
                return False
                
        except Exception as e:
            logger.error(f"Error connecting to RTSP stream: {e}")
            self.disconnect()
            return False
    
    def disconnect(self) -> None:
        """Disconnect from the RTSP stream."""
        if self.cap:
            self.cap.release()
            self.cap = None
        self.is_connected = False
        self.failed_frame_count = 0
        logger.info("Disconnected from RTSP stream")
    
    def _should_reconnect(self) -> bool:
        """Check if we should attempt to reconnect.
        
        Returns:
            True if reconnection is needed
        """
        current_time = time.time()
        return (self.failed_frame_count >= self.max_failed_frames or 
                current_time - self.last_successful_read > self.reconnect_threshold)
    
    def get_frame(self) -> Optional[np.ndarray]:
        """Get the latest frame from the stream with auto-reconnection.
        
        Returns:
            The frame as numpy array, or None if failed
        """
        if not self.is_connected or not self.cap:
            return None
            
        try:
            ret, frame = self.cap.read()
            if ret and frame is not None:
                # Successful read - reset failure counters
                self.failed_frame_count = 0
                self.last_successful_read = time.time()
                return frame
            else:
                # Failed read - increment counter
                self.failed_frame_count += 1
                
                # Check if we should attempt reconnection
                if self._should_reconnect():
                    logger.warning(f"Stream issues detected (failed: {self.failed_frame_count}, "
                                 f"last_success: {time.time() - self.last_successful_read:.1f}s ago)")
                    logger.info("Attempting to reconnect to RTSP stream...")
                    
                    self.disconnect()
                    if self.connect():
                        logger.info("Successfully reconnected to RTSP stream")
                        # Try to get a frame after reconnection
                        ret, frame = self.cap.read()
                        if ret and frame is not None:
                            self.failed_frame_count = 0
                            self.last_successful_read = time.time()
                            return frame
                    else:
                        logger.error("Failed to reconnect to RTSP stream")
                
                return None
                
        except Exception as e:
            self.failed_frame_count += 1
            logger.error(f"Error reading frame: {e}")
            
            # Attempt reconnection on critical errors
            if self._should_reconnect():
                logger.info("Attempting reconnection due to critical error...")
                self.disconnect()
                self.connect()
            
            return None
    
    def get_stream_info(self) -> dict:
        """Get information about the stream.
        
        Returns:
            Dictionary containing stream properties
        """
        if not self.cap:
            return {}
            
        return {
            'width': int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH)),
            'height': int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT)),
            'fps': self.cap.get(cv2.CAP_PROP_FPS),
            'codec': int(self.cap.get(cv2.CAP_PROP_FOURCC))
        }
    
    def frame_generator(self) -> Generator[np.ndarray, None, None]:
        """Generator that yields frames from the stream.
        
        Yields:
            Video frames as numpy arrays
        """
        while self.is_connected:
            frame = self.get_frame()
            if frame is not None:
                yield frame
            else:
                # Attempt to reconnect if frame reading fails
                logger.warning("Frame reading failed, attempting to reconnect...")
                if not self.connect():
                    break
    
    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.disconnect()
