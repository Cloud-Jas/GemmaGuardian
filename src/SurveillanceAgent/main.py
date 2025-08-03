#!/usr/bin/env python3
"""
Main entry point for the RTSP Security Monitor application.
"""

import sys
import time
import argparse
import threading
import asyncio
from pathlib import Path

# Add the project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from loguru import logger
from config.settings import load_settings, setup_logging
from config.dependency_injection import get_container, cleanup_container


def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="RTSP Security Monitor with Person Detection and AI Analysis"
    )
    parser.add_argument(
        "--preview", 
        action="store_true",
        help="Enable live preview window with detection overlays"
    )
    parser.add_argument(
        "--visual",
        action="store_true", 
        help="Enable visual monitoring mode (alias for --preview)"
    )
    parser.add_argument(
        "--mode",
        choices=["ollama", "transformer"],
        default="ollama",
        help="AI analysis mode: 'ollama' for Ollama server mode, 'transformer' for direct transformer mode (default: ollama)"
    )
    return parser.parse_args()


def check_dependencies(ai_mode: str = "ollama") -> bool:
    """Check if all required dependencies are available.
    
    Args:
        ai_mode: AI analysis mode to check dependencies for
    
    Returns:
        True if all dependencies are available, False otherwise
    """
    try:
        # Check OpenCV
        import cv2
        logger.info(f"OpenCV version: {cv2.__version__}")
        
        # Suppress OpenCV verbose warnings for codec issues (if available)
        try:
            cv2.setLogLevel(cv2.LOG_LEVEL_ERROR)
        except AttributeError:
            # Older OpenCV versions don't have this attribute
            logger.info("OpenCV log level setting not available in this version")
        
        # Check if FFmpeg is available
        import subprocess
        result = subprocess.run(
            ["ffmpeg", "-version"],
            capture_output=True,
            timeout=10
        )
        if result.returncode != 0:
            logger.error("FFmpeg not found. Please install FFmpeg.")
            return False
        
        logger.info("FFmpeg is available")
        
        # Check mode-specific dependencies
        if ai_mode == "transformer":
            logger.info("Checking transformer mode dependencies...")
            try:
                import torch
                import transformers
                import timm
                import av
                from PIL import Image
                logger.success("✅ Transformer dependencies available")
                
                # Check CUDA availability
                if torch.cuda.is_available():
                    logger.info(f"🔥 CUDA available: {torch.cuda.get_device_name(0)}")
                else:
                    logger.info("💻 Using CPU for transformer processing")
                    
            except ImportError as e:
                logger.error(f"Missing transformer dependency: {e}")
                logger.error("Please install with: pip install transformers timm av torch torchvision")
                return False
        else:
            logger.info("Checking Ollama mode dependencies...")
            # Check if Ollama is running (optional check for ollama mode)
            import requests
            try:
                response = requests.get("http://localhost:11434/api/tags", timeout=5)
                if response.status_code == 200:
                    logger.info("Ollama server is running")
                else:
                    logger.warning("Ollama server not responding. Make sure it's running.")
            except Exception:
                logger.warning("Could not connect to Ollama server. Make sure it's running.")
        
        return True
        
    except ImportError as e:
        logger.error(f"Missing required dependency: {e}")
        return False
    except Exception as e:
        logger.error(f"Dependency check failed: {e}")
        return False


def start_api_server():
    """Start the FastAPI server for mobile app connectivity in a separate thread."""
    def run_server():
        try:
            import uvicorn
            from modules.infrastructure.security_data_api import SecurityDataAPI
            
            logger.info("🌐 API server starting on http://0.0.0.0:8888")
            logger.info("📱 Mobile app can now connect for real-time data")
            
            # Create API instance and get FastAPI app
            api_service = SecurityDataAPI()
            
            # Run the FastAPI server
            uvicorn.run(
                api_service.app, 
                host="0.0.0.0", 
                port=8888, 
                log_level="info",
                access_log=False  # Reduce log noise
            )
        except Exception as e:
            logger.error(f"Failed to start API server: {e}")
    
    # Start the server in a separate daemon thread
    api_thread = threading.Thread(target=run_server, daemon=True)
    api_thread.start()
    return api_thread


