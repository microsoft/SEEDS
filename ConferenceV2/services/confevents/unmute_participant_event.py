from datetime import datetime
from pydantic import BaseModel

from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall


class UnmuteParticipantEvent(BaseModel):
    phone_number: str
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True
    
    async def execute_event(self):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already unmuted
        if self.phone_number in self.conf_call.state.participants:
            participant = self.conf_call.state.participants[self.phone_number]
            await self.conf_call.communication_api.unmute_participant(self.phone_number)
            participant.is_muted = False
            # SET RAISED HAND TO FALSE
            participant.is_raised = False
            participant.raised_at = -1

            self.conf_call.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT, 
                                                    metadata={
                                                        "phone_number": self.phone_number,
                                                        "is_muted": False
                                                    }, 
                                                    owner=self.conf_call.state.teacher_phone_number
                                                )
                                    )
            await self.conf_call.update_state()
