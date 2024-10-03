from datetime import datetime
from pydantic import BaseModel
from models.action_history import ActionHistory, ActionType
from services.confevents.base_event import ConferenceEvent
from models.participant import Role
from services.conference_call import ConferenceCall

class DTMFInputEvent(ConferenceEvent, BaseModel):
    phone_number: str = ""
    digit: str = ""
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True

    async def execute_event(self):
        if self.phone_number in self.conf_call.state.participants:
            participant = self.conf_call.state.participants[self.phone_number]

            # FLIP RAISE HAND STATE : PHONE NUMBER IS STUDENT AND INPUT IS 0 AND STUDENT HAND IS NOT ALREADY RAISED
            if participant.role == Role.STUDENT and self.digit == "0" and not participant.is_raised:
                print("HANDLING DTMF INPUT EVENT", self)
                participant.is_raised = True
                participant.raised_at = int(datetime.now().timestamp())
                self.conf_call.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.STUDENT_RAISE_HAND_STATE_CHANGE, 
                                                    metadata={
                                                        "phone_number": participant.phone_number,
                                                        "raised_hand": participant.is_raised,
                                                        "raised_at": participant.raised_at
                                                    }, 
                                                    owner=participant.phone_number
                                                )
                                )
                await self.conf_call.update_state()