def main():
    """Main function to run the security monitoring system."""
    try:
        # Parse command line arguments
        args = parse_arguments()
        enable_visual = args.preview or args.visual
        ai_mode = args.mode
        
        # Load configuration
        settings = load_settings()
        
        # Override AI mode from command line argument
        settings.ai_mode = ai_mode
        
        # Setup logging
        setup_logging(settings.log_level)
        
        logger.info("🔒 Security Monitoring System Starting...")
        logger.info(f"AI Mode: {ai_mode.upper()}")
        logger.info(f"RTSP URL: {settings.rtsp_url}")
        logger.info(f"Clip Duration: {settings.clip_duration}s")
        logger.info(f"Output Directory: {settings.clip_output_dir}")
        
        if ai_mode == "transformer":
            logger.info(f"Transformer Model: {settings.transformer_model}")
            logger.info(f"Transformer Device: {settings.transformer_device}")
        else:
            logger.info(f"Ollama URL: {settings.ollama_url}")
            logger.info(f"Ollama Model: {settings.ollama_model}")
            logger.info(f"Ollama Text Model: {settings.ollama_text_model}")
        
        if enable_visual:
            logger.info("Visual preview mode enabled - OpenCV window will display live feed")
        else:
            logger.info("Background monitoring mode - no visual output")
        
        # Check dependencies
        if not check_dependencies(ai_mode):
            logger.error("Dependency check failed. Please install missing dependencies.")
            sys.exit(1)
        
        # Initialize dependency injection container
        container = get_container(settings)
        
        # Get the main security monitor with visual mode if requested
        security_monitor = container.get_security_monitor(enable_visual_monitor=enable_visual)
        
        # Check AI service health based on mode
        analysis_service = container.get_security_analysis_service()
        if ai_mode == "transformer":
            if not analysis_service.check_ollama_health():  # Using same method name for consistency
                logger.warning("Transformer model health check failed. Video analysis may not work properly.")
                logger.warning("Make sure you have the required dependencies installed:")
                logger.warning("  pip install transformers timm av torch")
                logger.warning(f"  Model: {settings.transformer_model}")
            else:
                logger.success("✅ Transformer model loaded and ready")
        else:
            if not analysis_service.check_ollama_health():
                logger.warning("Ollama health check failed. Video analysis may not work properly.")
                logger.warning("Make sure Ollama is running and the model is available:")
                logger.warning(f"  ollama pull {settings.ollama_model}")
                logger.warning(f"  ollama pull {settings.ollama_text_model}")
            else:
                logger.success("✅ Ollama service is healthy and ready")
        
        # Start monitoring
        if security_monitor.start_monitoring():
            logger.success("🚀 Security monitoring system started successfully!")
            if enable_visual:
                logger.info("Live preview window should now be visible")
            
            # Start the API server for mobile app connectivity
            logger.info("🌐 Starting API server for mobile app...")
            api_thread = start_api_server()
            
            logger.info("Press Ctrl+C to stop the system")
            
            # Keep the main thread alive and periodically log status
            try:
                while security_monitor.is_running():
                    time.sleep(60)  # Check every minute
                    
                    # Log system status
                    status = security_monitor.get_status()
                    logger.info(
                        f"Status: Running={status['running']}, "
                        f"Recording={status['recording_in_progress']}, "
                        f"Analyzing={status['analysis_in_progress']}, "
                        f"Detections={status['total_detections']}, "
                        f"Clips={status['total_clips']}, "
                        f"Threats={status['threat_count']}, "
                        f"Pending={status['pending_analyses']}"
                    )
                    
            except KeyboardInterrupt:
                logger.info("Received shutdown signal...")
            
        else:
            logger.error("Failed to start security monitoring system")
            sys.exit(1)
            
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        sys.exit(1)
        
    finally:
        # Cleanup
        try:
            cleanup_container()
            logger.info("🔒 Security Monitoring System Stopped")
        except Exception as e:
            logger.error(f"Error during cleanup: {e}")


if __name__ == "__main__":
    main()
