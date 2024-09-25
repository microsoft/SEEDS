# routers/conference.py

import asyncio
import uuid
from fastapi import APIRouter, Depends, HTTPException, Request, WebSocket, WebSocketDisconnect
from typing import List
from routers.conference import conference_manager

router = APIRouter()
# settings = get_settings()

# Initialize services
# storage_manager = CosmosDBStorage(
#     endpoint=settings.COSMOS_ENDPOINT,
#     key=settings.COSMOS_KEY,
#     database_name=settings.COSMOS_DATABASE,
#     container_name=settings.COSMOS_CONTAINER,
# )

@router.websocket("/{conference_id}")
async def websocket_endpoint(websocket: WebSocket, conference_id: str):
    conf = conference_manager.get_conference(conference_id)
    if conf:
        print("WEBSOCKET ACCEPTED FOR CONF: ", conference_id)
        await websocket.accept()
        conf.set_websocket(websocket)
        
        try:
            while True:
                # Keep the connection alive or handle incoming messages here if needed
                await asyncio.sleep(10)  # Simulate some activity to keep connection open
        except WebSocketDisconnect:
            print(f"Websocket Client disconnected from {conference_id}")
            conf.set_websocket(None)  # Clear the WebSocket on disconnection
        except Exception as e:
            print(f"An error occurred: {e}")
            conf.set_websocket(None)  # Ensure cleanup in case of error
    
