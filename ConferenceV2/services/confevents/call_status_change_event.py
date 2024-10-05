from models.participant import CallStatus, Participant
from services.conference_call import ConferenceCall
from services.confevents.base_event import ConferenceEvent


class CallStatusChangeEvent(ConferenceEvent):
    def __init__(self, phone_number: str, status: CallStatus, conf_call: ConferenceCall):
        self.phone_number = phone_number
        self.status = status
        self.conf_call = conf_call

    async def execute_event(self):
        if self.phone_number in self.conf_call.state.participants:
            print("EXECUTING CALL STATUS CHANGE EVENT FOR NUMBER", self.phone_number, "STATUS:", self.status.value)
            participant: Participant = self.conf_call.state.participants[self.phone_number]
            participant.call_status = self.status
            await self.conf_call.update_state()
