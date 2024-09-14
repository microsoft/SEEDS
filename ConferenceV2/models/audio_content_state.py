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
    paused_at: Optional[datetime] = None
