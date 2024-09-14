# routers/webhooks.py

from fastapi import APIRouter, Request, BackgroundTasks
from services.conference_call_manager import ConferenceCallManager

router = APIRouter()

# Assume conference_manager is already instantiated elsewhere
# You might need to import or pass it to the router

@router.post("/webhooks/event")
async def event_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    # Process the event data asynchronously
    background_tasks.add_task(process_event, event_data)
    return {"status": "ok"}

async def process_event(event_data: dict):
    # Extract necessary information from event_data
    phone_number = event_data.get('from')
    call_status = event_data.get('status')
    await conference_manager.update_participant_status(phone_number, call_status)

@router.post("/webhooks/conversation_events")
async def conversation_events_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    background_tasks.add_task(process_conversation_event, event_data)
    return {"status": "ok"}

async def process_conversation_event(event_data: dict):
    # Extract information such as audio playback status, mute/unmute events
    event_type = event_data.get('event')
    if event_type == 'audio_playback':
        status = event_data.get('status')
        await conference_manager.update_audio_playback_status(status)
    elif event_type == 'mute':
        phone_number = event_data.get('participant')
        is_muted = event_data.get('is_muted')
        await conference_manager.mute_unmute_event(phone_number, is_muted)

@router.post("/webhooks/input")
async def input_webhook(request: Request, background_tasks: BackgroundTasks):
    event_data = await request.json()
    background_tasks.add_task(process_input_event, event_data)
    return {"status": "ok"}

async def process_input_event(event_data: dict):
    phone_number = event_data.get('from')
    dtmf_digit = event_data.get('dtmf')
    await conference_manager.handle_dtmf_input(phone_number, dtmf_digit)
