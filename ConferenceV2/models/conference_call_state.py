# models/conference_call_state.py

from pydantic import BaseModel
from typing import Dict, List

from models.action_history import ActionHistory
from models.audio_content_state import AudioContentState
from models.participant import Participant


class ConferenceCallState(BaseModel):
    call_status: str = None
    teacher_phone_number: str = None
    participants: Dict[str, Participant] = {}
    audio_content_state = AudioContentState()
    action_history: List[ActionHistory] = []
