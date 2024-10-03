from datetime import datetime
from pydantic import BaseModel

from models.action_history import ActionHistory, ActionType
from models.audio_content_state import ContentStatus
from services.conference_call import ConferenceCall


class PlayContentEvent(BaseModel):
    url: str = "/home/kavyansh/SEEDS/ConferenceV2/audio_test.wav"
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True
    
    async def execute_event(self):
        self.conf_call.state.audio_content_state.current_url = self.url
        self.conf_call.state.audio_content_state.status = ContentStatus.PLAYING
        # await self.communication_api.play_audio(url)
        await self.conf_call.websocket_service.play(self.url)
        self.conf_call.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE, 
                                                    metadata={
                                                        "playback_status": self.conf_call.state.audio_content_state.model_dump()
                                                    }, 
                                                    owner=self.conf_call.state.teacher_phone_number
                                                 )
                                    )
        await self.conf_call.update_state()