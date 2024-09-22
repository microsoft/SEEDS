# utils/client_connection_manager.py

from abc import ABC, abstractmethod
from typing import Any, List
import asyncio

from models.participant import Participant


class SmartphoneConnectionManager(ABC):
    # TODO: Callbacks to ConferenceCall instance is required to handle disconnections
    @abstractmethod
    async def connect(self, client: Participant):
        pass

    @abstractmethod
    async def disconnect(self, client: Participant):
        pass

    @abstractmethod
    async def send_message_to_client(self, client: Participant, message: Any):
        pass