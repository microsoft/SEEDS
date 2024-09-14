# services/communication_api.py

from abc import ABC, abstractmethod
from typing import List


class CommunicationAPI(ABC):
    @abstractmethod
    async def start_call(self, teacher_phone: str, student_phones: List[str], conference_id: str):
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
