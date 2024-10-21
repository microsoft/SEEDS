import json
from typing import Any, Dict
from models.participant import Participant
from fastapi.responses import StreamingResponse
import asyncio
from conf_logger import logger_instance
from services.smartphone_connection_manager.base_smartphone_connection_manager import SmartphoneConnectionManager


class SSEConnectionManager(SmartphoneConnectionManager):
    def __init__(self):
        self.active_connections: Dict[str, Dict] = {}  # Holds client phone_number to message queue and disconnect flag

    async def connect(self, client: Participant):
        """Create a StreamingResponse to handle SSE connection."""
        if client.phone_number not in self.active_connections:
            self.active_connections[client.phone_number] = {
                "queue": asyncio.Queue(),
                "disconnected": False
            }
            logger_instance.info(f"SSE Client {client.phone_number} connected")

        async def event_stream():
            """Generator to send messages to the client."""
            while True:
                # Break the loop if the client is marked as disconnected
                if self.active_connections[client.phone_number]["disconnected"]:
                    logger_instance.info(f"Stopping event stream for {client.phone_number}")
                    break

                try:
                    # Fetch messages from the queue and yield them as SSE format
                    message = await self.active_connections[client.phone_number]["queue"].get()
                    yield f"data: {json.dumps(message)}\n\n"
                except asyncio.CancelledError:
                    logger_instance.info(f"Event stream task canceled for {client.phone_number}")
                    break

        return StreamingResponse(event_stream(), media_type="text/event-stream")

    async def disconnect(self, client: Participant):
        """Remove client from active connections."""
        if client.phone_number in self.active_connections:
            # Set the disconnected flag to True to break the event_stream loop
            self.active_connections[client.phone_number]["disconnected"] = True
            # Remove the client from the active_connections after a delay
            del self.active_connections[client.phone_number]
            logger_instance.info(f"SSE Client {client.phone_number} disconnected")

    async def send_message_to_client(self, client: Participant, message: dict):
        """Send a message to the client via the client's message queue."""
        if client.phone_number in self.active_connections:
            await self.active_connections[client.phone_number]["queue"].put(message)  # Queue message for client
