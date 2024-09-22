# routers/conference.py

import uuid
from fastapi import APIRouter, Depends, HTTPException, Request, WebSocket
from typing import List
from services import ConferenceCallManager, VonageAPI, CosmosDBStorage, InMemoryStorageManager
from services.communication_api_factory import CommunicationAPIType
from services.smartphone_connection_manager_factory import SmartphoneConnectionManagerType
from utils.azure_service_bus_connection_manager import AzureServiceBusSmartphoneConnectionManager
from schemas.conference_schemas import EndConferenceRequest, StartConferenceRequest
from pydantic_settings import BaseSettings

router = APIRouter()
# settings = get_settings()

# Initialize services
# storage_manager = CosmosDBStorage(
#     endpoint=settings.COSMOS_ENDPOINT,
#     key=settings.COSMOS_KEY,
#     database_name=settings.COSMOS_DATABASE,
#     container_name=settings.COSMOS_CONTAINER,
# )

# Create an instance of ConferenceCallManager
conference_manager = ConferenceCallManager(
    communication_api_type=CommunicationAPIType.VONAGE,
    smartphone_connection_manager_type=SmartphoneConnectionManagerType.AZURE_SERVICE_BUS,
    storage_manager=InMemoryStorageManager(),
)

@router.post("/start")
async def start_conference(request: StartConferenceRequest):
    # Create and start the conference
    conference_call = await conference_manager.create_conference(request.teacher_phone, request.student_phones)
    return {
                "status": "Created", 
                "id": conference_call.conf_id
            }

@router.post("/smartphoneconnect")
async def connect_smartphone(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    return await conference.connect_smartphone()

@router.put("/end")
async def end_conference(request: EndConferenceRequest):
    # Create and start the conference
    conference_call = await conference_manager.end_conference(request.conference_id)
    return {
                "status": "End", 
                "conf": conference_call
            }

@router.put("/addparticipant")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.add_participant(phone_number)
    return {"message": "Participant added successfully"}

@router.put("/removeparticipant")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.remove_participant(phone_number)
    return {"message": "Participant removed successfully"}

@router.put("/muteparticipant")
async def mute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.mute_participant(phone_number)
    return {"message": "Participant muted successfully"}

@router.put("/unmuteparticipant")
async def unmute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.unmute_participant(phone_number)
    return {"message": "Participant unmuted successfully"}
