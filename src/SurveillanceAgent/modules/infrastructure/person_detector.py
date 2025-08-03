"""Person detection using MobileNet implementation."""

import cv2
import numpy as np
from typing import List, Tuple
from datetime import datetime
from loguru import logger
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import PersonDetection, BoundingBox
from modules.domain.services import IPersonDetectionService


class MobileNetPersonDetector(IPersonDetectionService):
    """Person detection service using MobileNet SSD."""
    
    def __init__(self, confidence_threshold: float = 0.5):
        """Initialize the MobileNet person detector.
        
        Args:
            confidence_threshold: Minimum confidence for valid detections
        """
        self.confidence_threshold = confidence_threshold
        self.net = None
        self.class_names = None
        self._load_model()
    
    def _load_model(self) -> None:
        """Load the MobileNet SSD model."""
        try:
            # Load pre-trained MobileNet SSD model
            prototxt_path = "models/MobileNetSSD_deploy.prototxt.txt"
            model_path = "models/MobileNetSSD_deploy.caffemodel"
            
            # Try to load from OpenCV's DNN module with COCO dataset
            self.net = cv2.dnn.readNetFromCaffe(prototxt_path, model_path)
            
            # COCO class names (person is class 15 in COCO, but 1 in MobileNet SSD)
            self.class_names = ["background", "aeroplane", "bicycle", "bird", "boat",
                               "bottle", "bus", "car", "cat", "chair", "cow", "diningtable",
                               "dog", "horse", "motorbike", "person", "pottedplant", "sheep",
                               "sofa", "train", "tvmonitor"]
            
            logger.success("MobileNet SSD model loaded successfully")
            
        except Exception as e:
            logger.error(f"Failed to load MobileNet model: {e}")
            logger.info("Falling back to Haar Cascade for person detection")
            self._load_haar_cascade()
    
    def _load_haar_cascade(self) -> None:
        """Fallback to Haar Cascade for person detection."""
        try:
            self.net = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_fullbody.xml')
            logger.success("Haar Cascade loaded as fallback")
        except Exception as e:
            logger.error(f"Failed to load Haar Cascade: {e}")
            self.net = None
    
    def detect_persons(self, frame: np.ndarray) -> List[PersonDetection]:
        """Detect persons in the given frame.
        
        Args:
            frame: Input video frame
            
        Returns:
            List of person detections
        """
        if self.net is None:
            return []
        
        detections = []
        frame_number = getattr(self, '_frame_counter', 0)
        self._frame_counter = frame_number + 1
        
        try:
            if isinstance(self.net, cv2.dnn_Net):
                detections = self._detect_with_mobilenet(frame, frame_number)
            else:
                detections = self._detect_with_haar_cascade(frame, frame_number)
                
        except Exception as e:
            logger.error(f"Error during person detection: {e}")
        
        return detections
    
    def _detect_with_mobilenet(self, frame: np.ndarray, frame_number: int) -> List[PersonDetection]:
        """Detect persons using MobileNet SSD.
        
        Args:
            frame: Input video frame
            frame_number: Current frame number
            
        Returns:
            List of person detections
        """
        h, w = frame.shape[:2]
        
        # Create blob from frame
        blob = cv2.dnn.blobFromImage(
            frame, 0.017, (300, 300), (103.94, 116.78, 123.68)
        )
        
        # Set input to the network
        self.net.setInput(blob)
        
        # Run forward pass
        detections_result = self.net.forward()
        
        persons = []
        
        # Process detections
        for i in range(detections_result.shape[2]):
            confidence = detections_result[0, 0, i, 2]
            class_id = int(detections_result[0, 0, i, 1])
            
            # Check if detection is a person (class_id 15 for person)
            if class_id == 15 and confidence > self.confidence_threshold:
                # Get bounding box coordinates
                x1 = int(detections_result[0, 0, i, 3] * w)
                y1 = int(detections_result[0, 0, i, 4] * h)
                x2 = int(detections_result[0, 0, i, 5] * w)
                y2 = int(detections_result[0, 0, i, 6] * h)
                
                # Create bounding box
                bbox = BoundingBox(
                    x=x1,
                    y=y1,
                    width=x2 - x1,
                    height=y2 - y1,
                    confidence=float(confidence)
                )
                
                # Create person detection
                detection = PersonDetection(
                    timestamp=datetime.now(),
                    bounding_box=bbox,
                    frame_number=frame_number,
                    confidence=float(confidence)
                )
                
                persons.append(detection)
        
        return persons
    
    def _detect_with_haar_cascade(self, frame: np.ndarray, frame_number: int) -> List[PersonDetection]:
        """Detect persons using Haar Cascade (fallback method).
        
        Args:
            frame: Input video frame
            frame_number: Current frame number
            
        Returns:
            List of person detections
        """
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # Detect persons
        bodies = self.net.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=3,
            minSize=(50, 100)
        )
        
        persons = []
        for (x, y, w, h) in bodies:
            # Create bounding box
            bbox = BoundingBox(
                x=x,
                y=y,
                width=w,
                height=h,
                confidence=0.8  # Haar cascade doesn't provide confidence
            )
            
            # Create person detection
            detection = PersonDetection(
                timestamp=datetime.now(),
                bounding_box=bbox,
                frame_number=frame_number,
                confidence=0.8
            )
            
            persons.append(detection)
        
        return persons
    
    def is_detection_valid(self, detection: PersonDetection) -> bool:
        """Validate if a detection meets quality criteria.
        
        Args:
            detection: Person detection to validate
            
        Returns:
            True if detection is valid, False otherwise
        """
        # Check confidence threshold
        if detection.confidence < self.confidence_threshold:
            return False
        
        # Check bounding box size (minimum size requirements)
        min_width, min_height = 30, 60
        if (detection.bounding_box.width < min_width or 
            detection.bounding_box.height < min_height):
            return False
        
        # Check aspect ratio (person should be taller than wide)
        aspect_ratio = detection.bounding_box.height / detection.bounding_box.width
        if aspect_ratio < 1.2:  # Persons are typically taller than wide
            return False
        
        return True
