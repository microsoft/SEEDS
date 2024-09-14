# models/event.py

from enum import Enum
from typing import Optional, Dict, Any
from pydantic import BaseModel


class EventType(str, Enum):
    PARTICIPANT_STATUS = "participant_status"
    DTMF_INPUT = "dtmf_input"
    AUDIO_PLAYBACK = "audio_playback"
    MUTE_UNMUTE = "mute_unmute"
    # Add more event types as needed


class WebHookEvent(BaseModel):
    conference_id: str
    event_type: EventType
    participant_phone: Optional[str] = None
    data: Dict[str, Any]
