"""Ollama client for AI analysis using Gemma model."""

import requests
import json
import base64
import concurrent.futures
from typing import List, Optional
from pathlib import Path
import cv2
import numpy as np
from loguru import logger
from datetime import datetime
import sys
import os
import shutil
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import VideoClip, SecurityAnalysis, SecurityThreatLevel
from modules.domain.services import ISecurityAnalysisService, SecurityThreatEvaluator


class OllamaSecurityAnalyzer(ISecurityAnalysisService):
    """Security analysis service using Ollama and Gemma model."""
    
    def __init__(self, ollama_url: str = "http://localhost:11434", 
                 model_name: str = "gemma3:4b",
                 text_model_name: str = "gemma3n:e4b",
                 analysis_prompt: str = None):
        """Initialize the Ollama security analyzer.
        
        Args:
            ollama_url: Ollama server URL
            model_name: Name of the model to use for vision analysis (default: gemma3:4b)
            text_model_name: Name of the model to use for text analysis (default: gemma3n:e4b)
            analysis_prompt: Custom prompt for analysis
        """
        self.ollama_url = ollama_url.rstrip('/')
        self.model_name = model_name  # Gemma 3 4B for vision analysis only
        self.text_model_name = text_model_name  # Gemma 3n e4b for text analysis
        self.consolidation_model = text_model_name  # Use text model for consolidation and summarization
        self.threat_evaluator = SecurityThreatEvaluator(ollama_url, text_model_name)  # Use text model for threat analysis
        
        # Create directories for frame storage and logs
        self.frames_dir = Path("frames_analyzed")
        self.analysis_logs_dir = Path("analysis_logs")
        self.frames_dir.mkdir(exist_ok=True)
        self.analysis_logs_dir.mkdir(exist_ok=True)
        
        self.analysis_prompt = "Analyze these surveillance frames objectively. Report only what you actually observe - do not invent or imagine details. " \
            "Document: personnel count, movements, vehicles, objects, and activities. " \
            "Identify security anomalies, unauthorized access, or suspicious behavior only if clearly visible. " \
            "For routine activity with no concerns, state: 'Routine surveillance - no security incidents detected.'"
        
    def analyze_video(self, video_clip: VideoClip) -> SecurityAnalysis:
        """Analyze a video clip for security concerns.
        
        Args:
            video_clip: The video clip to analyze
            
        Returns:
            SecurityAnalysis object with results
        """
        try:
            logger.info(f"Starting security analysis of video: {video_clip.file_path}")
            
            # Extract frames at 2-second intervals for 60 seconds (gives us 30 frames)
            frames = self.extract_frames(video_clip.file_path, num_frames=30)
            
            if not frames:
                logger.error("No frames extracted from video")
                return self._create_failed_analysis(video_clip, "Failed to extract frames")
            
            # Save frames locally for debugging and record keeping
            saved_frame_paths = self._save_frames_locally(frames, video_clip.file_path)
            
            # Convert frames to base64 for Ollama
            frame_data = self._frames_to_base64(frames)
            
            # Analyze each frame individually and then consolidate
            analysis_text = self._analyze_frames_individually(frame_data, video_clip.file_path, saved_frame_paths)
            
            if not analysis_text:
                logger.error("Failed to get analysis from Ollama after processing all frames")
                return self._create_failed_analysis(video_clip, "Failed to get AI analysis after retries")
            
            # Evaluate threat level and extract keywords in parallel for better performance
            import concurrent.futures
            with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
                threat_future = executor.submit(self.threat_evaluator.evaluate_threat_level, analysis_text)
                keywords_future = executor.submit(self.threat_evaluator.extract_keywords, analysis_text)
                
                threat_level = threat_future.result()
                keywords = keywords_future.result()
            
            # Calculate confidence based on analysis quality
            confidence = self._calculate_confidence(analysis_text, keywords)
            
            security_analysis = SecurityAnalysis(
                video_clip=video_clip,
                analysis_text=analysis_text,
                threat_level=threat_level,
                confidence=confidence,
                keywords=keywords,
                timestamp=datetime.now()
            )
            
            logger.success(f"Security analysis completed. Threat level: {threat_level.value}")
            return security_analysis
            
        except Exception as e:
            logger.error(f"Error during security analysis: {e}")
            return self._create_failed_analysis(video_clip, f"Analysis error: {str(e)}")
    
    def extract_frames(self, video_path: str, num_frames: int = 30) -> List[np.ndarray]:
        """Extract frames from video at 2-second intervals for 60 seconds duration.
        
        Args:
            video_path: Path to the video file
            num_frames: Number of frames to extract (default 30 for 60 seconds at 2-second intervals)
            
        Returns:
            List of frames as numpy arrays
        """
        frames = []
        
        try:
            cap = cv2.VideoCapture(video_path)
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            fps = cap.get(cv2.CAP_PROP_FPS)
            
            if total_frames == 0:
                logger.error("Video has no frames")
                return frames
            
            if fps <= 0:
                logger.warning("Could not determine FPS, using default 30 FPS")
                fps = 30.0
            
            # Calculate actual video duration
            actual_duration = total_frames / fps
            logger.info(f"Video info - Total frames: {total_frames}, FPS: {fps:.2f}, Duration: {actual_duration:.2f} seconds")
            
            # Calculate frame indices for 2-second intervals over 60 seconds
            frame_interval = int(fps * 2)  # 2 seconds * FPS = frames per 2-second interval
            max_duration_frames = int(fps * 60)  # 60 seconds total duration
            frame_indices = []
            
            current_frame = 0
            while current_frame < min(total_frames, max_duration_frames) and len(frame_indices) < num_frames:
                frame_indices.append(current_frame)
                current_frame += frame_interval
            
            logger.info(f"Extracting {len(frame_indices)} frames at 2-second intervals for 60-second duration (FPS: {fps})")
            
            for frame_idx in frame_indices:
                cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
                ret, frame = cap.read()
                
                if ret and frame is not None:
                    # Resize frame to Gemma 3 4B optimal resolution (1024x1024 for best quality)
                    # Using 1024x1024 for high quality analysis with the more capable model
                    frame = cv2.resize(frame, (1024, 1024))
                    frames.append(frame)
                else:
                    logger.warning(f"Failed to read frame at index {frame_idx}")
            
            cap.release()
            logger.info(f"Extracted {len(frames)} frames from video (2-second intervals over 60 seconds)")
            
        except Exception as e:
            logger.error(f"Error extracting frames: {e}")
        
        return frames
    
    def _frames_to_base64(self, frames: List[np.ndarray]) -> List[str]:
        """Convert frames to base64 encoded strings optimized for Gemma 3 4B.
        
        Gemma 3 4B supports high-resolution images up to 1024x1024 with excellent
        vision processing capabilities.
        
        Args:
            frames: List of frame arrays
            
        Returns:
            List of base64 encoded frame strings
        """
        base64_frames = []
        
        for frame in frames:
            try:
                # Ensure frame is the correct size (should already be 1024x1024 from extraction)
                if frame.shape[:2] != (1024, 1024):
                    frame = cv2.resize(frame, (1024, 1024))
                
                # Encode frame as high-quality JPEG for Gemma 3 4B
                _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 98])
                
                # Convert to base64
                frame_base64 = base64.b64encode(buffer).decode('utf-8')
                base64_frames.append(frame_base64)
                
            except Exception as e:
                logger.warning(f"Failed to encode frame: {e}")
        
        return base64_frames
    
    def _save_frames_locally(self, frames: List[np.ndarray], video_path: str) -> List[str]:
        """Save frames locally for debugging and record keeping.
        
        Args:
            frames: List of frame arrays
            video_path: Original video file path for naming
            
        Returns:
            List of saved frame file paths
        """
        saved_paths = []
        
        try:
            # Create a unique directory for this video's frames
            video_name = Path(video_path).stem
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            frame_session_dir = self.frames_dir / f"{video_name}_{timestamp}"
            frame_session_dir.mkdir(exist_ok=True)
            
            for i, frame in enumerate(frames):
                frame_filename = f"frame_{i+1:02d}.jpg"
                frame_path = frame_session_dir / frame_filename
                
                # Save frame as JPEG
                cv2.imwrite(str(frame_path), frame)
                saved_paths.append(str(frame_path))
                
            logger.info(f"Saved {len(saved_paths)} frames to {frame_session_dir}")
            
        except Exception as e:
            logger.error(f"Error saving frames locally: {e}")
        
        return saved_paths
    
    def _log_analysis_session(self, video_path: str, frame_paths: List[str], 
                             batch_analyses: List[dict], consolidated_analysis: str):
        """Log detailed analysis session information.
        
        Args:
            video_path: Original video file path
            frame_paths: List of saved frame file paths
            batch_analyses: List of batch analysis results with metadata
            consolidated_analysis: Final consolidated analysis
        """
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]  # Include milliseconds
            log_filename = f"analysis_session_{timestamp}.json"
            log_path = self.analysis_logs_dir / log_filename
            
            session_data = {
                "timestamp": datetime.now().isoformat() + "Z",
                "video_path": video_path,
                "model_used": self.model_name,
                "processing_method": "batch_analysis",
                "batch_size": 4,
                "frames_analyzed": {
                    "total_count": len(frame_paths),
                    "saved_paths": frame_paths
                },
                "batch_analyses": batch_analyses,
                "consolidated_analysis": consolidated_analysis,
                "analysis_prompt": self.analysis_prompt
            }
            
            with open(log_path, 'w', encoding='utf-8') as f:
                json.dump(session_data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Analysis session logged to {log_path}")
            
        except Exception as e:
            logger.error(f"Error logging analysis session: {e}")
    
    def _analyze_frames_individually(self, frame_data: List[str], video_path: str, saved_frame_paths: List[str]) -> Optional[str]:
        """Analyze frames in batches of 4 images per call.
        
        Args:
            frame_data: List of base64 encoded frames
            video_path: Original video file path for logging
            saved_frame_paths: List of saved frame file paths for logging
            
        Returns:
            Consolidated analysis text
        """
        import concurrent.futures
        import threading
        import time
        
        batch_analyses = []  # Store batch analysis results with metadata
        batch_size = 4
        
        # Split frames into batches of 4 for Gemma 3 4B efficiency
        frame_batches = [frame_data[i:i + batch_size] for i in range(0, len(frame_data), batch_size)]
        
        logger.info(f"Processing {len(frame_data)} frames in {len(frame_batches)} batches of up to {batch_size} images each")
        
        # Process batches in parallel but maintain order with timeout handling
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:  # Reduced workers to prevent overload
            # Submit each batch for analysis
            future_to_batch = {
                executor.submit(self._analyze_frame_batch, batch, batch_idx + 1, batch_idx * batch_size): batch_idx 
                for batch_idx, batch in enumerate(frame_batches)
            }
            
            # Create a results array to maintain order
            batch_results = [None] * len(frame_batches)
            
            # Collect results as they complete
            for future in concurrent.futures.as_completed(future_to_batch):
                batch_idx = future_to_batch[future]
                try:
                    batch_analysis_result = future.result()
                    if batch_analysis_result:
                        batch_results[batch_idx] = batch_analysis_result
                        logger.info(f"Completed analysis of batch {batch_idx + 1}")
                    else:
                        logger.warning(f"No analysis received for batch {batch_idx + 1}")
                        # Add placeholder for missing batch
                        start_frame = batch_idx * batch_size
                        end_frame = start_frame + len(frame_batches[batch_idx]) - 1
                        batch_results[batch_idx] = {
                            "batch_number": batch_idx + 1,
                            "frame_range": {"start": start_frame + 1, "end": end_frame + 1},
                            "frames_in_batch": len(frame_batches[batch_idx]),
                            "batch_summary": "No analysis received",  # Use batch_summary instead of raw_analysis
                            "individual_frame_analyses": [
                                {"frame_number": start_frame + i + 1, "analysis": "No analysis received"}
                                for i in range(len(frame_batches[batch_idx]))
                            ]
                        }
                except Exception as e:
                    logger.error(f"Error analyzing batch {batch_idx + 1}: {e}")
                    # Add error placeholder for failed batch
                    start_frame = batch_idx * batch_size
                    end_frame = start_frame + len(frame_batches[batch_idx]) - 1
                    batch_results[batch_idx] = {
                        "batch_number": batch_idx + 1,
                        "frame_range": {"start": start_frame + 1, "end": end_frame + 1},
                        "frames_in_batch": len(frame_batches[batch_idx]),
                        "batch_summary": f"Error: {str(e)}",  # Use batch_summary instead of raw_analysis
                        "individual_frame_analyses": [
                            {"frame_number": start_frame + i + 1, "analysis": f"Error: {str(e)}"}
                            for i in range(len(frame_batches[batch_idx]))
                        ]
                    }
            
            # Process results in the correct order and collect for consolidation
            # Collect batch summaries for consolidation
            batch_summaries = []
            for batch_idx, batch_result in enumerate(batch_results):
                if batch_result and isinstance(batch_result, dict) and "batch_summary" in batch_result:
                    batch_analyses.append(batch_result)
                    # Extract batch summary for consolidation
                    batch_summaries.append(batch_result["batch_summary"])
                else:
                    logger.warning(f"Batch {batch_idx} result is invalid or missing batch_summary: {batch_result}")
        
        if not batch_summaries:
            logger.error("No successful batch analyses")
            return None
        
        # Consolidate all batch summaries into final security assessment
        consolidated_analysis = self._consolidate_analyses(batch_summaries)
        
        # Log the complete analysis session with proper batch structure
        self._log_analysis_session(video_path, saved_frame_paths, batch_analyses, consolidated_analysis)
        
        return consolidated_analysis
    
    def _analyze_frame_batch(self, frame_batch: List[str], batch_number: int, start_frame_idx: int = 0) -> dict:
        """Analyze a batch of 4 frames simultaneously.
        
        Args:
            frame_batch: List of base64 encoded frames (up to 4)
            batch_number: Batch number for logging
            start_frame_idx: Starting frame index for this batch (for better labeling)
            
        Returns:
            Dictionary containing batch analysis result with metadata
        """
        try:
            logger.debug(f"Starting analysis of batch {batch_number} with {len(frame_batch)} frames (frames {start_frame_idx+1}-{start_frame_idx+len(frame_batch)})")

            prompt = "Analyze these surveillance frames objectively. Report only what you actually observe - do not invent or imagine details. " \
            "Document: personnel count, movements, vehicles, objects, and activities. " \
            "Identify security anomalies, unauthorized access, or suspicious behavior only if clearly visible. " \
            "For routine activity with no concerns, state: 'Routine surveillance - no security incidents detected.'"

            payload = {
                "model": self.model_name,
                "prompt": prompt,
                "images": frame_batch,
                "stream": False,
                "options": {
                    "temperature": 0.2,
                    "num_predict": 800,
                    "top_k": 40,
                    "top_p": 0.9
                }
            }
            
            logger.debug(f"Sending batch {batch_number} to Ollama for analysis")
            response = requests.post(
                f"{self.ollama_url}/api/generate",
                json=payload,
                timeout=180  # Increased timeout to 3 minutes for batch processing
            )
            
            if response.status_code == 200:
                result = response.json()
                raw_analysis = result.get('response', '').strip()
                
                if raw_analysis:
                    logger.info(f"Batch {batch_number} analysis received: {len(raw_analysis)} characters")
                    
                    # Create structured batch result with summary instead of individual frame analyses
                    batch_result = {
                        "batch_number": batch_number,
                        "frame_range": {
                            "start": start_frame_idx + 1,
                            "end": start_frame_idx + len(frame_batch)
                        },
                        "frames_in_batch": len(frame_batch),
                        "timestamp_range": {
                            "start_seconds": start_frame_idx * 2,
                            "end_seconds": (start_frame_idx + len(frame_batch) - 1) * 2
                        },
                        "batch_summary": raw_analysis 
                    }
                    
                    return batch_result
                else:
                    logger.warning(f"Batch {batch_number} analysis returned empty response")
                    return None
            else:
                logger.error(f"Batch {batch_number} analysis failed with status: {response.status_code}")
                logger.error(f"Response text: {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            logger.error(f"Batch {batch_number} analysis timed out after 180 seconds")
            return None
        except requests.exceptions.ConnectionError:
            logger.error(f"Connection error for batch {batch_number}")
            return None
        except Exception as e:
            logger.error(f"Error analyzing batch {batch_number}: {e}")
            return None

    def _consolidate_analyses(self, batch_summaries: List[str]) -> str:
        """Consolidate batch summaries into a comprehensive security assessment.
        
        Args:
            batch_summaries: List of batch summary texts
            
        Returns:
            Consolidated analysis summary
        """
        try:
            # Combine all batch summaries
            combined_summaries = "\n\n".join([f"Batch {i+1}: {summary}" for i, summary in enumerate(batch_summaries)])
            
            # Create effective consolidation prompt for security assessment
            consolidation_prompt = (
                "Consolidate these surveillance batch reports into a final security assessment. "
                "Report only facts observed in the footage - do not speculate or create fictional details. "
                "Summarize: personnel, movements, vehicles, objects, and any documented security concerns. "
                "Base conclusions only on what was actually reported. "
                "For normal activity, conclude: 'No security incidents detected.'\n\n"
                f"{combined_summaries}\n\n"
                "Security Assessment:"
            )
            
            # Use Gemma 3n e4b for consolidation and summarization
            payload = {
                "model": self.consolidation_model,  # Use Gemma 3n e4b for consolidation
                "prompt": consolidation_prompt,
                "stream": False,
                "options": {
                    "temperature": 0.3,  # Lower temperature for consistent security summaries
                    "num_predict": 1000  # Increased for comprehensive security assessment
                }
            }
            
            response = requests.post(
                f"{self.ollama_url}/api/generate",
                json=payload,
                timeout=420
            )
            
            if response.status_code == 200:
                result = response.json()
                consolidated = result.get('response', '').strip()
                if consolidated:
                    logger.info(f"Successfully consolidated frame analyses using {self.consolidation_model}")
                    return consolidated
            
            # Fallback: return combined batch summaries if consolidation fails
            logger.warning("Consolidation failed, returning combined batch summaries")
            return f"Multiple batch summaries:\n{combined_summaries}"
            
        except Exception as e:
            logger.error(f"Error consolidating analyses: {e}")
            # Fallback: return combined batch summaries
            return f"Batch summaries (consolidation failed):\n{'\n'.join(batch_summaries)}"
    
    def _calculate_confidence(self, analysis_text: str, keywords: List[str]) -> float:
        """Calculate confidence score for the analysis.
        
        Args:
            analysis_text: The analysis text
            keywords: Extracted keywords
            
        Returns:
            Confidence score between 0 and 1
        """
        base_confidence = 0.7  # Base confidence for successful analysis
        
        # Increase confidence based on analysis length and detail
        text_length_factor = min(len(analysis_text) / 200, 0.2)
        keyword_factor = min(len(keywords) / 5, 0.1)
        
        confidence = base_confidence + text_length_factor + keyword_factor
        return min(confidence, 1.0)
    
    def _create_failed_analysis(self, video_clip: VideoClip, error_message: str) -> SecurityAnalysis:
        """Create a failed analysis result.
        
        Args:
            video_clip: The video clip that failed analysis
            error_message: Error description
            
        Returns:
            SecurityAnalysis with failure information
        """
        return SecurityAnalysis(
            video_clip=video_clip,
            analysis_text=f"Analysis failed: {error_message}",
            threat_level=SecurityThreatLevel.LOW,
            confidence=0.0,
            keywords=[],
            timestamp=datetime.now()
        )
    
    def check_ollama_health(self) -> bool:
        """Check if Ollama server is healthy and model is available.
        
        Returns:
            True if healthy, False otherwise
        """
        try:
            # Check if server is running
            response = requests.get(f"{self.ollama_url}/api/tags", timeout=10)
            
            if response.status_code == 200:
                models = response.json().get('models', [])
                model_names = [model.get('name', '') for model in models]
                
                if self.model_name in model_names:
                    logger.info(f"Ollama is healthy and {self.model_name} is available")
                    return True
                else:
                    logger.warning(f"Model {self.model_name} not found in Ollama")
                    return False
            else:
                logger.error(f"Ollama health check failed: {response.status_code}")
                return False
                
        except Exception as e:
            logger.error(f"Ollama health check error: {e}")
            return False
