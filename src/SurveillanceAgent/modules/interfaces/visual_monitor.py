"""Visual monitoring interface for live RTSP stream with object detection."""

import cv2
import numpy as np
from typing import List, Optional, Tuple
from datetime import datetime
from loguru import logger
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import PersonDetection, SecurityThreatLevel


class VisualSecurityMonitor:
    """Visual interface for monitoring RTSP stream with object detection overlay."""
    
    def __init__(self, window_name: str = "Security Monitor"):
        """Initialize the visual monitor."""
        self.window_name = window_name
        self.window_initialized = False
        self.font = cv2.FONT_HERSHEY_SIMPLEX
        self.font_scale = 0.6
        self.font_thickness = 2
        
        # Colors (BGR format)
        self.colors = {
            'person_box': (0, 255, 0),      # Green for person detection
            'recording': (0, 0, 255),       # Red for recording indicator
            'normal': (0, 255, 0),          # Green for normal status
            'warning': (0, 165, 255),       # Orange for warnings
            'critical': (0, 0, 255),        # Red for critical threats
            'text_bg': (0, 0, 0),           # Black background for text
            'text': (255, 255, 255)         # White text
        }
        
        # Status tracking
        self.recording_status = False
        self.last_threat_level = SecurityThreatLevel.LOW
        self.detections_count = 0
        self.clips_recorded = 0
        self.analyses_completed = 0
        self.current_analysis = "System Ready"
        
    def initialize_window(self) -> bool:
        """Initialize the OpenCV window."""
        try:
            cv2.namedWindow(self.window_name, cv2.WINDOW_AUTOSIZE)
            cv2.resizeWindow(self.window_name, 1024, 768)
            self.window_initialized = True
            logger.info("Visual monitor window initialized")
            return True
        except Exception as e:
            logger.error(f"Failed to initialize visual monitor: {e}")
            return False
    
    def display_frame(self, frame, detections=None, status_info=None):
        """Display frame with overlays."""
        if not self.window_initialized:
            if not self.initialize_window():
                return False
        
        if frame is None:
            return True
            
        try:
            # Create a copy to avoid modifying original
            display_frame = frame.copy()
            
            # Draw person detection boxes
            if detections:
                display_frame = self._draw_detections(display_frame, detections)
            
            # Update status from info
            if status_info:
                self._update_status(status_info)
            
            # Draw status overlay
            display_frame = self._draw_status_overlay(display_frame)
            
            # Display the frame
            cv2.imshow(self.window_name, display_frame)
            
            # Handle key presses (1ms wait)
            key = cv2.waitKey(1) & 0xFF
            
            # Check if window was closed or ESC pressed
            if key == 27 or cv2.getWindowProperty(self.window_name, cv2.WND_PROP_VISIBLE) < 1:
                return False
                
            return True
            
        except Exception as e:
            logger.error(f"Error displaying frame: {e}")
            return False
    
    def _draw_detections(self, frame, detections):
        """Draw person detection bounding boxes."""
        for detection in detections:
            bbox = detection.bounding_box
            
            # Draw bounding box
            cv2.rectangle(
                frame,
                (bbox.x, bbox.y),
                (bbox.x + bbox.width, bbox.y + bbox.height),
                self.colors['person_box'],
                2
            )
            
            # Draw confidence label
            label = f"Person: {detection.confidence:.2f}"
            label_size = cv2.getTextSize(label, self.font, self.font_scale, self.font_thickness)[0]
            
            # Background for text
            cv2.rectangle(
                frame,
                (bbox.x, bbox.y - label_size[1] - 10),
                (bbox.x + label_size[0], bbox.y),
                self.colors['text_bg'],
                -1
            )
            
            # Text
            cv2.putText(
                frame,
                label,
                (bbox.x, bbox.y - 5),
                self.font,
                self.font_scale,
                self.colors['text'],
                self.font_thickness
            )
        
        return frame
    
    def _draw_status_overlay(self, frame):
        """Draw status information overlay."""
        h, w = frame.shape[:2]
        
        # Status panel dimensions
        panel_height = 150
        panel_width = 350
        panel_x = w - panel_width - 10
        panel_y = 10
        
        # Draw status panel background
        cv2.rectangle(frame, (panel_x, panel_y), (panel_x + panel_width, panel_y + panel_height), (0, 0, 0), -1)
        cv2.rectangle(frame, (panel_x, panel_y), (panel_x + panel_width, panel_y + panel_height), (255, 255, 255), 2)
        
        # Status text
        y_offset = panel_y + 25
        line_height = 20
        
        # Title
        cv2.putText(frame, "SECURITY MONITOR", (panel_x + 10, y_offset), self.font, 0.7, (255, 255, 255), 2)
        y_offset += line_height + 5
        
        # Recording status
        recording_color = self.colors['recording'] if self.recording_status else self.colors['normal']
        recording_text = "RECORDING" if self.recording_status else "MONITORING"
        cv2.putText(frame, f"Status: {recording_text}", (panel_x + 10, y_offset), self.font, 0.5, recording_color, 1)
        y_offset += line_height
        
        # Threat level
        threat_color = self._get_threat_color(self.last_threat_level)
        threat_text = self.last_threat_level.value.upper() if self.last_threat_level else "NONE"
        cv2.putText(frame, f"Threat: {threat_text}", (panel_x + 10, y_offset), self.font, 0.5, threat_color, 1)
        y_offset += line_height
        
        # Statistics
        cv2.putText(frame, f"Detections: {self.detections_count}", (panel_x + 10, y_offset), self.font, 0.4, (255, 255, 255), 1)
        y_offset += line_height
        cv2.putText(frame, f"Clips: {self.clips_recorded}", (panel_x + 10, y_offset), self.font, 0.4, (255, 255, 255), 1)
        y_offset += line_height
        cv2.putText(frame, f"Analyses: {self.analyses_completed}", (panel_x + 10, y_offset), self.font, 0.4, (255, 255, 255), 1)
        
        # Timestamp
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cv2.putText(frame, timestamp, (10, h - 10), self.font, 0.5, (255, 255, 255), 1)
        
        return frame
    
    def _get_threat_color(self, threat_level):
        """Get color for threat level."""
        color_map = {
            SecurityThreatLevel.LOW: self.colors['normal'],
            SecurityThreatLevel.MEDIUM: self.colors['warning'],
            SecurityThreatLevel.HIGH: self.colors['critical'],
            SecurityThreatLevel.CRITICAL: self.colors['critical']
        }
        return color_map.get(threat_level, self.colors['normal'])
    
    def _update_status(self, status_info):
        """Update internal status from system information."""
        self.recording_status = status_info.get('recording', False)
        self.detections_count = status_info.get('total_detections', 0)
        self.clips_recorded = status_info.get('total_clips', 0)
        self.analyses_completed = status_info.get('total_analyses', 0)
        
        if 'last_threat_level' in status_info:
            self.last_threat_level = status_info['last_threat_level']
        if 'current_analysis' in status_info:
            self.current_analysis = status_info['current_analysis']
    
    def update_analysis(self, analysis_text, threat_level):
        """Update the current analysis display."""
        self.current_analysis = analysis_text
        self.last_threat_level = threat_level
        self.analyses_completed += 1
    
    def cleanup(self):
        """Clean up OpenCV windows."""
        try:
            cv2.destroyAllWindows()
            logger.info("Visual monitor cleaned up")
        except Exception as e:
            logger.error(f"Error cleaning up visual monitor: {e}")
