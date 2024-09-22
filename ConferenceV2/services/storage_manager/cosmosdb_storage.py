# services/cosmosdb_storage.py

from services.storage_manager.base_storage_manager import StorageManager
from azure.cosmos.aio import CosmosClient
from azure.cosmos.exceptions import CosmosResourceNotFoundError
import asyncio


class CosmosDBStorage(StorageManager):
    def __init__(self, endpoint: str, key: str, database_name: str, container_name: str):
        self.client = CosmosClient(endpoint, credential=key)
        self.database_name = database_name
        self.container_name = container_name
        self.database = None
        self.container = None

    async def init_resources(self):
        if self.database is None or self.container is None:
            self.database = await self.client.create_database_if_not_exists(self.database_name)
            self.container = await self.database.create_container_if_not_exists(
                id=self.container_name, partition_key="/id"
            )

    async def save_state(self, conference_id: str, state: dict):
        await self.init_resources()
        state['id'] = conference_id  # Cosmos DB requires an 'id' field
        await self.container.upsert_item(state)

    async def load_state(self, conference_id: str) -> dict:
        await self.init_resources()
        try:
            response = await self.container.read_item(conference_id, partition_key=conference_id)
            return response
        except CosmosResourceNotFoundError:
            return None
