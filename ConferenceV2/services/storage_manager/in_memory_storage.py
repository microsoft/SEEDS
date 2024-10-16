# services/in_memory_storage.py

import json
from services.storage_manager.base_storage_manager import StorageManager
from typing import Dict
import os

class InMemoryStorageManager(StorageManager):
    def __init__(self):
        self.storage: Dict[str, dict] = {}

    async def save_state(self, conference_id: str, state: dict):
        self.storage[conference_id] = state
        # state_json_folder_path = f'{os.getcwd()}/states/{conference_id}'
        # fp = f"{state_json_folder_path}/states.json"
        # os.makedirs(state_json_folder_path, exist_ok=True)

        # states = []
        # if os.path.exists(fp):
        #     # Open and read the JSON file
        #     with open(fp, 'r') as json_file:
        #         states = json.load(json_file)
        # states.append(state)

        # with open(fp, 'w') as f:
        #     json.dump(states, f, indent=4)

    async def load_state(self, conference_id: str) -> dict:
        return self.storage.get(conference_id, None)
