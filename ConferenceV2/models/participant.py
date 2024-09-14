# models/participant.py

from pydantic import BaseModel
from enum import Enum
from typing import Optional


class Role(str, Enum):
    TEACHER = "Teacher"
    STUDENT = "Student"


class CallStatus(str, Enum):
    CONNECTED = "Connected"
    DISCONNECTED = "Disconnected"
    CONNECTING = "Connecting"


class Participant(BaseModel):
    name: str
    phone_number: str
    role: Role
    is_raised: bool = False
    is_muted: bool = False
    call_status: CallStatus = CallStatus.DISCONNECTED
