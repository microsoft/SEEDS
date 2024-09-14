# routers/conference.py

from fastapi import APIRouter, Depends, WebSocket
from typing import List
from services import ConferenceCallManager, VonageAPI, CosmosDBStorage, InMemoryStorageManager
from utils.smartphone_connection_manager import WebSocketSmartphoneConnectionManager
from schemas.conference_schemas import StartConferenceRequest
from config import get_settings

router = APIRouter()
settings = get_settings()

# Initialize services
communication_api = VonageAPI(api_key=settings.VONAGE_API_KEY, api_secret=settings.VONAGE_API_SECRET)
# storage_manager = CosmosDBStorage(
#     endpoint=settings.COSMOS_ENDPOINT,
#     key=settings.COSMOS_KEY,
#     database_name=settings.COSMOS_DATABASE,
#     container_name=settings.COSMOS_CONTAINER,
# )
storage_manager = InMemoryStorageManager()

# TODO: Create for each conference call
# Create an instance of ConferenceCallManager
conference_manager = ConferenceCallManager(
    communication_api=communication_api,
    storage_manager=storage_manager,
    connection_manager=WebSocketSmartphoneConnectionManager(),
)


@router.post("/start_conf")
async def start_conference(request: StartConferenceRequest):
    await conference_manager.start_conference(request.teacher_phone, request.student_phones)
    return {"message": "Conference started successfully"}


# TODO: Create ConferenceCallManager from DB
@router.post("/add_participant")
async def add_participant(phone_number: str):
    await conference_manager.add_participant(phone_number)
    return {"message": "Participant added successfully"}


# Additional endpoints for other actions (mute, unmute, etc.)

# TODO: Define SmartphoneConnectionManager connect API
@router.websocket("/smartphoneconnect")
async def websocket_endpoint(websocket: WebSocket):
    # await connection_manager.handle_request(websocket)
    pass
