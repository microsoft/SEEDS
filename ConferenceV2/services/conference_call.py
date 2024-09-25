# services/conference_call.py

from typing import List, Dict, Any

from fastapi import WebSocket
from models.conference_call_state import ConferenceCallState
from models.participant import Participant, Role, CallStatus
from models.audio_content_state import AudioContentState, ContentStatus
from models.action_history import ActionHistory, ActionType
from services.communication_api import CommunicationAPI
from services.storage_manager import StorageManager 
from services.smartphone_connection_manager import SmartphoneConnectionManager
from datetime import datetime
import asyncio

from services.websocket_service import WebSocketService


class ConferenceCall:
    def __init__(
        self,
        conf_id: str,
        communication_api: CommunicationAPI,
        storage_manager: StorageManager,
        connection_manager: SmartphoneConnectionManager,
        websocket_service: WebSocketService,
    ):
        self.conf_id = conf_id
        self.communication_api = communication_api
        self.storage_manager = storage_manager
        self.connection_manager = connection_manager
        self.websocket_service = websocket_service
        self.state = ConferenceCallState()
    
    def set_websocket(self, websocket: WebSocket):
        self.websocket_service.set_websocket(websocket)
    
    def set_participant_state(self, teacher_phone: str, student_phones: List[str]):
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

    async def start_conference(self):
        # Start the call via communication API
        await self.communication_api.start_conf(
            self.state.teacher_phone_number, 
            [student.phone_number for student in self.state.get_students()],
            websocket_ep=self.websocket_service.websocket_server_ep
        )
        # TODO: Set CONNECTED CALL STATUS WHEN ATLEAST ONE OF THE PARTICIPANTS HAVE PICKED UP
        self.state.call_status = CallStatus.RINGING
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
    
    async def add_participant(self, phone_number: str):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already connected
        participant = Participant(
            name="Student",
            phone_number=phone_number,
            role=Role.STUDENT,
            call_status=CallStatus.CONNECTING,
        )
        await self.communication_api.add_participant(phone_number)

        self.state.participants[phone_number] = participant
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_ADD_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def remove_participant(self, phone_number: str):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already removed
        if phone_number in self.state.participants:
            await self.communication_api.remove_participant(phone_number)
            del self.state.participants[phone_number]
            self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_REMOVE_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
            await self.update_state()

    async def mute_participant(self, phone_number: str, record_history: bool = True):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already muted
        if phone_number in self.state.participants:
            await self.communication_api.mute_participant(phone_number)
            self.state.participants[phone_number].is_muted = True
            if record_history: 
                self.state.action_history.append(ActionHistory(
                                                        timestamp= datetime.now().isoformat(), 
                                                        action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT, 
                                                        metadata={
                                                            "phone_number": phone_number,
                                                            "is_muted": True
                                                        }, 
                                                        owner=self.state.teacher_phone_number
                                                    )
                                        )
            await self.update_state()

    async def unmute_participant(self, phone_number: str, record_history: bool = True):
        # TODO: Speak out announcement messages in conversation through comm API, check if the participant is already unmuted
        if phone_number in self.state.participants:
            await self.communication_api.unmute_participant(phone_number)
            self.state.participants[phone_number].is_muted = False
            if record_history:
                self.state.action_history.append(ActionHistory(
                                                        timestamp= datetime.now().isoformat(), 
                                                        action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT, 
                                                        metadata={
                                                            "phone_number": phone_number,
                                                            "is_muted": False
                                                        }, 
                                                        owner=self.state.teacher_phone_number
                                                    )
                                        )
            await self.update_state()

    async def mute_all(self):
        # TODO: Speak out announcement messages in conversation through comm API
        tasks = []
        for participant in self.state.participants.values():
            if participant.role == Role.STUDENT and not participant.is_muted:
                tasks.append(self.mute_participant(participant.phone_number, record_history=False))
        await asyncio.gather(*tasks)
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_MUTE_ALL, 
                                                    metadata={}, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def unmute_all(self):
        # TODO: Speak out announcement messages in conversation through comm API
        tasks = []
        for participant in self.state.participants.values():
            if participant.role == Role.STUDENT and participant.is_muted:
                tasks.append(self.unmute_participant(participant.phone_number, record_history=False))
        await asyncio.gather(*tasks)
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_UNMUTE_ALL, 
                                                    metadata={}, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def play_content(self, url: str = "/home/kavyansh/SEEDS/ConferenceV2/audio_test.wav"):
        self.state.audio_content_state.current_url = url
        self.state.audio_content_state.status = ContentStatus.PLAYING
        # await self.communication_api.play_audio(url)
        await self.websocket_service.play(url)
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE, 
                                                    metadata={
                                                        "playback_status": self.state.audio_content_state.model_dump()
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def pause_content(self):
        self.state.audio_content_state.status = ContentStatus.PAUSED
        self.state.audio_content_state.paused_at = datetime.utcnow()
        await self.communication_api.pause_audio()
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE, 
                                                    metadata={
                                                        "playback_status": self.state.audio_content_state.model_dump()
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()
    
    async def update_participant_call_status(self, phone_number: str, call_status: str):
        # TODO: Speak out announcement messages in conversation through comm API
        if phone_number in self.state.participants:
            self.state.participants[phone_number].call_status = call_status
            self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.CONFERENCE_CALLSTATUS_CHANGE, 
                                                    metadata={
                                                        "phone_number": phone_number,
                                                        "call_status": call_status
                                                    }, 
                                                    owner=phone_number
                                                 )
                                    )
            await self.update_state()

    async def end_conference(self):
        await self.communication_api.end_conf()
        self.call_status = CallStatus.DISCONNECTED
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now().isoformat(), 
                                                    action_type=ActionType.CONFERENCE_END, 
                                                    metadata={}, 
                                                    # TODO: OWNER OF THIS CAN BE SYSTEM or TEACHER
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()
    
    async def update_state(self):
        # Save state to storage
        await self.storage_manager.save_state(self.conf_id, self.state.model_dump(by_alias=True))
        # Notify clients
        # # TODO: Finish notifying smartphone app
        # await self.connection_manager.send_message_to_client(client=self.state.get_teacher(),
        #                                                      message=self.state.model_dump())

    async def handle_student_raised_hand(self, phone_number: str):
        if phone_number in self.state.participants and self.state.participants[phone_number].role == Role.STUDENT:
            self.state.participants[phone_number].is_raised = not self.state.participants[phone_number].is_raised
            self.state.action_history.append(ActionHistory(
                                                timestamp= datetime.now().isoformat(), 
                                                action_type=ActionType.STUDENT_RAISE_HAND_STATE_CHANGE, 
                                                metadata={
                                                    "phone_number": phone_number,
                                                    "raised_hand": self.state.participants[phone_number].is_raised
                                                }, 
                                                owner=phone_number
                                                )
                                )
            await self.update_state()
    
    # INPUTS FROM WEBHOOK
    async def process_webhook_event(self, payload: dict):
        event = self.communication_api.parse_event_webhook(payload)
        # TODO: PROCESS EVENT to UPDATE STATES

    async def process_webhook_conversation_event(self, payload: dict):
        event = self.communication_api.parse_conversation_event_webhook(payload)
        # TODO: PROCESS EVENT to UPDATE STATES

    async def process_webhook_input_event(self, payload: dict):
        event = self.communication_api.parse_input_webhook(payload)
        # TODO: PROCESS EVENT to UPDATE STATES
