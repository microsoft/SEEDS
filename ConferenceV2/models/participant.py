# models/participant.py

from pydantic import BaseModel
from enum import Enum
from typing import Optional

class Role(str, Enum):
    TEACHER = "Teacher"
    STUDENT = "Student"


class CallStatus(str, Enum):
    CONNECTED = "connected"
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"


class Participant(BaseModel):
    name: str
    phone_number: str
    role: Role
    is_raised: bool = False
    is_muted: bool = False
    call_status: CallStatus = CallStatus.DISCONNECTED

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
        json_encoders = {
            Enum: lambda e: e.value,  # Encode enums as their values (this handles your enums like Role)
        }
