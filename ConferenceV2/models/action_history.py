# models/action_history.py

from enum import Enum
from pydantic import BaseModel
from datetime import datetime
from typing import Dict

class ActionType(str, Enum):
    CONFERENCE_START = "Conference-Start"
    CONFERENCE_END = "Conference-End"
    CONFERENCE_SINK = "Conference-Sink"
    CONFERENCE_CALLSTATUS_CHANGE = "Conference-CallStatusChange"
    STUDENT_RAISE_HAND_STATE_CHANGE = "Student-RaiseHandStateChange"
    TEACHER_ADD_STUDENT = "Teacher-AddStudent"
    TEACHER_REMOVE_STUDENT = "Teacher-RemoveStudent"
    TEACHER_MUTE_UNMUTE_STUDENT = "Teacher-MuteUnmuteStudent"
    TEACHER_MUTE_ALL = "Teacher-MuteAll"
    TEACHER_UNMUTE_ALL = "Teacher-UnmuteAll"
    TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE = "Teacher-AudioPlaybackStatusChange"
    

class ActionHistory(BaseModel):
    timestamp: str
    action_type: ActionType
    metadata: Dict
    owner: str  # Phone number or identifier of the user who performed the action

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
        