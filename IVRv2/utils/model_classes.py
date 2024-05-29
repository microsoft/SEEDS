from dataclasses import dataclass
from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

from utils.enums import CallStatus, ConversationRTCEventType

class UserAction(BaseModel):
    key_pressed: str
    timestamp: datetime

class StreamPlaybackInfo(BaseModel):
    play_id: str
    stream_url: str
    started_at: datetime
    stopped_at: Optional[datetime] = None
    done_at: Optional[datetime] = None
    
class IVRCallStateMongoDoc(BaseModel):
    id: str = Field(..., alias="_id")
    phone_number: str
    fsm_id: str
    current_state_id: str
    created_at: datetime
    stopped_at: Optional[datetime] = None
    duration: str | None = ""
    user_actions: List[UserAction] = []
    stream_playback: List[StreamPlaybackInfo] = []
    experience_data: Dict[str, Any] = {}
    
    def dict(self, **kwargs):
        # Use the super().dict() method with by_alias=True to use aliases in the output dictionary
        return super().dict(by_alias=True, **kwargs)

    class Config:
        # This will allow the model to be instantiated with 'id' instead of '_id'
        populate_by_name = True

class IVRfsmDoc(BaseModel):
    id: str = Field(..., alias="_id")
    created_at: int
    states: List[Dict]
    transitions: List[Dict]
    init_state_id: str
    
    def dict(self, **kwargs):
        # Use the super().dict() method with by_alias=True to use aliases in the output dictionary
        return super().dict(by_alias=True, **kwargs)

    class Config:
        # This will allow the model to be instantiated with 'id' instead of '_id'
        populate_by_name = True
    
    def __eq__(self, other):
        if not isinstance(other, IVRfsmDoc):
            return NotImplemented
        
        return (self.states == other.states and 
                self.transitions == other.transitions and 
                self.init_state_id == other.init_state_id)
    

class VonageCallStartResponse(BaseModel):
    uuid: str = ""
    status: str = ""
    direction: str = ""
    conversation_uuid: str = ""
    
class StartIVRFormData(BaseModel):
    sender: str

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

class ConversationRTCWebhookRequest(BaseModel):
    body: Dict[str, Any]  # Allows any structure
    application_id: str
    timestamp: datetime
    type: ConversationRTCEventType
    conversation_id: str = "DEFAULT"
    id: int = -1

class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails
    conversation_uuid: str
    
class StartIVRRequest(BaseModel):
    phone_number: str
    
@dataclass
class MongoCreds:
    host: str
    password: str
    port: int
    user_name: str


class Option(BaseModel):
    key: int
    value: str

class Menu(BaseModel):
    description: str
    options: Optional[List[Option]] = None
    level: int

