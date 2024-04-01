from dataclasses import dataclass
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum

class CallStatus(Enum):
    STARTED = "started"
    RINGING = "ringing"
    ANSWERED = "answered"
    BUSY = "busy"
    CANCELLED = "cancelled"
    UNANSWERED = "unanswered"
    DISCONNECTED = "disconnected"
    REJECTED = "rejected"
    FAILED = "failed"
    HUMAN = "human" 
    MACHINE = "machine"
    TIMEOUT = "timeout"
    COMPLETED = "completed"
    RECORD = "record"
    INPUT = "input"
    TRANSFER = "transfer"
    
    @staticmethod
    def get_end_call_enums():
        return [
            CallStatus.BUSY,
            CallStatus.CANCELLED,
            CallStatus.UNANSWERED,
            CallStatus.DISCONNECTED,
            CallStatus.REJECTED,
            CallStatus.FAILED,
            CallStatus.COMPLETED,
            CallStatus.TIMEOUT
        ]

class IVRCallStateMongoDoc(BaseModel):
    id: str = Field(..., alias="_id")
    createdAt: datetime
    current_state_id: str
    
    def dict(self, **kwargs):
        # Use the super().dict() method with by_alias=True to use aliases in the output dictionary
        return super().dict(by_alias=True, **kwargs)

    class Config:
        # This will allow the model to be instantiated with 'id' instead of '_id'
        populate_by_name = True
    

class EventWebhookRequest(BaseModel):
    end_time: str | None = ""
    network: str | None = ""
    duration: str | None = ""# This is represented as a string, but you might consider converting it to an integer or float if it represents time in seconds or similar
    start_time: str | None = ""
    rate: str | None = ""
    price: str | None = ""
    from_: str = Field(..., alias='from')  # 'from' is a Python reserved keyword, so we use an alias
    headers: dict = {}
    uuid: str
    to: str
    conversation_uuid: str
    status: CallStatus
    direction: str
    timestamp: str = ""
    
    class Config:
        # Specify custom JSON serialization for CallStatus
        json_encoders = {
            CallStatus: lambda v: v.value,  # Serialize CallStatus to its value
        }

class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails
    to: str
    
class StartIVRRequest(BaseModel):
    phone_number: str
    
@dataclass
class MongoCreds:
    host: str
    password: str
    port: int
    user_name: str
