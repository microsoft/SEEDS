# services/conference_call_manager.py

from typing import List, Dict, Any
from models.participant import Participant, Role, CallStatus
from models.audio_content_state import AudioContentState, ContentStatus
from models.action_history import ActionHistory
from services.communication_api import CommunicationAPI
from services.storage_manager import StorageManager
from utils.smartphone_connection_manager import SmartphoneConnectionManager
from datetime import datetime
import asyncio


class ConferenceCallManager:
    def __init__(
        self,
        communication_api: CommunicationAPI,
        storage_manager: StorageManager,
        connection_manager: SmartphoneConnectionManager,
    ):
        self.conference_id = None
        self.communication_api = communication_api
        self.storage_manager = storage_manager
        self.connection_manager = connection_manager
        self.participants: Dict[str, Participant] = {}
        self.audio_content_state = AudioContentState()
        self.action_history: List[ActionHistory] = []

    async def start_conference(self, teacher_phone: str, student_phones: List[str]):
        # Create teacher participant
        teacher = Participant(
            name="Teacher",
            phone_number=teacher_phone,
            role=Role.TEACHER,
            # TODO: Map to original Vonage call statuses
            call_status=CallStatus.CONNECTED, 
        )
        self.participants[teacher_phone] = teacher

        # Create student participants
        for phone in student_phones:
            student = Participant(
                # TODO: Check if this field is neccessary
                name="Student",
                phone_number=phone,
                role=Role.STUDENT,
                call_status=CallStatus.CONNECTING,
            )
            self.participants[phone] = student

        # Start the call via communication API
        # TODO: Start call should return conf ID
        self.conference_id = await self.communication_api.start_call(teacher_phone, student_phones)

        # Update state and save
        await self.update_state()

    async def add_participant(self, phone_number: str):
        participant = Participant(
            name="Student",
            phone_number=phone_number,
            role=Role.STUDENT,
            call_status=CallStatus.CONNECTING,
        )
        self.participants[phone_number] = participant
        await self.communication_api.add_participant(self.conference_id, phone_number)
        await self.update_state()

    async def remove_participant(self, phone_number: str):
        if phone_number in self.participants:
            await self.communication_api.remove_participant(self.conference_id, phone_number)
            del self.participants[phone_number]
            await self.update_state()

    async def mute_participant(self, phone_number: str):
        if phone_number in self.participants:
            await self.communication_api.mute_participant(self.conference_id, phone_number)
            self.participants[phone_number].is_muted = True
            await self.update_state()

    async def unmute_participant(self, phone_number: str):
        if phone_number in self.participants:
            await self.communication_api.unmute_participant(self.conference_id, phone_number)
            self.participants[phone_number].is_muted = False
            await self.update_state()

    async def mute_all(self):
        tasks = []
        for participant in self.participants.values():
            if participant.role == Role.STUDENT and not participant.is_muted:
                tasks.append(self.mute_participant(participant.phone_number))
        await asyncio.gather(*tasks)

    async def unmute_all(self):
        tasks = []
        for participant in self.participants.values():
            if participant.role == Role.STUDENT and participant.is_muted:
                tasks.append(self.unmute_participant(participant.phone_number))
        await asyncio.gather(*tasks)

    async def play_content(self, url: str):
        self.audio_content_state.current_url = url
        self.audio_content_state.status = ContentStatus.PLAYING
        await self.communication_api.play_audio(self.conference_id, url)
        await self.update_state()

    async def pause_content(self):
        self.audio_content_state.status = ContentStatus.PAUSED
        self.audio_content_state.paused_at = datetime.utcnow()
        await self.communication_api.pause_audio(self.conference_id)
        await self.update_state()

    async def end_conference(self):
        await self.communication_api.end_call(self.conference_id)
        self.participants.clear()
        await self.update_state()
    
    async def update_participant_status(self, phone_number: str, call_status: str):
        if phone_number in self.participants:
            self.participants[phone_number].call_status = call_status
            await self.update_state()

    async def handle_dtmf_input(self, phone_number: str, dtmf_digit: str):
        if dtmf_digit == '1':  # Assuming '1' is the key to raise hand
            if phone_number in self.participants:
                self.participants[phone_number].is_raised = True
                await self.update_state()
                # Optionally, log this action in action_history

    async def update_audio_playback_status(self, status: str):
        self.content_manager.status = status
        await self.update_state()

    async def mute_unmute_event(self, phone_number: str, is_muted: bool):
        if phone_number in self.participants:
            self.participants[phone_number].is_muted = is_muted
            await self.update_state()

    async def update_state(self):
        # Save state to storage
        state = {
            'participants': [p.dict() for p in self.participants.values()],
            'audio_content_state': self.audio_content_state.dict(),
            'action_history': [a.dict() for a in self.action_history],
            'conference_id': self.conference_id,
        }
        await self.storage_manager.save_state(self.conference_id, state)
        # Notify clients
        await self.connection_manager.send_message_to_clients(state)

    async def handle_student_raised_hand(self, phone_number: str):
        if phone_number in self.participants:
            self.participants[phone_number].is_raised = True
            await self.update_state()
