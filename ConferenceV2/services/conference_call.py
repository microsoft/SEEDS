# services/conference_call.py

from typing import List
from datetime import datetime
import asyncio

from fastapi import WebSocket
from models.conference_call_state import ConferenceCallState
from services.confevents.base_event import ConferenceEvent
from models.participant import Participant, Role, CallStatus
from models.action_history import ActionHistory, ActionType
from services.communication_api import CommunicationAPI
from services.storage_manager import StorageManager 
from services.smartphone_connection_manager import SmartphoneConnectionManager
from services.vanilla_websocket_service import VanillaWebSocketService


class ConferenceCall:
    def __init__(
        self,
        conf_id: str,
        communication_api: CommunicationAPI,
        storage_manager: StorageManager,
        connection_manager: SmartphoneConnectionManager,
    ):
        self.conf_id = conf_id
        self.communication_api = communication_api
        self.storage_manager = storage_manager
        self.connection_manager = connection_manager
        self.websocket_service = VanillaWebSocketService(
                on_disconnect_callback=self.__on_websocket_disconnect_callback,
            )
        self.state = ConferenceCallState()
        self.event_queue = asyncio.Queue()
        self.event_queue_processing_task: asyncio.Task = None
    
    async def queue_event(self, event: ConferenceEvent):
        await self.event_queue.put(event)
    
    def start_processing_conf_events_from_queue(self):
        if self.event_queue_processing_task != None:
            self.event_queue_processing_task.cancel()
        self.event_queue_processing_task = asyncio.create_task(self.__process_conf_events_queue())
    
    def set_participant_state(self, teacher_phone: str, student_phones: List[str]):
        self.state.participants = {}
        teacher = Participant(
            name="Teacher",
            phone_number=teacher_phone,
            role=Role.TEACHER,
            call_status=CallStatus.DISCONNECTED,
        )
        self.state.participants[teacher_phone] = teacher
        self.state.teacher_phone_number = teacher_phone

        # Create student participants
        for phone in student_phones:
            student = Participant(
                name="Student",
                phone_number=phone,
                role=Role.STUDENT,
                call_status=CallStatus.DISCONNECTED,
            )
            self.state.participants[phone] = student
    
    def set_websocket(self, websocket: WebSocket):
        self.websocket_service.set_websocket(websocket)

    async def start_conference(self):
        # Start the call via communication API
        await self.communication_api.start_conf(
            self.state.teacher_phone_number, 
            [student.phone_number for student in self.state.get_students()]
        )
        # TODO: Set CONNECTED CALL STATUS WHEN ATLEAST ONE OF THE PARTICIPANTS HAVE PICKED UP
        self.state.action_history.append(ActionHistory(
                                                    timestamp=datetime.now().isoformat(), 
                                                    action_type=ActionType.CONFERENCE_START, 
                                                    metadata={
                                                        "teacher_phone": self.state.teacher_phone_number,
                                                        "student_phones": [student.phone_number for student in self.state.get_students()]
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        # Update state and save
        await self.update_state()
    
    async def connect_smartphone(self):
        teacher = self.state.get_teacher()
        if teacher:
            return await self.connection_manager.connect(client=teacher)
        raise ValueError("No teacher participant in conf call " + self.conf_id)
    
    async def disconnect_smartphone(self):
        teacher = self.state.get_teacher()
        if teacher:
            return await self.connection_manager.disconnect(client=teacher)
        raise ValueError("No teacher participant in conf call " + self.conf_id)
      
    async def end_conference(self):
        await self.communication_api.end_conf()
        self.state.is_running = False
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.CONFERENCE_END, 
                                                    metadata={}, 
                                                    # TODO: OWNER OF THIS CAN BE SYSTEM or TEACHER
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()
        # self.event_queue_processing_task.cancel() # Not ending processing tasks because call disconnect status events will be received from vonage
        await self.websocket_service.close_websocket()
    
    async def update_state(self):
        # Save state to storage
        await self.storage_manager.save_state(self.conf_id, self.state.model_dump(by_alias=True))
        # Notify clients
        # # TODO: Finish notifying smartphone app
        await self.connection_manager.send_message_to_client(client=self.state.get_teacher(),
                                                             message=self.state.model_dump(by_alias=True))
    
    async def __on_websocket_disconnect_callback(self):
        await self.communication_api.connect_websocket()
    
    # Dequeue function: runs continuously to process tasks
    async def __process_conf_events_queue(self):
        while True:
            event: ConferenceEvent = await self.event_queue.get()
            await event.execute_event()
            await asyncio.sleep(0.2)
    

        