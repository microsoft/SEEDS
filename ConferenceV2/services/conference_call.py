# services/conference_call.py

from typing import List, Dict, Any
from models.conference_call_state import ConferenceCallState
from models.participant import Participant, Role, CallStatus
from models.audio_content_state import AudioContentState, ContentStatus
from models.action_history import ActionHistory, ActionType
from services.communication_api import CommunicationAPI
from services.storage_manager import StorageManager
from utils.smartphone_connection_manager import SmartphoneConnectionManager
from datetime import datetime
import asyncio


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
        self.state = ConferenceCallState()

    async def start_conference(self, teacher_phone: str, student_phones: List[str]):
        # Create teacher participant
        teacher = Participant(
            name="Teacher",
            phone_number=teacher_phone,
            role=Role.TEACHER,
            call_status=CallStatus.CONNECTED,
        )
        self.state.participants[teacher_phone] = teacher
        self.state.teacher_phone_number = teacher_phone

        # Create student participants
        for phone in student_phones:
            student = Participant(
                name="Student",
                phone_number=phone,
                role=Role.STUDENT,
                call_status=CallStatus.CONNECTING,
            )
            self.state.participants[phone] = student

        # Start the call via communication API
        await self.communication_api.start_conf(
            teacher_phone, student_phones, self.conf_id
        )
        # TODO: Set CONNECTED CALL STATUS WHEN ATLEAST ONE OF THE PARTICIPANTS HAVE PICKED UP
        self.state.call_status = CallStatus.RINGING
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.CONFERENCE_START, 
                                                    metadata={
                                                        "teacher_phone": teacher_phone,
                                                        "student_phones": student_phones
                                                    }, 
                                                    owner=teacher_phone
                                                 )
                                    )
        # Update state and save
        await self.update_state()
    
    async def add_participant(self, phone_number: str):
        # TODO: Speak out announcement messages in conversation through comm API
        participant = Participant(
            name="Student",
            phone_number=phone_number,
            role=Role.STUDENT,
            call_status=CallStatus.CONNECTING,
        )
        self.state.participants[phone_number] = participant
        await self.communication_api.add_participant(self.state.conference_id, phone_number)

        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_ADD_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def remove_participant(self, phone_number: str):
        # TODO: Speak out announcement messages in conversation through comm API
        if phone_number in self.state.participants:
            await self.communication_api.remove_participant(self.state.conference_id, phone_number)
            del self.state.participants[phone_number]
            self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_REMOVE_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
            await self.update_state()

    async def mute_participant(self, phone_number: str, record_history: True):
        # TODO: Speak out announcement messages in conversation through comm API
        if phone_number in self.state.participants:
            await self.communication_api.mute_participant(self.state.conference_id, phone_number)
            self.state.participants[phone_number].is_muted = True
            if record_history: 
                self.state.action_history.append(ActionHistory(
                                                        timestamp= datetime.now(), 
                                                        action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT, 
                                                        metadata={
                                                            "phone_number": phone_number,
                                                            "is_muted": True
                                                        }, 
                                                        owner=self.state.teacher_phone_number
                                                    )
                                        )
            await self.update_state()

    async def unmute_participant(self, phone_number: str, record_history: True):
        # TODO: Speak out announcement messages in conversation through comm API
        if phone_number in self.state.participants:
            await self.communication_api.unmute_participant(self.state.conference_id, phone_number)
            self.state.participants[phone_number].is_muted = False
            if record_history:
                self.state.action_history.append(ActionHistory(
                                                        timestamp= datetime.now(), 
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
                                                    timestamp= datetime.now(), 
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
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_UNMUTE_ALL, 
                                                    metadata={}, 
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def play_content(self, url: str):
        self.state.audio_content_state.current_url = url
        self.state.audio_content_state.status = ContentStatus.PLAYING
        await self.communication_api.play_audio(self.state.conference_id, url)
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
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
        await self.communication_api.pause_audio(self.state.conference_id)
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
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
                                                    timestamp= datetime.now(), 
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
        await self.communication_api.end_call(self.conference_id)
        self.call_status = CallStatus.DISCONNECTED
        self.state.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.CONFERENCE_END, 
                                                    metadata={}, 
                                                    # TODO: OWNER OF THIS CAN BE SYSTEM or TEACHER
                                                    owner=self.state.teacher_phone_number
                                                 )
                                    )
        await self.update_state()
    
    async def update_state(self):
        # Save state to storage
        await self.storage_manager.save_state(self.state.conference_id, self.state.model_dump(by_alias=True))
        # Notify clients
        # TODO: Finish notifying smartphone app
        # await self.connection_manager.send_message_to_clients(state)

    async def handle_student_raised_hand(self, phone_number: str):
        if phone_number in self.state.participants and self.state.participants[phone_number].role == Role.STUDENT:
            self.state.participants[phone_number].is_raised = not self.state.participants[phone_number].is_raised
            self.state.action_history.append(ActionHistory(
                                                timestamp= datetime.now(), 
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
