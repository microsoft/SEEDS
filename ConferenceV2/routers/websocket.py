# routers/conference.py

import uuid
from fastapi import APIRouter, Depends, HTTPException, Request, WebSocket
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
        await websocket.accept()
        conf.set_websocket(websocket)
    
