from datetime import datetime
from models.action_history import ActionHistory, ActionType
from models.audio_content_state import ContentStatus
from services.conference_call import ConferenceCall


class PauseContentEvent:
    def __init__(self, conf_call: ConferenceCall):
        self.conf_call = conf_call

    async def execute_event(self):
        audio_state = self.conf_call.state.audio_content_state
        
        # Update audio content state to paused
        audio_state.status = ContentStatus.PAUSED
        audio_state.paused_at = datetime.now().isoformat()
        
        # Pause the audio content via websocket service
        await self.conf_call.websocket_service.pause()
        
        # Log the action in the action history
        self.conf_call.state.action_history.append(
            ActionHistory(
                timestamp=datetime.now().isoformat(),
                action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE,
                metadata={
                    "playback_status": audio_state.__dict__  # Using __dict__ to mimic model_dump
                },
                owner=self.conf_call.state.teacher_phone_number
            )
        )
        
        # Update the conference call state
        await self.conf_call.update_state()
