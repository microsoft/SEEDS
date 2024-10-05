from datetime import datetime
from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall


class UnmuteParticipantEvent:
    def __init__(self, phone_number: str, conf_call: ConferenceCall):
        self.phone_number = phone_number
        self.conf_call = conf_call

    async def execute_event(self):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already unmuted
        if self.phone_number in self.conf_call.state.participants:
            participant = self.conf_call.state.participants[self.phone_number]
            
            # Unmute the participant via communication API
            await self.conf_call.communication_api.unmute_participant(self.phone_number)
            
            # Update participant's mute status
            participant.is_muted = False
            # Set raised hand to false
            participant.is_raised = False
            participant.raised_at = -1
            
            # Log the unmute action in the action history
            self.conf_call.state.action_history.append(
                ActionHistory(
                    timestamp=datetime.now().isoformat(),
                    action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT,
                    metadata={
                        "phone_number": self.phone_number,
                        "is_muted": False
                    },
                    owner=self.conf_call.state.teacher_phone_number
                )
            )
            
            # Update the conference call state
            await self.conf_call.update_state()
