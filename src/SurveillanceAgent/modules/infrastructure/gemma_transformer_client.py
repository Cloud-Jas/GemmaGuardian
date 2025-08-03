"""Gemma 3n transformer client for AI analysis using direct transformers implementation."""

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
import json
import textwrap
import torch
from PIL import Image

# Transformers imports
try:
    from transformers import AutoProcessor, Gemma3nForConditionalGeneration
    TRANSFORMERS_AVAILABLE = True
except ImportError:
    TRANSFORMERS_AVAILABLE = False
    logger.warning("Transformers library not available. Install with: pip install transformers timm av")

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from modules.domain.entities import VideoClip, SecurityAnalysis, SecurityThreatLevel
from modules.domain.services import ISecurityAnalysisService, SecurityThreatEvaluator


class GemmaTransformerSecurityAnalyzer(ISecurityAnalysisService):
    """Security analysis service using Gemma 3n transformer model directly."""
    
    def __init__(self, 
                 model_name: str = "google/gemma-3n-e4b-it",
                 analysis_prompt: str = None,
                 device: str = "auto",
                 resolution: int = 512):
        """Initialize the Gemma transformer security analyzer.
        
        Args:
            model_name: Name of the Gemma 3n model to use
            analysis_prompt: Custom prompt for analysis
            device: Device to run the model on ("auto", "cuda", "cpu")
            resolution: Image resolution for processing (default 512 for efficiency)
        """
        if not TRANSFORMERS_AVAILABLE:
            raise ImportError("Transformers library required for transformer mode. Install with: pip install transformers timm av")
        
        self.model_name = model_name
        self.device = device
        self.resolution = resolution
        
        # Create directories for frame storage and logs
        self.frames_dir = Path("frames_analyzed")
        self.analysis_logs_dir = Path("analysis_logs")
        self.frames_dir.mkdir(exist_ok=True)
        self.analysis_logs_dir.mkdir(exist_ok=True)
        
        self.analysis_prompt = analysis_prompt or (
            "Analyze these surveillance frames objectively. Report only what you actually observe - do not invent or imagine details. "
            "Document: personnel count, movements, vehicles, objects, and activities. "
            "Identify security anomalies, unauthorized access, or suspicious behavior only if clearly visible. "
            "For routine activity with no concerns, state: 'Routine surveillance - no security incidents detected.'"
        )
        
        # Initialize model and processor
        self.model = None
        self.processor = None
        self.threat_evaluator = None
        self._load_model()
    
    def _load_model(self) -> None:
        """Load the Gemma 3n model and processor."""
        try:
            logger.info(f"Loading Gemma 3n model: {self.model_name}")
            
            # Determine the best device to use
            device = self._get_optimal_device()
            logger.info(f"Using device: {device}")
            
            # Set torch dtype based on device
            if device.startswith('cuda'):
                torch_dtype = torch.bfloat16  # Use bfloat16 for GPU efficiency
                logger.info("Using bfloat16 precision for GPU")
            else:
                torch_dtype = torch.float32   # Use float32 for CPU stability
                logger.info("Using float32 precision for CPU")
            
            # Load model with device-appropriate configuration
            logger.info("Loading model with optimized configuration...")
            self.model = Gemma3nForConditionalGeneration.from_pretrained(
                self.model_name, 
                torch_dtype=torch_dtype,
                low_cpu_mem_usage=True,
                device_map=device if device != 'cpu' else None,
                attn_implementation="eager"  # Use eager attention implementation
            ).eval()
            
            # Move to device if needed
            if device == 'cpu':
                self.model = self.model.to('cpu')
            
            # Load processor
            self.processor = AutoProcessor.from_pretrained(self.model_name)
            
            # Initialize threat evaluator (using text-only capabilities)
            self.threat_evaluator = TransformerThreatEvaluator(self.model, self.processor)
            
            logger.success(f"Gemma 3n model loaded successfully on {device}")
            
        except Exception as e:
            logger.error(f"Failed to load Gemma 3n model: {e}")
            raise
    
    def _get_optimal_device(self) -> str:
        """Determine the optimal device to use based on availability and user preference."""
        # Check user preference from settings
        if self.device.lower() == 'cpu':
            logger.info("CPU mode explicitly requested")
            return 'cpu'
        
        # Check if CUDA is available
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / 1024**3  # GB
            logger.info(f"CUDA available: {gpu_name} ({gpu_memory:.1f} GB)")
            
            # Check if we have enough GPU memory (Gemma 3n-e4b needs ~8GB)
            if gpu_memory >= 6.0:  # Minimum 6GB for reasonable performance
                logger.info("Sufficient GPU memory available, using CUDA")
                return 'cuda:0'
            else:
                logger.warning(f"GPU has only {gpu_memory:.1f} GB memory, falling back to CPU")
                return 'cpu'
        else:
            logger.info("CUDA not available, using CPU")
            return 'cpu'
    
    def analyze_video(self, video_clip: VideoClip) -> SecurityAnalysis:
        """Analyze a video clip for security concerns.
        
        Args:
            video_clip: The video clip to analyze
            
        Returns:
            SecurityAnalysis object with results
        """
        try:
            logger.info(f"Starting transformer security analysis of video: {video_clip.file_path}")
            
            # Extract frames at 2-second intervals for 60 seconds (gives us 30 frames)
            frames = self.extract_frames(video_clip.file_path, num_frames=5)  # Reduced for testing
            
            if not frames:
                logger.error("No frames extracted from video")
                return self._create_failed_analysis(video_clip, "Failed to extract frames")
            
            # Save frames locally for debugging and record keeping
            saved_frame_paths = self._save_frames_locally(frames, video_clip.file_path)
            
            # Convert frames to PIL Images for transformer
            pil_images = self._frames_to_pil(frames)
            
            # Analyze frames in batches and then consolidate
            analysis_text = self._analyze_frames_with_transformer(pil_images, video_clip.file_path, saved_frame_paths)
            
            if not analysis_text:
                logger.error("Failed to get analysis from transformer after processing all frames")
                return self._create_failed_analysis(video_clip, "Failed to get transformer analysis after retries")
            
            # Evaluate threat level and extract keywords using transformer in parallel
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
            
            logger.success(f"Transformer security analysis completed. Threat level: {threat_level.value}")
            return security_analysis
            
        except Exception as e:
            logger.error(f"Error during transformer security analysis: {e}")
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
                    # Resize frame to configured resolution for transformer processing
                    frame = cv2.resize(frame, (self.resolution, self.resolution))
                    frames.append(frame)
                else:
                    logger.warning(f"Failed to read frame at index {frame_idx}")
            
            cap.release()
            logger.info(f"Extracted {len(frames)} frames from video (2-second intervals over 60 seconds)")
            
        except Exception as e:
            logger.error(f"Error extracting frames: {e}")
        
        return frames
    
    def _frames_to_pil(self, frames: List[np.ndarray]) -> List[Image.Image]:
        """Convert OpenCV frames to PIL Images for transformer processing.
        
        Args:
            frames: List of OpenCV frame arrays (BGR format)
            
        Returns:
            List of PIL Images (RGB format)
        """
        pil_images = []
        
        for frame in frames:
            try:
                # Convert BGR to RGB
                rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                
                # Convert to PIL Image
                pil_image = Image.fromarray(rgb_frame)
                pil_images.append(pil_image)
                
            except Exception as e:
                logger.warning(f"Failed to convert frame to PIL: {e}")
        
        return pil_images
    
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
    
    def _analyze_frames_with_transformer(self, pil_images: List[Image.Image], video_path: str, saved_frame_paths: List[str]) -> Optional[str]:
        """Analyze frames using the transformer model in batches.
        
        Args:
            pil_images: List of PIL Images
            video_path: Original video file path for logging
            saved_frame_paths: List of saved frame file paths for logging
            
        Returns:
            Consolidated analysis text
        """
        batch_analyses = []
        batch_size = 1
        
        # Split frames into batches of 1 for transformer efficiency
        image_batches = [pil_images[i:i + batch_size] for i in range(0, len(pil_images), batch_size)]
        
        logger.info(f"Processing {len(pil_images)} frames in {len(image_batches)} batches of up to {batch_size} images each")
        
        # Process batches sequentially to avoid memory issues
        for batch_idx, image_batch in enumerate(image_batches):
            try:
                logger.info(f"Processing batch {batch_idx + 1}/{len(image_batches)} with {len(image_batch)} images")
                
                # Create messages for this batch
                content = []
                for image in image_batch:
                    content.append({"type": "image", "image": image})
                
                content.append({
                    "type": "text", 
                    "text": self.analysis_prompt
                })
                
                messages = [
                    {
                        "role": "system",
                        "content": [{"type": "text", "text": "You are a professional security analyst. Report only factual observations from the images - never invent or hallucinate details."}]
                    },
                    {
                        "role": "user",
                        "content": content
                    }
                ]
                
                # Get analysis for this batch
                batch_analysis = self._predict(messages)
                
                if batch_analysis:
                    batch_analyses.append({
                        "batch_number": batch_idx + 1,
                        "images_count": len(image_batch),
                        "analysis": batch_analysis,
                        "timestamp": datetime.now().isoformat()
                    })
                    logger.info(f"Batch {batch_idx + 1} analysis completed")
                else:
                    logger.warning(f"Batch {batch_idx + 1} analysis failed")
                
            except Exception as e:
                logger.error(f"Error processing batch {batch_idx + 1}: {e}")
        
        if not batch_analyses:
            logger.error("No successful batch analyses")
            return None
        
        # Consolidate batch analyses into final report
        consolidated_analysis = self._consolidate_batch_analyses(batch_analyses)
        
        # Log analysis session
        self._log_analysis_session(video_path, saved_frame_paths, batch_analyses, consolidated_analysis)
        
        return consolidated_analysis
    
    def _predict(self, messages: list) -> str:
        """Make prediction using the transformer model.
        
        Args:
            messages: Chat messages including images and text
            
        Returns:
            Model response text
        """
        try:
            inputs = self.processor.apply_chat_template(
                messages,
                add_generation_prompt=True,
                tokenize=True,
                return_dict=True,
                return_tensors="pt",
            ).to(self.model.device)

            input_len = inputs["input_ids"].shape[-1]

            with torch.inference_mode():
                generation = self.model.generate(**inputs, max_new_tokens=1500, do_sample=False, use_cache=False)
                generation = generation[0][input_len:]

            return self.processor.decode(generation, skip_special_tokens=True)
            
        except Exception as e:
            logger.error(f"Error during transformer prediction: {e}")
            return None
    
    def _consolidate_batch_analyses(self, batch_analyses: List[dict]) -> str:
        """Consolidate multiple batch analyses into a single comprehensive report.
        
        Args:
            batch_analyses: List of batch analysis results
            
        Returns:
            Consolidated analysis text
        """
        try:
            # Prepare consolidation prompt
            analyses_text = "\n\n".join([
                f"Time Segment {batch['batch_number']}: {batch['analysis']}"
                for batch in batch_analyses
            ])
            
            consolidation_prompt = (
                f"Consolidate these surveillance analysis segments into a final security assessment. "
                f"Report only facts observed in the footage - do not speculate or create fictional details. "
                f"Summarize: personnel, movements, vehicles, objects, and any documented security concerns. "
                f"Base conclusions only on what was actually reported:\n\n{analyses_text}\n\n"
                f"Security Assessment:"
            )
            
            messages = [
                {
                    "role": "system",
                    "content": [{"type": "text", "text": "You are a security analyst consolidating surveillance reports. Only report factual observations - never invent details."}]
                },
                {
                    "role": "user",
                    "content": [{"type": "text", "text": consolidation_prompt}]
                }
            ]
            
            consolidated_result = self._predict(messages)
            
            if consolidated_result:
                logger.info("Batch analyses consolidated successfully")
                return consolidated_result
            else:
                logger.warning("Consolidation failed, using first batch analysis")
                return batch_analyses[0]['analysis']
                
        except Exception as e:
            logger.error(f"Error consolidating batch analyses: {e}")
            return batch_analyses[0]['analysis'] if batch_analyses else "Analysis failed"
    
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
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
            log_filename = f"transformer_analysis_session_{timestamp}.json"
            log_path = self.analysis_logs_dir / log_filename
            
            session_data = {
                "timestamp": datetime.now().isoformat() + "Z",
                "video_path": video_path,
                "model_used": self.model_name,
                "processing_method": "transformer_batch_analysis",
                "batch_size": 1,
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
            
            logger.info(f"Transformer analysis session logged to {log_path}")
            
        except Exception as e:
            logger.error(f"Error logging transformer analysis session: {e}")
    
    def _calculate_confidence(self, analysis_text: str, keywords: List[str]) -> float:
        """Calculate confidence score based on analysis quality.
        
        Args:
            analysis_text: Analysis text
            keywords: Extracted keywords
            
        Returns:
            Confidence score between 0 and 1
        """
        confidence = 0.5  # Base confidence
        
        # Increase confidence based on analysis length and detail
        if len(analysis_text) > 100:
            confidence += 0.2
        if len(analysis_text) > 200:
            confidence += 0.1
        
        # Increase confidence based on keywords
        if len(keywords) > 0:
            confidence += 0.1
        if len(keywords) > 3:
            confidence += 0.1
        
        return min(confidence, 1.0)
    
    def _create_failed_analysis(self, video_clip: VideoClip, error_message: str) -> SecurityAnalysis:
        """Create a failed analysis result.
        
        Args:
            video_clip: The video clip that failed analysis
            error_message: Error description
            
        Returns:
            SecurityAnalysis object indicating failure
        """
        return SecurityAnalysis(
            video_clip=video_clip,
            analysis_text=f"Analysis failed: {error_message}",
            threat_level=SecurityThreatLevel.LOW,
            confidence=0.0,
            keywords=["analysis_failed"],
            timestamp=datetime.now()
        )
    
    def check_ollama_health(self) -> bool:
        """Check if the transformer model is loaded and ready.
        
        Returns:
            True if model is ready, False otherwise
        """
        try:
            return self.model is not None and self.processor is not None
        except Exception:
            return False


class TransformerThreatEvaluator:
    """Threat evaluator using transformer model for text-only analysis."""
    
    def __init__(self, model, processor):
        """Initialize with transformer model and processor."""
        self.model = model
        self.processor = processor
    
    def evaluate_threat_level(self, analysis_text: str) -> SecurityThreatLevel:
        """Evaluate threat level from analysis text using transformer.
        
        Args:
            analysis_text: Security analysis text
            
        Returns:
            SecurityThreatLevel
        """
        try:
            threat_prompt = (
                f"Based on this security analysis, classify the threat level as LOW, MEDIUM, HIGH, or CRITICAL. "
                f"Base classification only on what was actually observed - do not assume or invent threats:\n\n"
                f"{analysis_text}\n\n"
                f"Respond with only one word: LOW, MEDIUM, HIGH, or CRITICAL"
            )
            
            messages = [
                {
                    "role": "system",
                    "content": [{"type": "text", "text": "You are a security threat classifier. Base classifications only on observed facts - never assume threats."}]
                },
                {
                    "role": "user",
                    "content": [{"type": "text", "text": threat_prompt}]
                }
            ]
            
            result = self._predict(messages)
            
            if result:
                result_upper = result.strip().upper()
                if "CRITICAL" in result_upper:
                    return SecurityThreatLevel.CRITICAL
                elif "HIGH" in result_upper:
                    return SecurityThreatLevel.HIGH
                elif "MEDIUM" in result_upper:
                    return SecurityThreatLevel.MEDIUM
                else:
                    return SecurityThreatLevel.LOW
            
        except Exception as e:
            logger.warning(f"Error evaluating threat level with transformer: {e}")
        
        return SecurityThreatLevel.LOW
    
    def extract_keywords(self, analysis_text: str) -> List[str]:
        """Extract keywords from analysis text using transformer.
        
        Args:
            analysis_text: Security analysis text
            
        Returns:
            List of keywords
        """
        try:
            keyword_prompt = (
                f"Extract 3-5 relevant security keywords from this analysis based only on what was actually observed. "
                f"Do not add keywords for details that were not mentioned. "
                f"Respond with only the keywords separated by commas:\n\n{analysis_text}"
            )
            
            messages = [
                {
                    "role": "system",
                    "content": [{"type": "text", "text": "You are a keyword extractor. Extract only keywords for details that were actually mentioned - never add fictional keywords."}]
                },
                {
                    "role": "user",
                    "content": [{"type": "text", "text": keyword_prompt}]
                }
            ]
            
            result = self._predict(messages)
            
            if result:
                keywords = [kw.strip() for kw in result.split(',')]
                return [kw for kw in keywords if kw and len(kw) > 1][:5]
            
        except Exception as e:
            logger.warning(f"Error extracting keywords with transformer: {e}")
        
        return ["surveillance", "monitoring"]
    
    def _predict(self, messages: list) -> str:
        """Make prediction using the transformer model."""
        try:
            inputs = self.processor.apply_chat_template(
                messages,
                add_generation_prompt=True,
                tokenize=True,
                return_dict=True,
                return_tensors="pt",
            ).to(self.model.device)

            input_len = inputs["input_ids"].shape[-1]

            with torch.inference_mode():
                generation = self.model.generate(**inputs, max_new_tokens=1500, do_sample=False, use_cache=False)
                generation = generation[0][input_len:]

            return self.processor.decode(generation, skip_special_tokens=True)
            
        except Exception as e:
            logger.error(f"Error during transformer prediction: {e}")
            return None
