"""Domain services for business logic."""

from abc import ABC, abstractmethod
from typing import List, Optional
import numpy as np
from .entities import PersonDetection, VideoClip, SecurityAnalysis, SecurityThreatLevel


class IPersonDetectionService(ABC):
    """Interface for person detection service."""
    
    @abstractmethod
    def detect_persons(self, frame: np.ndarray) -> List[PersonDetection]:
        """Detect persons in a video frame."""
        pass
    
    @abstractmethod
    def is_detection_valid(self, detection: PersonDetection) -> bool:
        """Validate if a detection meets quality criteria."""
        pass


class IVideoRecordingService(ABC):
    """Interface for video recording service."""
    
    @abstractmethod
    def record_clip(self, duration: float, output_path: str) -> Optional[VideoClip]:
        """Record a video clip from the stream."""
        pass
    
    @abstractmethod
    def is_recording_active(self) -> bool:
        """Check if recording is currently active."""
        pass


class ISecurityAnalysisService(ABC):
    """Interface for security analysis service."""
    
    @abstractmethod
    def analyze_video(self, video_clip: VideoClip) -> SecurityAnalysis:
        """Analyze a video clip for security concerns."""
        pass
    
    @abstractmethod
    def extract_frames(self, video_path: str, num_frames: int = 2) -> List[np.ndarray]:
        """Extract representative frames from a video."""
        pass


class INotificationService(ABC):
    """Interface for notification service."""
    
    @abstractmethod
    async def send_security_alert(self, analysis: SecurityAnalysis, video_path: str = None) -> bool:
        """Send security alert notification."""
        pass
    
    @abstractmethod
    def should_notify(self, analysis: SecurityAnalysis) -> bool:
        """Check if notification should be sent."""
        pass
    
    @abstractmethod
    def send_test_notification(self) -> bool:
        """Send test notification."""
        pass


