# models/participant.py

from pydantic import BaseModel, Field
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
    raised_at: int = Field(default=-1)
    is_raised: bool = Field(default=False)
    is_muted: bool = Field(default=False)
    call_status: CallStatus = Field(default=CallStatus.DISCONNECTED)

    class Config:
        use_enum_values = True  # Automatically use enum values instead of objects for serialization
        json_encoders = {
            Enum: lambda e: e.value,  # Encode enums as their values (this handles your enums like Role)
        }
