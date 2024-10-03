
from pydantic import BaseModel
from services.confevents.base_event import ConferenceEvent
from models.participant import CallStatus, Participant
from services.conference_call import ConferenceCall


class CallStatusChangeEvent(ConferenceEvent, BaseModel):
    phone_number: str = ""
    status: CallStatus = CallStatus.DISCONNECTED
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True

    async def execute_event(self):
        if self.phone_number in self.conf_call.state.participants:
            participant: Participant = self.conf_call.state.participants[self.phone_number]
            participant.call_status = self.status
            await self.conf_call.update_state()