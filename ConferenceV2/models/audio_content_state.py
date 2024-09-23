# models/content_manager.py

from pydantic import BaseModel
from enum import Enum
from typing import Optional
from datetime import datetime


class ContentStatus(str, Enum):
    PLAYING = "Playing"
    PAUSED = "Paused"
    STOPPED = "Stopped"


class AudioContentState(BaseModel):
    current_url: Optional[str] = None
    status: ContentStatus = ContentStatus.STOPPED
    paused_at: Optional[str] = None

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
        json_encoders = {
            Enum: lambda e: e.value,  # Encode enums as their values (this handles your enums like Role)
        }
