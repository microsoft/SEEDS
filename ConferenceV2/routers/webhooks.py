# routers/webhooks.py

from fastapi import APIRouter, Request, BackgroundTasks, HTTPException
from services.conference_call_manager import ConferenceCallManager
from typing import Dict

router = APIRouter()

# Import the conference_manager instance
from routers.conference import conference_manager

@router.post("/event")
async def event_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    background_tasks.add_task(process_event, event_data)
    return {"status": "ok"}

async def process_event(event_data: Dict):
    # TODO: Think about how to get `conference_id` from event_data, without following the specific vonage event request payload
    conference_id = event_data.get('conference_id')
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        return
    await conference.process_webhook_event(event_data)

@router.post("/conversation_events")
async def conversation_events_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    background_tasks.add_task(process_conversation_event, event_data)
    return {"status": "ok"}

async def process_conversation_event(event_data: Dict):
    conference_id = event_data.get('conference_id')
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        return
    await conference.process_webhook_conversation_event(event_data)

@router.post("/input")
async def input_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    background_tasks.add_task(process_input_event, event_data)
    return {"status": "ok"}

async def process_input_event(event_data: Dict):
    conference_id = event_data.get('conference_id')
    conference = conference_manager.get_conference(conference_id)
    if not conference:
        return
    await conference.process_webhook_input_event(event_data)
