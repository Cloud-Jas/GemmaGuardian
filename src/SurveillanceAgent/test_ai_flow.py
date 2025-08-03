#!/usr/bin/env python3
"""
GemmaGuardian AI Model Flow Test Script
Tests both Ollama and Transformer modes using existing recorded videos
"""

import os
import sys
import json
import time
import argparse
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any

# Add the modules to Python path
sys.path.append(str(Path(__file__).parent))

def setup_environment():
    """Setup environment and imports"""
    try:
        # Import required modules
        from config.settings import load_settings
        from modules.infrastructure.ollama_client import OllamaSecurityAnalyzer
        from modules.infrastructure.gemma_transformer_client import GemmaTransformerSecurityAnalyzer
        from modules.domain.entities import SecurityAnalysis
        import cv2
        import base64
        
        return {
            'load_settings': load_settings,
            'OllamaSecurityAnalyzer': OllamaSecurityAnalyzer,
            'GemmaTransformerSecurityAnalyzer': GemmaTransformerSecurityAnalyzer,
            'SecurityAnalysis': SecurityAnalysis,
            'cv2': cv2,
            'base64': base64
        }
    except ImportError as e:
        print(f"❌ Failed to import modules: {e}")
        print("Make sure you're running this script from the SurveillanceAgent directory")
        sys.exit(1)

def get_available_videos() -> List[Path]:
    """Get list of available recorded videos"""
    recordings_dir = Path("recordings")
    if not recordings_dir.exists():
        print("❌ Recordings directory not found")
        return []
    
    video_files = list(recordings_dir.glob("*.mp4"))
    return sorted(video_files, reverse=True)  # Most recent first

def extract_frames_from_video(video_path: Path, modules: Dict) -> List[str]:
    """Extract frames from video for AI analysis"""
    print(f"🖼️ Extracting frames from {video_path.name}...")
    
    try:
        import cv2
        import base64
        import numpy as np
        
        # Open video
        cap = cv2.VideoCapture(str(video_path))
        if not cap.isOpened():
            print(f"❌ Failed to open video: {video_path}")
            return []
        
        frames = []
        frame_count = 0
        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_interval = int(fps * 2)  # Every 2 seconds
        
        print(f"📊 Video info: FPS={fps:.1f}, extracting every {frame_interval} frames")
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            if frame_count % frame_interval == 0:
                # Resize frame to 1024x1024
                resized_frame = cv2.resize(frame, (1024, 1024))
                
                # Convert to base64
                _, buffer = cv2.imencode('.jpg', resized_frame)
                frame_base64 = base64.b64encode(buffer).decode('utf-8')
                frames.append(frame_base64)
                
                if len(frames) >= 12:  # Limit to 12 frames for testing
                    break
            
            frame_count += 1
        
        cap.release()
        print(f"✅ Extracted {len(frames)} frames")
        return frames
        
    except Exception as e:
        print(f"❌ Error extracting frames: {e}")
        return []

def test_ollama_mode(frames: List[str], modules: Dict, video_path: str) -> Dict[str, Any]:
    """Test Ollama mode analysis"""
    print("\n🌐 Testing Ollama Mode...")
    
    try:
        settings = modules['load_settings']()
        
        # Initialize Ollama client
        ollama_client = modules['OllamaSecurityAnalyzer'](
            ollama_url=getattr(settings, 'ollama_url', 'http://localhost:11434'),
            model_name=getattr(settings, 'ollama_model', 'gemma3:4b'),
            text_model_name=getattr(settings, 'ollama_text_model', 'gemma3:4b')
        )
        
        # Test server connectivity
        print("🔌 Testing Ollama server connectivity...")
        if not ollama_client.check_ollama_health():
            return {
                'success': False,
                'error': 'Failed to connect to Ollama server',
                'details': 'Make sure Ollama is running: ollama serve'
            }
        
        print("✅ Ollama server connected successfully")
        
        # Create a VideoClip object for analysis
        from modules.domain.entities import VideoClip, PersonDetection, BoundingBox
        from datetime import datetime
        import os
        import cv2
        
        # Get video info
        cap = cv2.VideoCapture(video_path)
        frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        fps = cap.get(cv2.CAP_PROP_FPS)
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        duration = frame_count / fps if fps > 0 else 30.0
        cap.release()
        
        # Create a dummy PersonDetection for the trigger
        dummy_detection = PersonDetection(
            timestamp=datetime.now(),
            bounding_box=BoundingBox(x=100, y=100, width=50, height=100, confidence=0.8),
            frame_number=1,
            confidence=0.8
        )
        
        video_clip = VideoClip(
            file_path=video_path,
            start_time=datetime.now(),
            duration=duration,
            frame_count=frame_count,
            resolution=(width, height),
            trigger_detection=dummy_detection
        )
        
        print(f"🎬 Analyzing video clip: {os.path.basename(video_path)}")
        print(f"📊 Video duration: {duration:.1f}s, {frame_count} frames")
        print(f"📐 Resolution: {width}x{height}")
        
        # Analyze the video using the full video analysis pipeline
        start_time = time.time()
        try:
            analysis_result = ollama_client.analyze_video(video_clip)
            processing_time = time.time() - start_time
            
            print(f"✅ Analysis completed in {processing_time:.1f}s")
            print(f"� Threat level: {analysis_result.threat_level}")
            print(f"🎯 Confidence: {analysis_result.confidence:.2f}")
            print(f"🏷️ Keywords: {', '.join(analysis_result.keywords[:5])}")  # Show first 5 keywords
            
            return {
                'success': True,
                'mode': 'ollama',
                'analysis': analysis_result.analysis_text,
                'threat_level': analysis_result.threat_level.value,
                'confidence': analysis_result.confidence,
                'keywords': analysis_result.keywords,
                'processing_time': processing_time,
                'frames_analyzed': len(frames)
            }
            
        except Exception as e:
            print(f"❌ Video analysis failed: {e}")
            return {
                'success': False,
                'error': f'Video analysis failed: {str(e)}'
            }
            
    except Exception as e:
        return {
            'success': False,
            'error': f'Ollama mode test failed: {str(e)}'
        }

