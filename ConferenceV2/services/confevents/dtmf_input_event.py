from datetime import datetime
from models.action_history import ActionHistory, ActionType
from services.confevents.base_event import ConferenceEvent
from models.participant import Role, Participant
from services.conference_call import ConferenceCall


class DTMFInputEvent(ConferenceEvent):
    def __init__(self, phone_number: str, digit: str, conf_call: ConferenceCall):
        self.phone_number = phone_number
        self.digit = digit
        self.conf_call = conf_call

    async def execute_event(self):
        if self.phone_number in self.conf_call.state.participants:
            participant: Participant = self.conf_call.state.participants[self.phone_number]

            # Flip raise hand state: if participant is a student, input is "0", and hand is not already raised
            if participant.role == Role.STUDENT and self.digit == "0" and not participant.is_raised:
                print("HANDLING DTMF INPUT EVENT", self)
                participant.is_raised = True
                participant.raised_at = int(datetime.now().timestamp())
                
                # Append action history for the raised hand event
                self.conf_call.state.action_history.append(ActionHistory(
                    timestamp=datetime.now().isoformat(),
                    action_type=ActionType.STUDENT_RAISE_HAND_STATE_CHANGE,
                    metadata={
                        "phone_number": participant.phone_number,
                        "raised_hand": participant.is_raised,
                        "raised_at": participant.raised_at
                    },
                    owner=participant.phone_number
                ))

                # Update the conference call state
                await self.conf_call.update_state()
