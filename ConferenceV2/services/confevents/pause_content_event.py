from datetime import datetime
from pydantic import BaseModel
from models.action_history import ActionHistory, ActionType
from models.audio_content_state import ContentStatus
from services.conference_call import ConferenceCall

class PauseContentEvent(BaseModel):
    conf_call: ConferenceCall

    class Config:
        arbitrary_types_allowed=True
    
    async def execute_event(self):
        self.conf_call.state.audio_content_state.status = ContentStatus.PAUSED
        self.conf_call.state.audio_content_state.paused_at =  datetime.now().isoformat()
        await self.conf_call.websocket_service.pause()
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