def test_transformer_mode(frames: List[str], modules: Dict) -> Dict[str, Any]:
    """Test Transformer mode analysis"""
    print("\n🔥 Testing Transformer Mode...")
    
    try:
        settings = modules['load_settings']()
        
        # Initialize Transformer client
        transformer_client = modules['GemmaTransformerSecurityAnalyzer'](
            model_name=getattr(settings, 'transformer_model', 'google/gemma-3n-e2b-it'),
            device=getattr(settings, 'transformer_device', 'auto'),
            resolution=int(getattr(settings, 'transformer_resolution', '512'))
        )
        
        # Model is loaded during initialization
        print("✅ Transformer model loaded successfully")
        
        # Get hardware info
        import torch
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            print(f"🎮 Using GPU: {gpu_name} ({gpu_memory:.1f}GB)")
        else:
            print("💻 Using CPU processing")
        
        # Process frames in batches
        batch_size = 4
        batches = [frames[i:i+batch_size] for i in range(0, len(frames), batch_size)]
        
        print(f"📦 Processing {len(batches)} batches of frames...")
        
        batch_analyses = []
        start_time = time.time()
        
        for i, batch in enumerate(batches):
            print(f"🧠 Processing batch {i+1}/{len(batches)} ({len(batch)} frames)...")
            try:
                analysis = transformer_client.analyze_frames_batch(batch)
                batch_analyses.append(analysis)
                print(f"✅ Batch {i+1} completed ({len(analysis)} chars)")
                
                # Clear GPU cache periodically
                if torch.cuda.is_available():
                    torch.cuda.empty_cache()
                    
            except Exception as e:
                print(f"⚠️ Batch {i+1} failed: {e}")
                batch_analyses.append(f"Batch {i+1} analysis failed: {str(e)}")
        
        # Consolidate results
        print("🔗 Consolidating batch results...")
        try:
            consolidated_analysis = transformer_client.consolidate_analyses(batch_analyses)
            processing_time = time.time() - start_time
            
            return {
                'success': True,
                'mode': 'transformer',
                'analysis': consolidated_analysis,
                'processing_time': processing_time,
                'batches_processed': len(batches),
                'frames_analyzed': len(frames),
                'gpu_used': torch.cuda.is_available() if 'torch' in locals() else False
            }
        except Exception as e:
            return {
                'success': False,
                'error': f'Consolidation failed: {str(e)}',
                'batch_analyses': batch_analyses
            }
            
    except Exception as e:
        return {
            'success': False,
            'error': f'Transformer mode test failed: {str(e)}'
        }

