from typing import Any, Dict
from models.participant import Participant
from fastapi.responses import StreamingResponse
import asyncio

from services.smartphone_connection_manager.base_smartphone_connection_manager import SmartphoneConnectionManager


class SSEConnectionManager(SmartphoneConnectionManager):
    def __init__(self):
        self.active_connections: Dict[str, asyncio.Queue] = {}  # Holds client phone_number to message queue

    async def connect(self, client: Participant):
        """Create a StreamingResponse to handle SSE connection."""
        if client.phone_number not in self.active_connections:
            self.active_connections[client.phone_number] = asyncio.Queue()
            print(f"Client {client.phone_number} connected")

        async def event_stream():
            """Generator to send messages to the client."""
            while True:
                # Fetch messages from the queue and yield them as SSE format
                message = await self.active_connections[client.phone_number].get()
                yield f"data: {message}\n\n"

        return StreamingResponse(event_stream(), media_type="text/event-stream")

    async def disconnect(self, client: Participant):
        """Remove client from active connections."""
        if client.phone_number in self.active_connections:
            del self.active_connections[client.phone_number]
            print(f"Client {client.phone_number} disconnected")

    async def send_message_to_client(self, client: Participant, message: Any):
        """Send a message to the client via the client's message queue."""
        if client.phone_number in self.active_connections:
            await self.active_connections[client.phone_number].put(message)  # Queue message for client
