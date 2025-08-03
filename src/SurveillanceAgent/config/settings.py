"""Configuration settings for the security monitoring system."""

import os
from pathlib import Path
from typing import Optional
from pydantic import Field
from pydantic_settings import BaseSettings
from loguru import logger


class SecurityMonitorSettings(BaseSettings):
    """Configuration settings using Pydantic for validation."""
    
        # RTSP Stream Configuration
    rtsp_url: str = Field(
        default="rtsp://admin:admin@192.168.0.100:554/ch0_0.264",
        description="RTSP stream URL"
    )
    
    # Recording Configuration  
    clip_duration: float = Field(
        default=60.0,
        ge=5.0,
        le=300.0,
        description="Duration of security clips in seconds"
    )
    
    clip_output_dir: str = Field(
        default="./recordings",
        description="Directory to save video clips"
    )
    
    # AI/ML Configuration
    ollama_url: str = Field(
        default="http://localhost:11434",
        description="Ollama server URL"
    )
    
    ollama_model: str = Field(
        default="gemma3:4b",
        description="Ollama model name for vision tasks"
    )
    
    ollama_text_model: str = Field(
        default="gemma3n:e4b",
        description="Ollama model name for text analysis tasks (threat evaluation, keyword extraction)"
    )
    
    # AI Mode Configuration
    ai_mode: str = Field(
        default="ollama",
        description="AI analysis mode: 'ollama' for Ollama server, 'transformer' for direct transformer"
    )
    
    # Transformer Configuration (for transformer mode)
    transformer_model: str = Field(
        default="google/gemma-3n-e2b-it",
        description="HuggingFace transformer model name for direct transformer mode"
    )
    
    transformer_device: str = Field(
        default="auto",
        description="Device for transformer model: 'auto' (GPU if available, else CPU), 'cuda', 'cpu'"
    )
    
    transformer_resolution: int = Field(
        default=512,
        description="Image resolution for transformer model processing (512x512 for efficiency)"
    )
    
    detection_confidence_threshold: float = Field(
        default=0.5,
        ge=0.0,
        le=1.0,
        description="Minimum confidence for person detection"
    )
    
    # Analysis Configuration
    analysis_prompt: str = Field(
        default=(
            "Analyze this video for security concerns. Look for suspicious activities, "
            "potential burglary attempts, unusual behavior, or any security threats. "
            "Provide a detailed assessment."
        ),
        description="Prompt for AI security analysis"
    )
    
    # System Configuration
    log_level: str = Field(
        default="INFO",
        description="Logging level (DEBUG, INFO, WARNING, ERROR)"
    )
    
    data_dir: str = Field(
        default="./data",
        description="Directory for data storage"
    )
    
    # Cleanup Configuration
    max_clip_age_days: int = Field(
        default=7,
        ge=1,
        description="Maximum age of clips to keep (days)"
    )
    
    # Performance Configuration
    max_concurrent_analyses: int = Field(
        default=2,
        ge=1,
        le=10,
        description="Maximum concurrent video analyses"
    )
    
    frame_processing_fps: float = Field(
        default=5.0,
        ge=1.0,
        le=30.0,
        description="Target FPS for frame processing"
    )
    
    # Mobile Notification Configuration
    enable_mobile_notifications: bool = Field(
        default=True,
        description="Enable mobile push notifications"
    )
    
    notification_min_threat_level: str = Field(
        default="MEDIUM",
        description="Minimum threat level for notifications (LOW, MEDIUM, HIGH, CRITICAL)"
    )
    
    mobile_notification_url: Optional[str] = Field(
        default=None,
        description="HTTP endpoint for mobile notifications (e.g., http://192.168.1.100:3000/notify)"
    )
    
    notification_webhook_urls: str = Field(
        default="",
        description="Comma-separated list of webhook URLs for notifications"
    )
    
    notification_server_port: int = Field(
        default=8888,
        ge=1024,
        le=65535,
        description="Port for notification server (serves videos and status)"
    )
    
    server_ip: Optional[str] = Field(
        default=None,
        description="Server IP address for notifications (auto-detected if not set)"
    )
    
    class Config:
        """Pydantic configuration."""
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False
        
    def __init__(self, **kwargs):
        """Initialize settings and create necessary directories."""
        super().__init__(**kwargs)
        self._create_directories()
        
    def _create_directories(self) -> None:
        """Create necessary directories if they don't exist."""
        try:
            Path(self.clip_output_dir).mkdir(parents=True, exist_ok=True)
            Path(self.data_dir).mkdir(parents=True, exist_ok=True)
            logger.info("Created necessary directories")
        except Exception as e:
            logger.error(f"Failed to create directories: {e}")
    
    @property
    def database_path(self) -> str:
        """Get the database file path."""
        return str(Path(self.data_dir) / "security_monitor.db")
    
    @property
    def video_clips_data_path(self) -> str:
        """Get the video clips data file path."""
        return str(Path(self.data_dir) / "video_clips.json")
    
    @property
    def analyses_data_path(self) -> str:
        """Get the analyses data file path."""
        return str(Path(self.data_dir) / "security_analyses.json")


def load_settings() -> SecurityMonitorSettings:
    """Load and validate configuration settings.
    
    Returns:
        Validated configuration settings
    """
    try:
        settings = SecurityMonitorSettings()
        logger.info("Configuration loaded successfully")
        return settings
    except Exception as e:
        logger.error(f"Failed to load configuration: {e}")
        raise


def setup_logging(log_level: str = "INFO") -> None:
    """Setup logging configuration.
    
    Args:
        log_level: Logging level to use
    """
    try:
        # Remove default logger
        logger.remove()
        
        # Add console logger with custom format
        logger.add(
            sink=lambda msg: print(msg, end=""),
            format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
                   "<level>{level: <8}</level> | "
                   "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
                   "<level>{message}</level>",
            level=log_level,
            colorize=True
        )
        
        # Add file logger
        log_dir = Path("logs")
        log_dir.mkdir(exist_ok=True)
        
        logger.add(
            sink=log_dir / "security_monitor_{time:YYYY-MM-DD}.log",
            format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}",
            level=log_level,
            rotation="1 day",
            retention="30 days",
            compression="zip"
        )
        
        logger.info(f"Logging setup complete with level: {log_level}")
        
    except Exception as e:
        print(f"Failed to setup logging: {e}")


# Global settings instance
settings: Optional[SecurityMonitorSettings] = None


def get_settings() -> SecurityMonitorSettings:
    """Get the global settings instance.
    
    Returns:
        Global settings instance
    """
    global settings
    if settings is None:
        settings = load_settings()
    return settings