def save_test_results(results: Dict[str, Any], video_name: str):
    """Save test results to file"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    results_file = Path(f"test_results_{video_name}_{timestamp}.json")
    
    with open(results_file, 'w') as f:
        json.dump(results, indent=2, fp=f, default=str)
    
    print(f"📄 Test results saved to: {results_file}")

def print_comparison(ollama_result: Dict, transformer_result: Dict):
    """Print comparison between both modes"""
    print("\n" + "="*80)
    print("📊 COMPARISON RESULTS")
    print("="*80)
    
    # Success status
    print(f"🌐 Ollama Mode:     {'✅ SUCCESS' if ollama_result.get('success') else '❌ FAILED'}")
    print(f"🔥 Transformer Mode: {'✅ SUCCESS' if transformer_result.get('success') else '❌ FAILED'}")
    
    if ollama_result.get('success') and transformer_result.get('success'):
        # Performance comparison
        print(f"\n⏱️ Processing Time:")
        print(f"   Ollama:      {ollama_result.get('processing_time', 0):.1f} seconds")
        print(f"   Transformer: {transformer_result.get('processing_time', 0):.1f} seconds")
        
        # Frames processed
        print(f"\n🖼️ Frames Analyzed:")
        print(f"   Ollama:      {ollama_result.get('frames_analyzed', 0)} frames")
        print(f"   Transformer: {transformer_result.get('frames_analyzed', 0)} frames")
        
        # Analysis length
        ollama_analysis = ollama_result.get('analysis', '')
        transformer_analysis = transformer_result.get('analysis', '')
        print(f"\n📝 Analysis Length:")
        print(f"   Ollama:      {len(ollama_analysis)} characters")
        print(f"   Transformer: {len(transformer_analysis)} characters")
        
        # GPU usage
        if transformer_result.get('gpu_used'):
            print(f"\n🎮 GPU: Used by Transformer mode")
        else:
            print(f"\n💻 GPU: Not available/used")
    
    # Error details
    if not ollama_result.get('success'):
        print(f"\n❌ Ollama Error: {ollama_result.get('error', 'Unknown error')}")
    
    if not transformer_result.get('success'):
        print(f"\n❌ Transformer Error: {transformer_result.get('error', 'Unknown error')}")

def main():
    """Main test function"""
    parser = argparse.ArgumentParser(description='Test GemmaGuardian AI modes with recorded videos')
    parser.add_argument('--video', type=str, help='Specific video file to test (default: most recent)')
    parser.add_argument('--mode', choices=['ollama', 'transformer', 'both'], default='both',
                       help='Which AI mode to test (default: both)')
    parser.add_argument('--frames', type=int, default=12, help='Maximum frames to extract (default: 12)')
    
    args = parser.parse_args()
    
    print("🧪 GemmaGuardian AI Model Flow Test")
    print("="*50)
    
    # Setup environment
    modules = setup_environment()
    
    # Get available videos
    videos = get_available_videos()
    if not videos:
        print("❌ No recorded videos found in recordings/ directory")
        sys.exit(1)
    
    # Select video
    if args.video:
        video_path = Path("recordings") / args.video
        if not video_path.exists():
            print(f"❌ Video not found: {video_path}")
            sys.exit(1)
    else:
        video_path = videos[0]  # Most recent
    
    print(f"🎬 Testing with video: {video_path.name}")
    print(f"📁 Video size: {video_path.stat().st_size / (1024*1024):.1f} MB")
    
    # Extract frames
    frames = extract_frames_from_video(video_path, modules)
    if not frames:
        print("❌ Failed to extract frames from video")
        sys.exit(1)
    
    # Limit frames if requested
    if args.frames and len(frames) > args.frames:
        frames = frames[:args.frames]
        print(f"📏 Limited to {args.frames} frames for testing")
    
    # Test results
    test_results = {
        'video_file': video_path.name,
        'timestamp': datetime.now().isoformat(),
        'frames_extracted': len(frames),
        'test_mode': args.mode
    }
    
    # Test Ollama mode
    if args.mode in ['ollama', 'both']:
        ollama_result = test_ollama_mode(frames, modules, str(video_path))
        test_results['ollama'] = ollama_result
    
    # Test Transformer mode
    if args.mode in ['transformer', 'both']:
        transformer_result = test_transformer_mode(frames, modules)
        test_results['transformer'] = transformer_result
    
    # Print results
    if args.mode == 'both':
        print_comparison(test_results.get('ollama', {}), test_results.get('transformer', {}))
    else:
        result = test_results.get(args.mode, {})
        if result.get('success'):
            print(f"\n✅ {args.mode.title()} mode test completed successfully!")
            print(f"⏱️ Processing time: {result.get('processing_time', 0):.1f} seconds")
            print(f"📝 Analysis length: {len(result.get('analysis', ''))} characters")
        else:
            print(f"\n❌ {args.mode.title()} mode test failed: {result.get('error', 'Unknown error')}")
    
    # Save results
    save_test_results(test_results, video_path.stem)
    
    print("\n🎉 Test completed!")
    print(f"📄 Results saved for future reference")

if __name__ == "__main__":
    main()
