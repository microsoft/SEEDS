from datetime import datetime
from azure.identity.aio import DefaultAzureCredential
from azure.storage.blob.aio import BlobServiceClient
import os
from urllib.parse import urlparse
from models.action_history import ActionHistory, ActionType
from models.audio_content_state import ContentStatus
from services.conference_call import ConferenceCall

class PlayContentEvent:
    def __init__(self, conf_call: ConferenceCall, url: str = f"https://{os.environ.get("STORAGE_ACCOUNT_NAME", "")}.blob.core.windows.net/output-container/25/1.0.wav"):
        self.url = url
        self.conf_call = conf_call

    async def execute_event(self):
        # Update the audio content state with the current URL and status
        self.conf_call.state.audio_content_state.current_url = self.url
        self.conf_call.state.audio_content_state.status = ContentStatus.PLAYING

        # Play the audio via websocket service
        await self.conf_call.websocket_service.play(self.url)
        
        # Log the action in the action history
        self.conf_call.state.action_history.append(
            ActionHistory(
                timestamp=datetime.now().isoformat(),
                action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE,
                metadata={
                    "playback_status": self.conf_call.state.audio_content_state.__dict__  # Using __dict__ to mimic model_dump
                },
                owner=self.conf_call.state.teacher_phone_number
            )
        )
        
        # Update the conference call state
        await self.conf_call.update_state()
