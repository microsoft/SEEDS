# routers/conference.py

import uuid
from fastapi import APIRouter, Depends, HTTPException, Request, WebSocket
from typing import List
from services.conference_call import ConferenceCall
from services.conference_call_manager import ConferenceCallManager
from services.communication_api import CommunicationAPIType
from services.storage_manager import InMemoryStorageManager
from services.smartphone_connection_manager import SmartphoneConnectionManagerType
from schemas.conference_schemas import CreateConferenceRequest
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

@router.post("/test-createstart")
async def create_start_conference(request: CreateConferenceRequest):
    conference_call_id = conference_manager.create_conference(request.teacher_phone, request.student_phones)
    await conference_manager.start_conference_call(conference_call_id)
    return {
                "status": "STARTED", 
                "id": conference_call_id
            }

@router.post("/create")
async def create_conference(request: CreateConferenceRequest):
    conference_call_id = conference_manager.create_conference(request.teacher_phone, request.student_phones)
    return {
                "status": "CREATED", 
                "id": conference_call_id
            }

@router.post("/start/{conference_id}")
async def start_conference(conference_id: str):
    await conference_manager.start_conference_call(conference_id)
    return {
                "status": "STARTED", 
                "id": conference_id
            }

@router.post("/teacherappconnect/{conference_id}")
async def connect_smartphone(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    return await conference.connect_smartphone()

@router.post("/teacherappdisconnect/{conference_id}")
async def disconnect_smartphone(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    return await conference.disconnect_smartphone()

@router.put("/end/{conference_id}")
async def end_conference(conference_id: str):
    conference_call = await conference_manager.end_conference(conference_id)
    return {
                "status": "END", 
                "conf": conference_call
            }

@router.put("/addparticipant/{conference_id}")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.add_participant(phone_number)
    return {"message": "Participant added successfully"}

@router.put("/removeparticipant/{conference_id}")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.remove_participant(phone_number)
    return {"message": "Participant removed successfully"}

@router.put("/muteparticipant/{conference_id}")
async def mute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.mute_participant(phone_number)
    return {"message": "Participant muted successfully"}

@router.put("/unmuteparticipant/{conference_id}")
async def unmute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.unmute_participant(phone_number)
    return {"message": "Participant unmuted successfully"}

@router.put("/playaudio/{conference_id}")
async def play_audio(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.play_content()
    return {"message": "Playing audio"}

@router.put("/pauseaudio/{conference_id}")
async def play_audio(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.pause_content()
    return {"message": "Playing audio"}
