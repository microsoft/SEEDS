# routers/conference.py

import uuid
from fastapi import APIRouter, Depends, HTTPException, Request, WebSocket
from typing import List
from services.conference_call import ConferenceCall
from services.conference_call_manager import ConferenceCallManager
from services.communication_api import CommunicationAPIType
from services.confevents.add_participant_event import AddParticipantEvent
from services.confevents.mute_participant_event import MuteParticipantEvent
from services.confevents.pause_content_event import PauseContentEvent
from services.confevents.play_content_event import PlayContentEvent
from services.confevents.remove_participant_event import RemoveParticipantEvent
from services.confevents.unmute_participant_event import UnmuteParticipantEvent
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
    smartphone_connection_manager_type=SmartphoneConnectionManagerType.SSE,
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

@router.get("/teacherappconnect/{conference_id}")
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
                "conf": conference_call.conf_id
            }

@router.put("/addparticipant/{conference_id}")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(AddParticipantEvent(phone_number=phone_number, conf_call=conference))
    return {"message": "Event Queued for execution"}

@router.put("/removeparticipant/{conference_id}")
async def add_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(RemoveParticipantEvent(phone_number=phone_number, conf_call=conference))
    return {"message": "Event Queued for execution"}

@router.put("/muteparticipant/{conference_id}")
async def mute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(MuteParticipantEvent(phone_number=phone_number, conf_call=conference))
    return {"message": "Event Queued for execution"}

@router.put("/unmuteparticipant/{conference_id}")
async def unmute_participant(conference_id: str, phone_number: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(UnmuteParticipantEvent(phone_number=phone_number, conf_call=conference))
    return {"message": "Event Queued for execution"}

@router.put("/playaudio/{conference_id}")
async def play_audio(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(PlayContentEvent(conf_call=conference))
    return {"message": "Event Queued for execution"}

@router.put("/pauseaudio/{conference_id}")
async def play_audio(conference_id: str):
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        raise HTTPException(status_code=404, detail="Conference not found")
    await conference.queue_event(PauseContentEvent(conf_call=conference))
    return {"message": "Event Queued for execution"}
