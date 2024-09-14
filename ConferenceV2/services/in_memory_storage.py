# services/in_memory_storage.py

from services.storage_manager import StorageManager
from typing import Dict

class InMemoryStorageManager(StorageManager):
    def __init__(self):
        self.storage: Dict[str, dict] = {}

    async def save_state(self, conference_id: str, state: dict):
        self.storage[conference_id] = state

    async def load_state(self, conference_id: str) -> dict:
        return self.storage.get(conference_id, None)
