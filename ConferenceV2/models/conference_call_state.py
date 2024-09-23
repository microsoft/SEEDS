# models/conference_call_state.py

from enum import Enum
from pydantic import BaseModel
from typing import Dict, List

from models.action_history import ActionHistory
from models.audio_content_state import AudioContentState
from models.participant import Participant, Role


class ConferenceCallState(BaseModel):
    call_status: str = None
    teacher_phone_number: str = None
    participants: Dict[str, Participant] = {}
    audio_content_state: AudioContentState = AudioContentState()
    action_history: List[ActionHistory] = []

    def get_teacher(self):
        if self.teacher_phone_number and self.teacher_phone_number in self.participants:
            return self.participants[self.teacher_phone_number]
        return None
    
    def get_students(self):
        return [partipant for partipant in self.participants.values() if partipant.role != Role.TEACHER]

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
        json_encoders = {
            Enum: lambda e: e.value,  # Encode enums as their values (this handles your enums like Role)
        }
