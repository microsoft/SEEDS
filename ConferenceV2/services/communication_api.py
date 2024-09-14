# services/communication_api.py

from abc import ABC, abstractmethod
from typing import List, Optional

from models.webhook_event import WebHookEvent


class CommunicationAPI(ABC):
    @abstractmethod
    async def start_call(self, teacher_phone: str, student_phones: List[str]):
        pass

    @abstractmethod
    async def end_call(self, conference_id: str):
        pass

    @abstractmethod
    async def add_participant(self, conference_id: str, phone_number: str):
        pass

    @abstractmethod
    async def remove_participant(self, conference_id: str, phone_number: str):
        pass

    @abstractmethod
    async def mute_participant(self, conference_id: str, phone_number: str):
        pass

    @abstractmethod
    async def unmute_participant(self, conference_id: str, phone_number: str):
        pass

    @abstractmethod
    async def play_audio(self, conference_id: str, url: str):
        pass

    @abstractmethod
    async def pause_audio(self, conference_id: str):
        pass

    @abstractmethod
    def parse_event_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        pass

    @abstractmethod
    def parse_conversation_event_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        pass

    @abstractmethod
    def parse_input_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        pass