# models/content_manager.py

from pydantic import BaseModel, Field
from enum import Enum
from typing import Optional
from datetime import datetime


class ContentStatus(str, Enum):
    PLAYING = "Playing"
    PAUSED = "Paused"
    STOPPED = "Stopped"


class AudioContentState(BaseModel):
    current_url: Optional[str] = None
    status: ContentStatus = Field(default=ContentStatus.STOPPED)
    paused_at: Optional[str] = None

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