class SecurityThreatEvaluator:
    """Service for evaluating security threat levels using AI analysis."""
    
    def __init__(self, ollama_url: str = "http://localhost:11434", model_name: str = "gemma3n:e4b"):
        """Initialize the threat evaluator with Ollama configuration."""
        self.ollama_url = ollama_url.rstrip('/')
        self.model_name = model_name
    
    def evaluate_threat_level(self, analysis_text: str) -> SecurityThreatLevel:
        """Evaluate threat level using AI analysis."""
        try:
            threat_prompt = (
                "Based on the following security analysis, determine the threat level. "
                "Base classification only on what was actually observed - do not assume or invent threats. "
                "Respond with ONLY ONE of these exact words: LOW, MEDIUM, HIGH, CRITICAL.\n\n"
                "Guidelines:\n"
                "- LOW: Normal scene, no people, routine activity, no concerns\n"
                "- MEDIUM: Unusual but not threatening behavior, minor concerns\n"
                "- HIGH: Suspicious behavior, potential security risk, concerning activity\n"
                "- CRITICAL: Clear threats, weapons, violence, break-ins, immediate danger\n\n"
                f"Security Analysis:\n{analysis_text}\n\n"
                "Threat Level:"
            )
            
            import requests
            
            payload = {
                "model": self.model_name,
                "prompt": threat_prompt,
                "stream": False,
                "options": {
                    "temperature": 0.1,  # Very low temperature for consistent classification
                    "num_predict": 100    # Short response - just the threat level
                }
            }
            
            # Retry logic with increasing timeouts
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    timeout = 60 + (attempt * 30)  # 60s, 90s, 120s
                    
                    response = requests.post(
                        f"{self.ollama_url}/api/generate",
                        json=payload,
                        timeout=timeout
                    )
                    
                    if response.status_code == 200:
                        result = response.json()
                        threat_response = result.get('response', '').strip().upper()
                        
                        # Map response to enum
                        if 'CRITICAL' in threat_response:
                            return SecurityThreatLevel.CRITICAL
                        elif 'HIGH' in threat_response:
                            return SecurityThreatLevel.HIGH
                        elif 'MEDIUM' in threat_response:
                            return SecurityThreatLevel.MEDIUM
                        else:  # Default to LOW for any other response including 'LOW'
                            return SecurityThreatLevel.LOW
                    else:
                        if attempt == max_retries - 1:
                            break  # Exit retry loop, will go to fallback
                        
                except requests.exceptions.Timeout:
                    if attempt == max_retries - 1:
                        print(f"AI threat evaluation timed out after {max_retries} attempts")
                        break  # Exit retry loop, will go to fallback
                    else:
                        print(f"Threat evaluation timeout on attempt {attempt + 1}, retrying...")
                        
                except Exception as retry_e:
                    if attempt == max_retries - 1:
                        raise retry_e  # Re-raise the exception to be caught by outer try-catch
                    else:
                        print(f"Threat evaluation error on attempt {attempt + 1}: {retry_e}, retrying...")
            
        except Exception as e:
            # Fallback to keyword-based evaluation if AI fails
            print(f"AI threat evaluation failed: {e}")
            return self._fallback_keyword_evaluation(analysis_text)
        
        # Default fallback
        return SecurityThreatLevel.LOW
    
    def _fallback_keyword_evaluation(self, analysis_text: str) -> SecurityThreatLevel:
        """Fallback keyword-based threat evaluation."""
        text_lower = analysis_text.lower()
        
        # Check for explicit "no security concerns" statements first
        safe_indicators = [
            'no security concerns detected',
            'normal scene',
            'no people or suspicious activity visible',
            'no suspicious activity',
            'all frames show normal',
            'normal activity',
            'no threats detected',
            'appears normal'
        ]
        
        if any(indicator in text_lower for indicator in safe_indicators):
            return SecurityThreatLevel.LOW
        
        # Critical threats
        critical_keywords = ['weapon', 'violence', 'attack', 'break-in', 'forced entry']
        if any(keyword in text_lower for keyword in critical_keywords):
            return SecurityThreatLevel.CRITICAL
        
        # High threats (with context awareness)
        high_keywords = ['suspicious', 'unauthorized', 'trespassing']
        if any(keyword in text_lower for keyword in high_keywords):
            # Check for negative context
            negative_context = ['no suspicious', 'not suspicious', 'no unauthorized']
            if not any(neg in text_lower for neg in negative_context):
                return SecurityThreatLevel.HIGH
        
        # Medium threats
        medium_keywords = ['unusual', 'loitering', 'investigation']
        if any(keyword in text_lower for keyword in medium_keywords):
            return SecurityThreatLevel.MEDIUM
        
        return SecurityThreatLevel.LOW
    
    def extract_keywords(self, analysis_text: str) -> List[str]:
        """Extract security-relevant keywords using AI analysis."""
        try:
            keyword_prompt = (
                "Extract 3-5 relevant security keywords from the following analysis based only on what was actually observed. "
                "Do not add keywords for details that were not mentioned. "
                "Return only the keywords separated by commas, no explanations.\n"
                "Focus on: people, activities, objects, threats, or scene descriptions.\n\n"
                f"Analysis: {analysis_text}\n\n"
                "Keywords:"
            )
            
            import requests
            
            payload = {
                "model": self.model_name,
                "prompt": keyword_prompt,
                "stream": False,
                "options": {
                    "temperature": 0.2,
                    "num_predict": 50
                }
            }
            
            # Retry logic with increasing timeouts
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    timeout = 60 + (attempt * 30)  # 60s, 90s, 120s
                    
                    response = requests.post(
                        f"{self.ollama_url}/api/generate",
                        json=payload,
                        timeout=timeout
                    )
                    
                    if response.status_code == 200:
                        result = response.json()
                        keywords_response = result.get('response', '').strip()
                        
                        # Parse keywords from response
                        keywords = [k.strip().lower() for k in keywords_response.split(',')]
                        keywords = [k for k in keywords if k and len(k) > 2]  # Filter out short/empty words
                        return keywords[:5]  # Limit to 5 keywords
                    else:
                        if attempt == max_retries - 1:
                            break  # Exit retry loop, will go to fallback
                        
                except requests.exceptions.Timeout:
                    if attempt == max_retries - 1:
                        print(f"AI keyword extraction timed out after {max_retries} attempts")
                        break  # Exit retry loop, will go to fallback
                    else:
                        print(f"Keyword extraction timeout on attempt {attempt + 1}, retrying...")
                        
                except Exception as retry_e:
                    if attempt == max_retries - 1:
                        raise retry_e  # Re-raise the exception to be caught by outer try-catch
                    else:
                        print(f"Keyword extraction error on attempt {attempt + 1}: {retry_e}, retrying...")
            
        except Exception as e:
            print(f"AI keyword extraction failed: {e}")
            return self._fallback_keyword_extraction(analysis_text)
        
        return self._fallback_keyword_extraction(analysis_text)
    
    def _fallback_keyword_extraction(self, analysis_text: str) -> List[str]:
        """Fallback keyword extraction using simple text analysis."""
        text_lower = analysis_text.lower()
        
        # Basic keyword list for fallback
        all_keywords = [
            'normal', 'suspicious', 'unusual', 'people', 'person', 'individual',
            'parking', 'vehicle', 'car', 'building', 'entrance', 'activity',
            'walking', 'running', 'standing', 'weapon', 'threat', 'security',
            'loitering', 'trespassing', 'breaking', 'forced entry'
        ]
        
        found_keywords = []
        for keyword in all_keywords:
            if keyword in text_lower:
                found_keywords.append(keyword)
        
        return found_keywords[:5]  # Limit to 5 keywords
