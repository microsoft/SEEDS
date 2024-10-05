from datetime import datetime
from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall


class RemoveParticipantEvent:
    def __init__(self, phone_number: str, conf_call: ConferenceCall):
        self.phone_number = phone_number
        self.conf_call = conf_call

    async def execute_event(self):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already removed
        if self.phone_number in self.conf_call.state.participants:
            # Remove the participant via communication API
            await self.conf_call.communication_api.remove_participant(self.phone_number)
            
            # Delete the participant from the conference state
            del self.conf_call.state.participants[self.phone_number]
            
            # Log the removal in the action history
            self.conf_call.state.action_history.append(
                ActionHistory(
                    timestamp=datetime.now().isoformat(),
                    action_type=ActionType.TEACHER_REMOVE_STUDENT,
                    metadata={
                        "phone_number": self.phone_number
                    },
                    owner=self.conf_call.state.teacher_phone_number
                )
            )
            
            # Update the conference call state
            await self.conf_call.update_state()
