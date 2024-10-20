from datetime import datetime
from models.action_history import ActionHistory, ActionType
from services.conference_call import ConferenceCall

class EndConferenceEvent:
    def __init__(self, conf_call: ConferenceCall):
        self.conf_call = conf_call

    async def execute_event(self):
        await self.conf_call.communication_api.end_conf()
        self.conf_call.state.is_running = False
        self.conf_call.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.CONFERENCE_END, 
                                                    metadata={}, 
                                                    # TODO: OWNER OF THIS CAN BE SYSTEM or TEACHER
                                                    owner=self.conf_call.state.teacher_phone_number
                                                 )
                                    )
        # self.event_queue_processing_task.cancel() # Not ending processing tasks because call disconnect status events will be received from vonage
        await self.conf_call.websocket_service.close_websocket()     
        # Log the action in the action history
        self.conf_call.state.action_history.append(
            ActionHistory(
                timestamp=datetime.now().isoformat(),
                action_type=ActionType.CONFERENCE_END,
                metadata={},
                owner=self.conf_call.state.teacher_phone_number
            )
        )
        await self.conf_call.update_state()
