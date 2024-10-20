from datetime import datetime
from typing import Callable
from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall

class SinkConferenceEvent:
    def __init__(self, conf_call: ConferenceCall, on_sink_callback: Callable[[], None]):
        self.conf_call = conf_call
        self.on_sink_callback = on_sink_callback

    async def execute_event(self):
        self.conf_call.state.is_running = False   
             
        # Log the action in the action history
        self.conf_call.state.action_history.append(
            ActionHistory(
                timestamp=datetime.now().isoformat(),
                action_type=ActionType.CONFERENCE_SINK,
                metadata={},
                owner=self.conf_call.state.teacher_phone_number
            )
        )
        
        self.conf_call.end_processing_conf_events_from_queue()
        await self.conf_call.connection_manager.disconnect(self.conf_call.state.get_teacher())
        if self.on_sink_callback:
            self.on_sink_callback()
        # Update the conference call state
        await self.conf_call.update_state()
