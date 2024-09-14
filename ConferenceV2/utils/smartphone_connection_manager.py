# utils/client_connection_manager.py

from abc import ABC, abstractmethod
from typing import Any, List
import asyncio

# TODO: Need a global connection manager that keeps adding new connections to live connections pool, 
# and sends messsages to particular connections, whenever its function is called

class SmartphoneConnectionManager(ABC):
    @abstractmethod
    async def connect(self, client: Any):
        pass

    @abstractmethod
    async def disconnect(self, client: Any):
        pass

    @abstractmethod
    async def send_message_to_clients(self, message: Any):
        pass

    @abstractmethod
    async def handle_request(self, client: Any):
        pass


class WebSocketSmartphoneConnectionManager(SmartphoneConnectionManager):
    def __init__(self):
        self.active_connections: List[Any] = []

    async def connect(self, websocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    async def disconnect(self, websocket):
        self.active_connections.remove(websocket)

    async def send_message_to_clients(self, message: Any):
        for connection in self.active_connections:
            await connection.send_json(message)

    async def handle_request(self, websocket):
        await self.connect(websocket)
        try:
            while True:
                data = await websocket.receive_text()
                # Handle incoming data if needed
        except Exception:
            await self.disconnect(websocket)
