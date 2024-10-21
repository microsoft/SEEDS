# routers/webhooks.py

import json
from fastapi import APIRouter, Request, BackgroundTasks, HTTPException, Response
from fastapi.responses import JSONResponse
from models.participant import CallStatus
from services.confevents.mute_participant_event import MuteParticipantEvent
from services.confevents.vonage.vonage_call_status_change_event import VonageCallStatusChangeEvent
from services.confevents.vonage.vonage_dtmf_input_event import VonageDTMFInputEvent, VonageRTCEventType
from services.conference_call_manager import ConferenceCallManager
from typing import Dict
from conf_logger import logger_instance


router = APIRouter()

# Import the conference_manager instance
from routers.conference import conference_manager

from fastapi import APIRouter, Request, Response

router = APIRouter()

@router.post("/event/{conference_id}")
async def event_webhook(request: Request, conference_id: str, background_tasks: BackgroundTasks):
    # logger_instance.info("RECEIVED EVENT for ", conference_id)
    event_data = await request.json()
    background_tasks.add_task(process_event, event_data, conference_id)
    return {"status": "ok"}

@router.post("/conversationevents")
async def conversation_events_webhook(request: Request, background_tasks: BackgroundTasks):
    # logger_instance.info("CONV EVENT RECEIVED")
    event_data = await request.json()
    background_tasks.add_task(process_conversation_event, event_data)
    return {"status": "ok"}

async def process_event(event_data: Dict, conference_id: str):
    try: 
        conf = conference_manager.get_conference(conference_id)
        if conf:
            vonage_call_status_change_event = VonageCallStatusChangeEvent(**event_data)
            call_status_change_event = vonage_call_status_change_event.get_conf_call_status_change_event(conf)
            await conf.queue_event(call_status_change_event)

            # If a student just connected, mute the student
            student_phone_numbers = [student.phone_number for student in conf.state.get_students()]
            if call_status_change_event.status == CallStatus.CONNECTED and call_status_change_event.phone_number in student_phone_numbers: 
                await conf.queue_event(MuteParticipantEvent(phone_number=call_status_change_event.phone_number, conf_call=conf))
    except:
        logger_instance.info("NOT a call_status_change_event")
        
async def process_conversation_event(event_data: Dict):
    try:
        vonage_dtmf_input_event = VonageDTMFInputEvent(**event_data)
        if vonage_dtmf_input_event.type == VonageRTCEventType.DTMF:
            conf = conference_manager.get_conference_from_phone_number(vonage_dtmf_input_event.get_user_phone_number())
            if conf:
                dtmf_input_event = vonage_dtmf_input_event.get_conf_dtmf_input_event(conf)
                logger_instance.info(json.dumps(event_data, indent=2))
                await conf.queue_event(dtmf_input_event)
    
    except:
        logger_instance.info("NOT a dtmf_input_event")
