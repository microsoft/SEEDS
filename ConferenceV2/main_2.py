# main.py

import os
import asyncio
import time
from abc import ABC, abstractmethod
from typing import Any, List, Dict, Optional
from dotenv import load_dotenv

from fastapi import (
    FastAPI,
    APIRouter,
    WebSocket,
    WebSocketDisconnect,
    Request,
    Depends,
    HTTPException,
)
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

load_dotenv()

# Abstract Interface
class ClientConnectionManager(ABC):
    @abstractmethod
    async def connect(self, client: Any):
        """Establish a connection with the client."""
        pass

    @abstractmethod
    async def disconnect(self, client: Any):
        """Handle client disconnection."""
        pass

    @abstractmethod
    async def send_message(self, message: str):
        """Send a message to the client."""
        pass

    @abstractmethod
    async def wait_for_reconnect(self, client: Any) -> bool:
        """Wait for the client to reconnect before marking them as disconnected."""
        pass

    @abstractmethod
    async def handle_request(self, client: Any):
        """Handle incoming requests from the client."""
        pass

# WebSocket Manager
class WebSocketConnectionManager(ClientConnectionManager):
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        print(f"WebSocket client {websocket.client} connected.")

    async def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
            print(f"WebSocket client {websocket.client} disconnected.")

    async def send_message(self, message: str):
        for connection in self.active_connections:
            await connection.send_text(message)

    async def wait_for_reconnect(self, websocket: WebSocket) -> bool:
        await asyncio.sleep(10)  # Wait for 10 seconds
        if websocket not in self.active_connections:
            print(f"WebSocket client {websocket.client} did not reconnect within 10 seconds.")
            return False
        return True

    async def handle_request(self, websocket: WebSocket):
        await self.connect(websocket)
        try:
            while True:
                data = await websocket.receive_text()
                await self.send_message(f"Message received: {data}")
        except WebSocketDisconnect:
            disconnected = await self.wait_for_reconnect(websocket)
            if not disconnected:
                await self.disconnect(websocket)

# SSE Manager
class SSEConnectionManager(ClientConnectionManager):
    def __init__(self):
        self.active_connections: List[Request] = []

    async def connect(self, request: Request):
        self.active_connections.append(request)
        print(f"SSE client {request.client.host} connected.")

    async def disconnect(self, request: Request):
        if request in self.active_connections:
            self.active_connections.remove(request)
            print(f"SSE client {request.client.host} disconnected.")

    async def send_message(self, message: str):
        # Implement server-initiated SSE messages if necessary
        pass  # Placeholder

    async def wait_for_reconnect(self, request: Request) -> bool:
        await asyncio.sleep(10)  # Wait for 10 seconds
        if request not in self.active_connections:
            print(f"SSE client {request.client.host} did not reconnect within 10 seconds.")
            return False
        return True

    async def handle_request(self, request: Request):
        await self.connect(request)

        async def event_generator():
            try:
                while True:
                    if await request.is_disconnected():
                        disconnected = await self.wait_for_reconnect(request)
                        if not disconnected:
                            await self.disconnect(request)
                            break
                    yield f"data: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n"
                    await asyncio.sleep(1)
            except asyncio.CancelledError:
                await self.disconnect(request)

        return StreamingResponse(event_generator(), media_type="text/event-stream")

# Long Polling Manager
class LongPollingConnectionManager(ClientConnectionManager):
    def __init__(self):
        self.active_connections: Dict[str, float] = {}

    async def connect(self, client_id: str):
        self.active_connections[client_id] = time.time()
        print(f"Long-polling client {client_id} connected.")

    async def disconnect(self, client_id: str):
        if client_id in self.active_connections:
            del self.active_connections[client_id]
            print(f"Long-polling client {client_id} disconnected.")

    async def send_message(self, message: str):
        # Implement message sending to long-polling clients if necessary
        print(f"Sending message to long-polling clients: {message}")
        pass  # Placeholder

    async def wait_for_reconnect(self, client_id: str) -> bool:
        await asyncio.sleep(10)  # Wait for 10 seconds
        if client_id not in self.active_connections:
            print(f"Long-polling client {client_id} did not reconnect within 10 seconds.")
            return False
        return True

    async def handle_request(self, client_id: str, polling_interval: int = 15):
        await self.connect(client_id)
        await asyncio.sleep(polling_interval)  # Simulate holding the connection

        if await self.wait_for_reconnect(client_id):
            return {"message": "Response after polling interval."}
        await self.disconnect(client_id)
        return {"message": "Client disconnected due to no reconnection."}

