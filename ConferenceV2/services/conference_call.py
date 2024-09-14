# services/conference_call.py

from typing import List, Dict, Any
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
        communication_api: CommunicationAPI,
        storage_manager: StorageManager,
        connection_manager: SmartphoneConnectionManager,
    ):
        self.communication_api = communication_api
        self.storage_manager = storage_manager
        self.connection_manager = connection_manager
        self.conference_id = None
        self.call_status = CallStatus.RINGING
        self.teacher_phone_number = None
        self.participants: Dict[str, Participant] = {}
        self.audio_content_state = AudioContentState()
        self.action_history: List[ActionHistory] = []

    async def start_conference(self, teacher_phone: str, student_phones: List[str]):
        # Create teacher participant
        teacher = Participant(
            name="Teacher",
            phone_number=teacher_phone,
            role=Role.TEACHER,
            call_status=CallStatus.CONNECTED,
        )
        self.participants[teacher_phone] = teacher
        self.teacher_phone_number = teacher_phone

        # Create student participants
        for phone in student_phones:
            student = Participant(
                name="Student",
                phone_number=phone,
                role=Role.STUDENT,
                call_status=CallStatus.CONNECTING,
            )
            self.participants[phone] = student

        # Start the call via communication API
        self.conference_id = await self.communication_api.start_call(
            teacher_phone, student_phones
        )
        self.call_status = CallStatus.CONNECTED
        self.action_history.append(ActionHistory(
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
    
    async def process_webhook_event(self, payload: dict):
        await self.communication_api.process_webhook_event(payload)

    async def process_webhook_conversation_event(self, payload: dict):
        await self.communication_api.process_webhook_conversation_event(payload)

    async def process_webhook_input_event(self, payload: dict):
        await self.communication_api.process_webhook_input_event(payload)

    async def add_participant(self, phone_number: str):
        participant = Participant(
            name="Student",
            phone_number=phone_number,
            role=Role.STUDENT,
            call_status=CallStatus.CONNECTING,
        )
        self.participants[phone_number] = participant
        await self.communication_api.add_participant(self.conference_id, phone_number)

        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_ADD_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def remove_participant(self, phone_number: str):
        if phone_number in self.participants:
            await self.communication_api.remove_participant(self.conference_id, phone_number)
            del self.participants[phone_number]
            self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_REMOVE_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
            await self.update_state()

    async def mute_participant(self, phone_number: str, record_history: True):
        if phone_number in self.participants:
            await self.communication_api.mute_participant(self.conference_id, phone_number)
            self.participants[phone_number].is_muted = True
            if record_history: 
                self.action_history.append(ActionHistory(
                                                        timestamp= datetime.now(), 
                                                        action_type=ActionType.TEACHER_MUTE_STUDENT, 
                                                        metadata={
                                                            "phone_number": phone_number
                                                        }, 
                                                        owner=self.teacher_phone_number
                                                    )
                                        )
            await self.update_state()

    async def unmute_participant(self, phone_number: str, record_history: True):
        if phone_number in self.participants:
            await self.communication_api.unmute_participant(self.conference_id, phone_number)
            self.participants[phone_number].is_muted = False
            if record_history:
                self.action_history.append(ActionHistory(
                                                        timestamp= datetime.now(), 
                                                        action_type=ActionType.TEACHER_UNMUTE_STUDENT, 
                                                        metadata={
                                                            "phone_number": phone_number
                                                        }, 
                                                        owner=self.teacher_phone_number
                                                    )
                                        )
            await self.update_state()

    async def mute_all(self):
        tasks = []
        for participant in self.participants.values():
            if participant.role == Role.STUDENT and not participant.is_muted:
                tasks.append(self.mute_participant(participant.phone_number, record_history=False))
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_MUTE_ALL, 
                                                    metadata={}, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await asyncio.gather(*tasks)

    async def unmute_all(self):
        tasks = []
        for participant in self.participants.values():
            if participant.role == Role.STUDENT and participant.is_muted:
                tasks.append(self.unmute_participant(participant.phone_number, record_history=False))
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_UNMUTE_ALL, 
                                                    metadata={}, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await asyncio.gather(*tasks)

    async def play_content(self, url: str):
        self.audio_content_state.current_url = url
        self.audio_content_state.status = ContentStatus.PLAYING
        await self.communication_api.play_audio(self.conference_id, url)
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_PLAY_CONTENT, 
                                                    metadata={
                                                        "url": url
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def pause_content(self):
        self.audio_content_state.status = ContentStatus.PAUSED
        self.audio_content_state.paused_at = datetime.utcnow()
        await self.communication_api.pause_audio(self.conference_id)
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_PAUSE_CONTENT, 
                                                    metadata={
                                                        "url": self.audio_content_state.current_url
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def end_conference(self):
        await self.communication_api.end_call(self.conference_id)
        self.call_status = CallStatus.DISCONNECTED
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.CONFERENCE_END, 
                                                    metadata={}, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await self.update_state()
    
    async def update_participant_status(self, phone_number: str, call_status: str):
        if phone_number in self.participants:
            self.participants[phone_number].call_status = call_status
            self.action_history.append(ActionHistory(
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

    async def update_audio_playback_status(self, status: str):
        self.audio_content_state.status = status
        self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_AUDIO_PLAYBACK_STATUS_CHANGE, 
                                                    metadata={
                                                        "status": status
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
        await self.update_state()

    async def mute_unmute_event(self, phone_number: str, is_muted: bool):
        if phone_number in self.participants:
            self.participants[phone_number].is_muted = is_muted
            self.action_history.append(ActionHistory(
                                                    timestamp= datetime.now(), 
                                                    action_type=ActionType.TEACHER_MUTE_UNMUTE_STUDENT, 
                                                    metadata={
                                                        "phone_number": phone_number,
                                                        "is_muted": is_muted
                                                    }, 
                                                    owner=self.teacher_phone_number
                                                 )
                                    )
            await self.update_state()

    async def update_state(self):
        # Save state to storage
        await self.storage_manager.save_state(self.conference_id, self.toJson())
        # Notify clients
        # TODO: Finish notifying smartphone app
        # await self.connection_manager.send_message_to_clients(state)

    async def handle_student_raised_hand(self, phone_number: str):
        if phone_number in self.participants and self.participants[phone_number].role == Role.STUDENT:
            self.participants[phone_number].is_raised = not self.participants[phone_number].is_raised
            self.action_history.append(ActionHistory(
                                                timestamp= datetime.now(), 
                                                action_type=ActionType.STUDENT_RAISE_HAND_STATE_CHANGE, 
                                                metadata={
                                                    "phone_number": phone_number,
                                                    "raised_hand": self.participants[phone_number].is_raised
                                                }, 
                                                owner=phone_number
                                                )
                                )
            await self.update_state()    

    def toJson(self):
        return {
            'participants': [p.dict() for p in self.participants.values()],
            'audio_content_state': self.audio_content_state.dict(),
            'action_history': [a.dict() for a in self.action_history],
            'call_status': self.call_status,
            'conference_id': self.conference_id,
            'teacher_phone_number': self.teacher_phone_number
        }
