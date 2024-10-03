from datetime import datetime
from pydantic import BaseModel

from models.action_history import ActionHistory, ActionType
from models.participant import CallStatus, Participant, Role
from services.conference_call import ConferenceCall


class AddPartcipantEvent(BaseModel):
    phone_number: str
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True
    
    async def execute_event(self):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already connected
        participant = Participant(
            name="Student",
            phone_number=self.phone_number,
            role=Role.STUDENT,
            call_status=CallStatus.CONNECTING,
        )
        await self.conf_call.communication_api.add_participant(self.phone_number)

        self.conf_call.state.participants[self.phone_number] = participant
        self.conf_call.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_ADD_STUDENT, 
                                                    metadata={
                                                        "phone_number": self.phone_number
                                                    }, 
                                                    owner=self.conf_call.state.teacher_phone_number
                                                 )
                                    )
        await self.conf_call.update_state()