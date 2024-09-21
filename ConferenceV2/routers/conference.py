# routers/conference.py

import uuid
from fastapi import APIRouter, Depends, HTTPException, WebSocket
from typing import List
from services import ConferenceCallManager, VonageAPI, CosmosDBStorage, InMemoryStorageManager
from services.communication_api_factory import CommunicationAPIType
from utils.smartphone_connection_manager import WebSocketSmartphoneConnectionManager
from schemas.conference_schemas import EndConferenceRequest, StartConferenceRequest
from config import get_settings

router = APIRouter()
settings = get_settings()

# Initialize services
# storage_manager = CosmosDBStorage(
#     endpoint=settings.COSMOS_ENDPOINT,
#     key=settings.COSMOS_KEY,
#     database_name=settings.COSMOS_DATABASE,
#     container_name=settings.COSMOS_CONTAINER,
# )
storage_manager = InMemoryStorageManager()

# Factory function to create a new connection manager for each conference
def connection_manager_factory():
    return WebSocketSmartphoneConnectionManager()

# Create an instance of ConferenceCallManager
conference_manager = ConferenceCallManager(
    communication_api_type=CommunicationAPIType.VONAGE,
    storage_manager=storage_manager,
)

@router.post("/start")
async def start_conference(request: StartConferenceRequest):
    # Create and start the conference
    conference_call = await conference_manager.create_conference(request.teacher_phone, 
                                               request.student_phones, 
                                               smartphone_connection_manager=connection_manager_factory())
    return {
                "status": "Created", 
                "id": conference_call.conference_id
            }

@router.post("/end")
async def end_conference(request: EndConferenceRequest):
    # Create and start the conference
    conference_call = await conference_manager.end_conference(request.conference_id)
    return {
                "status": "Created", 
                "conf": conference_call
            }

@router.post("/add_participant")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.add_participant(phone_number)
    return {"message": "Participant added successfully"}


# Additional endpoints for other actions (mute, unmute, etc.)

# TODO: Define SmartphoneConnectionManager connect API
@router.websocket("/ws/{conference_id}")
async def websocket_endpoint(websocket: WebSocket, conference_id: str):
    pass
    # conference = conference_manager.get_conference(conference_id)
    # if not conference:
    #     await websocket.close(code=1000)
    #     return
    # await conference.connection_manager.handle_request(websocket)