# Azure Web PubSub Manager
# Uncomment and install the Azure SDK if you plan to use Azure Web PubSub
# from azure.messaging.webpubsubservice import WebPubSubServiceClient

class AzureWebPubSubConnectionManager(ClientConnectionManager):
    def __init__(self, connection_string: str, hub_name: str):
        # Uncomment the following line if using Azure Web PubSub
        # self.service_client = WebPubSubServiceClient.from_connection_string(connection_string, hub_name)
        pass

    async def connect(self, client: Any):
        # Azure Web PubSub handles connections externally
        print("Azure Web PubSub client connected.")

    async def disconnect(self, client: Any):
        # Azure Web PubSub handles disconnections externally
        print("Azure Web PubSub client disconnected.")

    async def send_message(self, message: str):
        # Uncomment the following line if using Azure Web PubSub
        # await self.service_client.send_to_all(message)
        pass

    async def wait_for_reconnect(self, client: Any) -> bool:
        await asyncio.sleep(10)  # Wait for 10 seconds
        # Azure Web PubSub handles reconnections externally
        print("Azure Web PubSub client did not reconnect within 10 seconds.")
        return False

    async def handle_request(self, client: Any):
        # Azure Web PubSub manages connections and messaging
        pass  # Placeholder

# Routers

# WebSocket Router
websocket_router = APIRouter()

from fastapi.responses import HTMLResponse

@websocket_router.get("/ws-documentation")
async def custom_documentation():
    return HTMLResponse("""
    <h1>WebSocket Endpoint Documentation</h1>
    <h2>/connect</h2>
    <p>This endpoint establishes a WebSocket connection for real-time communication.</p>
    <p>The endpoint to be called is "/connect" </p>
    <h3>How to Connect</h3>
    <pre>
    const socket = new WebSocket("ws://yourserver.com/connect");
    </pre>
    <h3>Example Usage</h3>
    <!-- Include your example code here -->
    """)


@websocket_router.websocket("/connect")
async def websocket_endpoint(
    websocket: WebSocket
):
    manager = WebSocketConnectionManager()
    return await manager.handle_request(websocket)

# SSE Router
sse_router = APIRouter()

class SSERequest(BaseModel):
    client_id: str

@sse_router.get("/connect")
async def sse_endpoint(
    request: SSERequest,
):
    manager = SSEConnectionManager()
    return await manager.handle_request(request)

# Long Polling Router
long_polling_router = APIRouter()

class LongPollingRequest(BaseModel):
    client_id: str
    polling_interval: Optional[int] = 15  # Default is 15 seconds

@long_polling_router.post("/connect")
async def long_polling_endpoint(
    request: LongPollingRequest
):
    manager = LongPollingConnectionManager()
    response = await manager.handle_request(request.client_id, request.polling_interval)
    return response

# Azure Web PubSub Router
azure_webpubsub_router = APIRouter()

def get_azure_webpubsub_manager() -> ClientConnectionManager:
    connection_string = os.getenv("AZURE_WEBPUBSUB_CONNECTION_STRING")
    hub_name = os.getenv("AZURE_WEBPUBSUB_HUB_NAME")
    if not connection_string or not hub_name:
        raise HTTPException(status_code=500, detail="Azure Web PubSub configuration missing.")
    return AzureWebPubSubConnectionManager(connection_string, hub_name)

@azure_webpubsub_router.post("/connect")
async def azure_webpubsub_endpoint():
    # Azure Web PubSub manages connections externally.
    raise HTTPException(status_code=501, detail="Azure Web PubSub connections are managed externally.")

# Main Application
app = FastAPI(title="Connection Manager API")

# Get the connection type from the environment variable
connection_type = os.getenv("CONNECTION_TYPE").lower()

print(connection_type)

if connection_type == "websocket":
    app.include_router(websocket_router)
elif connection_type == "sse":
    app.include_router(sse_router)
elif connection_type == "long-polling":
    app.include_router(long_polling_router)
elif connection_type == "azure-webpubsub":
    app.include_router(azure_webpubsub_router)
else:
    raise HTTPException(status_code=500, detail="Invalid CONNECTION_TYPE configured on the server.")

@app.get("/")
async def root():
    return {"message": "Connection Manager API. Use /docs for API documentation."}
