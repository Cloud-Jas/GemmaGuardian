#!/usr/bin/env python3
"""
Test script to demonstrate both AI modes for security analysis.
"""

import argparse
import sys
from pathlib import Path

# Add the project root to Python path
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from loguru import logger
from config.settings import load_settings
from config.dependency_injection import get_container


def test_ai_mode(mode: str):
    """Test the specified AI mode."""
    logger.info(f"🧪 Testing AI mode: {mode.upper()}")
    
    try:
        # Load settings and override AI mode
        settings = load_settings()
        settings.ai_mode = mode
        
        # Get container and create security analysis service
        container = get_container(settings)
        analysis_service = container.get_security_analysis_service()
        
        # Check health
        if analysis_service.check_ollama_health():
            logger.success(f"✅ {mode.upper()} mode initialized successfully!")
            
            if mode == "transformer":
                logger.info("🔥 Transformer model ready for direct inference")
                logger.info(f"📦 Model: {settings.transformer_model}")
                logger.info(f"💻 Device: {settings.transformer_device}")
            else:
                logger.info("🌐 Ollama service ready for API calls")
                logger.info(f"🔗 URL: {settings.ollama_url}")
                logger.info(f"👁️ Vision Model: {settings.ollama_model}")
                logger.info(f"📝 Text Model: {settings.ollama_text_model}")
                
            return True
        else:
            logger.error(f"❌ {mode.upper()} mode failed to initialize")
            return False
            
    except Exception as e:
        logger.error(f"❌ Error testing {mode} mode: {e}")
        return False


def main():
    """Main test function."""
    parser = argparse.ArgumentParser(description="Test AI modes for security analysis")
    parser.add_argument(
        "--mode",
        choices=["ollama", "transformer", "both"],
        default="both",
        help="AI mode to test: 'ollama', 'transformer', or 'both' (default: both)"
    )
    
    args = parser.parse_args()
    
    logger.info("🔒 GemmaGuardian AI Mode Test")
    logger.info("=" * 50)
    
    success = True
    
    if args.mode == "both":
        # Test both modes
        logger.info("Testing both AI modes...")
        logger.info("")
        
        # Test Ollama mode
        ollama_success = test_ai_mode("ollama")
        logger.info("")
        
        # Test Transformer mode
        transformer_success = test_ai_mode("transformer")
        
        success = ollama_success and transformer_success
        
    else:
        # Test specific mode
        success = test_ai_mode(args.mode)
    
    logger.info("")
    logger.info("=" * 50)
    
    if success:
        logger.success("🎉 All tests passed! Both AI modes are ready.")
        logger.info("")
        logger.info("Usage examples:")
        logger.info("  Ollama mode:      python main.py --mode ollama")
        logger.info("  Transformer mode: python main.py --mode transformer")
        logger.info("  With preview:     python main.py --mode transformer --preview")
        logger.info("")
        logger.info("Full system:")
        logger.info("  python start_full_system.py --mode transformer")
    else:
        logger.error("❌ Some tests failed. Please check the error messages above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
