# services/storage_manager.py

from abc import ABC, abstractmethod


class StorageManager(ABC):
    @abstractmethod
    async def save_state(self, conference_id: str, state: dict):
        pass

    @abstractmethod
    async def load_state(self, conference_id: str) -> dict:
        pass
