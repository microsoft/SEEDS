from dataclasses import dataclass
from datetime import datetime
import json
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

from utils.enums import CallStatus, ConversationRTCEventType

class UserAction(BaseModel):
    key_pressed: str
    timestamp: datetime
    pre_state_id: str = 'pre-default'
    post_state_id: str = 'post-default'

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)

class StreamPlaybackInfo(BaseModel):
    play_id: str
    stream_url: str
    started_at: datetime
    stopped_at: Optional[datetime] = None
    done_at: Optional[datetime] = None

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)
    
class IVRCallStateMongoDoc(BaseModel):
    id: str
    phone_number: str
    fsm_id: str
    current_state_id: str
    created_at: datetime
    stopped_at: Optional[datetime] = None
    duration: str | None = ""
    user_actions: List[UserAction] = []
    stream_playback: List[StreamPlaybackInfo] = []
    experience_data: Dict[str, Any] = {}
    call_status_updates: Dict[str, Any] = {}

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)

class IVRfsmDoc(BaseModel):
    id: str
    created_at: int
    states: List[Dict]
    transitions: List[Dict]
    init_state_id: str
    
    def __eq__(self, other):
        if not isinstance(other, IVRfsmDoc):
            return NotImplemented
        
        return (self.states == other.states and 
                self.transitions == other.transitions and 
                self.init_state_id == other.init_state_id)

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)
    

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

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)
    
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

    def dict(self, *args, **kwargs):
        # Serialize the model to JSON and then parse it back to a Python dictionary
        json_string = self.json(*args, **kwargs)
        return json.loads(json_string)

class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails
    conversation_uuid: str
    
class StartIVRRequest(BaseModel):
    phone_number: str
    
class FSMRequest(BaseModel):
    fsm_id: str    
    
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

class BulkCallRequest(BaseModel):
    phone_numbers: List[str] = Field(..., description="List of phone numbers to call")
    content_ids: List[str] = Field(..., description="List of content IDs to fetch FSM")
