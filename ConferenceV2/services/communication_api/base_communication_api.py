# services/communication_api.py

from abc import ABC, abstractmethod
from typing import List, Optional, Tuple
from models.webhook_event import WebHookEvent


class CommunicationAPI(ABC):
    # RETURN CONF ID
    @abstractmethod
    async def start_conf(self, teacher_phone: str, student_phones: List[str]) -> str:
        pass

    # ENDS A CONF
    @abstractmethod
    async def end_conf(self):
        pass

    @abstractmethod
    def connect_websocket(self):
        pass

    # ADD PARTICIPANT TO THE CONF
    @abstractmethod
    async def add_participant(self, phone_number: str):
        pass

    # REMOVE PARTICIPANT TO THE CONF
    @abstractmethod
    async def remove_participant(self, phone_number: str):
        pass

    # MUTE PARTICIPANT IN THE CONF
    @abstractmethod
    async def mute_participant(self, phone_number: str):
        pass

    # UNMUTE PARTICIPANT IN THE CONF
    @abstractmethod
    async def unmute_participant(self, phone_number: str):
        pass