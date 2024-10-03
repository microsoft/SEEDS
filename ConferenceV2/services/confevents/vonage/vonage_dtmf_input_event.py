
from enum import Enum
from typing import Any
from pydantic import BaseModel, Field, validator

from services.confevents.dtmf_input_event import DTMFInputEvent
from services.conference_call import ConferenceCall

class ChannelNumber(BaseModel):
    type: str
    number: str

class Channel(BaseModel):
    id: str
    type: str
    to: ChannelNumber
    from_: ChannelNumber = Field(..., alias="from")

    class Config:
        populate_by_name = True

class Body(BaseModel):
    digit: str
    duration: int
    channel: Channel
    dtmf_seq: int

class VonageRTCEventType(str, Enum):
    DTMF = "audio:dtmf"
    UNKNOWN = "ringing"

class VonageDTMFInputEvent(BaseModel):
    body: Body
    type: VonageRTCEventType

    @validator('type', pre=True)
    def validate_enum_field(cls, value: Any) -> VonageRTCEventType:
        if value not in VonageRTCEventType.__members__.values():
            return VonageRTCEventType.UNKNOWN
        return VonageRTCEventType(value)

    def get_conf_dtmf_input_event(self, conf_call: ConferenceCall) -> DTMFInputEvent:
        return DTMFInputEvent(
            phone_number=self.body.channel.to.number,
            digit=self.body.digit,
            conf_call=conf_call
        )
