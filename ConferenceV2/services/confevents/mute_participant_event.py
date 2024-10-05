from datetime import datetime
from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall


class MuteParticipantEvent:
    def __init__(self, phone_number: str, conf_call: ConferenceCall):
        self.phone_number = phone_number
        self.conf_call = conf_call

    async def execute_event(self):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already muted
        if self.phone_number in self.conf_call.state.participants:
            # Mute the participant using communication API
            await self.conf_call.communication_api.mute_participant(self.phone_number)
            
            # Update the participant's muted status
            self.conf_call.state.participants[self.phone_number].is_muted = True
            
            # Log the action in the action history
            self.conf_call.state.action_history.append(
                ActionHistory(
                    timestamp=datetime.now().isoformat(),
                    action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT,
                    metadata={
                        "phone_number": self.phone_number,
                        "is_muted": True
                    },
                    owner=self.conf_call.state.teacher_phone_number
                )
            )
            
            # Update the conference call state
            await self.conf_call.update_state()
