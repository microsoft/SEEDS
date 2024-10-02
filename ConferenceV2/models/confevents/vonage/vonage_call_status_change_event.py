from enum import Enum
from typing import Any

from pydantic import BaseModel, Field, validator

from models.confevents.call_status_change_event import CallStatusChangeEvent
from models.participant import CallStatus


class VonageCallStatus(str, Enum):
    STARTED = "started"
    RINGING = "ringing"
    ANSWERED = "answered"
    COMPLETED = "completed"
    NOTCONNECTED = "notconnected"


class VonageCallStatusChangeEvent(BaseModel):
    status: VonageCallStatus
    phone_number: str = Field(..., alias="to")

    @validator('status', pre=True)
    def validate_status_field(cls, value: Any) -> VonageCallStatus:
        if value not in VonageCallStatus.__members__.values():
            return VonageCallStatus.NOTCONNECTED
        return VonageCallStatus(value)

    class Config:
        populate_by_name = True  # Corrected from 'allow_population_by_field_name'
    
    def get_conf_call_status_change_event(self) -> CallStatusChangeEvent:
        status: CallStatus = None
        if self.status == VonageCallStatus.STARTED or self.status == VonageCallStatus.RINGING:
            status = CallStatus.CONNECTING
        elif self.status == VonageCallStatus.ANSWERED:
            status = CallStatus.CONNECTED
        else:
            status = CallStatus.DISCONNECTED

        return CallStatusChangeEvent(
            phone_number=self.phone_number,
            status=status
        )
