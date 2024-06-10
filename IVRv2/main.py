import json
import uuid
from fastapi import FastAPI, Request, Response, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ValidationError
import traceback
from datetime import datetime
import time
from utils.enums import CallStatus, ConversationRTCEventType
from utils.functions import CustomJSONEncoder, format_data_html
import vonage
from fastapi.responses import JSONResponse
import traceback  
from dotenv import load_dotenv
import os
import asyncio
from actions.base_actions.talk_action import TalkAction

from actions.vonage_actions.vonage_action_factory import VonageActionFactory
from fsm.instantiation import instantiate_from_latest_content, instantitate_from_doc
from fsm.radio_instantiation import instantiate_from_content_ids
from utils.mongodb import MongoDB
from fastapi.responses import HTMLResponse
from fsm.visualiseIVR import get_latest_content, process_content
from utils.model_classes import ConversationRTCWebhookRequest, DTMFInput, EventWebhookRequest, IVRCallStateMongoDoc, IVRfsmDoc, \
    MongoCreds, StartIVRFormData, StreamPlaybackInfo, UserAction, VonageCallStartResponse, BulkCallRequest, FSMRequest
import copy

load_dotenv()

application_id = os.getenv("VONAGE_APPLICATION_ID")

fsm = dict()
latest_fsm_id = None

app = FastAPI()

# Add CORS middleware configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # List of allowed origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

mongo_creds = MongoCreds(host=os.environ.get("MONGO_HOST"),
                         password=os.environ.get("MONGO_PASSWORD"),
                         port=int(os.environ.get("MONGO_PORT")),
                         user_name=os.environ.get("MONGO_USER_NAME"))

ongoing_fsm_mongo = MongoDB(conn_creds=mongo_creds, 
                            db_name="ivr", 
                            collection_name="ongoingIVRState")

ivrv2_logs_mongo = MongoDB(conn_creds=mongo_creds, 
                            db_name="ivr", 
                            collection_name="ivrV2Logs")

fsm_json_mongo = MongoDB(conn_creds=mongo_creds, 
                         db_name="ivr",
                         collection_name="fsm")

radio_fsm_mongo = MongoDB(conn_creds=mongo_creds, 
                          db_name="ivr", 
                          collection_name="radio")

action_factory = VonageActionFactory()

accumulator = action_factory.get_action_accumulator_implmentation()


@app.get("/ivr_structure", response_class=HTMLResponse, description="This API returns the IVR structure in a visual format")
async def get_ivr_structure():
    content = await get_latest_content()
    structured_content = process_content(content)
    print(structured_content)
    html_content = format_data_html(structured_content)
    return html_content



@app.get("/getFSM")
async def get_fsm(fsm_id: str):
    # global fsm
    fsm_by_id = None
    doc = await fsm_json_mongo.find_by_id(fsm_id)
    if doc is not None:
        fsm_by_id = instantitate_from_doc(IVRfsmDoc(**doc))
    else:
        doc = await radio_fsm_mongo.find_by_id(fsm_id)
        if doc is not None:
            fsm_by_id = instantitate_from_doc(IVRfsmDoc(**doc))
    # fsm_by_id = fsm.get(fsm_id)
    if fsm_by_id is not None:
        new_fsm = copy.deepcopy(fsm_by_id)
        states = []
        for state_id in fsm_by_id.states:
            # print("STATE", state_id)
            state_object = fsm_by_id.states[state_id]
            # print("STATE OBJECT fsm.states[state] ", state_object)
            new_state = dict()
            new_state['id'] = state_object.id
            new_state['menu'] = state_object.menu.dict() if state_object.menu is not None else None
            new_state['transition_map'] = state_object.serialize_transitions()
            states.append(new_state)
        new_fsm.states = states
        return new_fsm  
    else:
        raise HTTPException(status_code=404, detail="FSM not found")

@app.on_event("startup")
async def startup_event():
    global fsm
    global latest_fsm_id
    
    latest_doc = await fsm_json_mongo.collection.find_one(sort=[("created_at", -1)])
    if latest_doc != None: 
        latest_fsm = instantitate_from_doc(IVRfsmDoc(**latest_doc))
        fsm[latest_fsm.fsm_id] = latest_fsm
        latest_fsm_id = latest_fsm.fsm_id
        # fsm = instantitate_from_doc(IVRfsmDoc(**latest_doc))
        print("Instantiated FSM with id: ", latest_fsm.fsm_id)
    else:
        print("No FSM found in MongoDB, please call `updateivr` API to create a new FSM object from latest content before calling any APIs")

@app.post("/updateivr")
async def update_ivr(request: Request, response: Response):
    global fsm
    global latest_fsm_id
    
    # FIND ONGOING FSM COUNT
    docs = await ongoing_fsm_mongo.find_all()
    if len(docs) > 0:
        response.status_code = 409
        return {"message": f"Cannot Update IVR right now. {len(docs)} users are currently using it. Please try again after an hour.", \
            "status_code": response.status_code}
    
    updated_fsm = await instantiate_from_latest_content()
    fsm[updated_fsm.fsm_id] = updated_fsm  
    latest_fsm_id = updated_fsm.fsm_id  
    # fsm = await instantiate_from_latest_content()
    current_fsm_doc = updated_fsm.serialize()
    
    response_message = "Successfully created FSM. "
    
    # CHECK IF THE LATEST CONTENT FSM IS SAME AS THE LATEST FSM STORED IN MONGO
    latest_doc = await fsm_json_mongo.collection.find_one(sort=[("created_at", -1)])
    if latest_doc != None:
        latest_fsm_doc = IVRfsmDoc(**latest_doc)
        
        if current_fsm_doc != latest_fsm_doc:
            # CURRENT FSM IS DIFFERENT, SAVE IT IN MONGO
            await fsm_json_mongo.insert(current_fsm_doc.dict())
            response_message += "Current FSM is different from previous FSM. Added a new FSM in mongo."
        else:
            # USE SAME FSM ID AS IN LATEST FSM DOC FROM MONGO
            fsm[latest_fsm_doc.id] = updated_fsm
            del fsm[updated_fsm.fsm_id]
            latest_fsm_id = latest_fsm_doc.id
            # latest_fsm_id = latest_fsm_doc.id
            # fsm.fsm_id = latest_fsm_doc.id
            response_message += "Current FSM and FSM in mongo are same, skipping addition of new FSM to mongo."
    else:
        await fsm_json_mongo.insert(current_fsm_doc.dict())
        response_message += "FSM collection was empty. Added a new FSM in mongo."
    
    response.status_code = 200
    return {"message": response_message, 
            "status_code": response.status_code}
    
@app.post("/startivr")
async def start_ivr(response: Response, sender: str = Form(...)):
    global fsm
    try:
        client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))

        # form_data = await request.form()
        # data = dict(form_data)
        # phone_number = data.get('sender', None)

        sender_data = StartIVRFormData(sender=sender)
        phone_number = sender_data.sender
        
        # Extract the 'sender' value from the form data
        # if phone_number is None:
        #     response.status_code = 400
        #     return {"detail": "Sender value is required"}
        
        print("RECIEVED START IVR CALL FOR PHONE NUMBER", phone_number)
        
        doc = await ongoing_fsm_mongo.find({'phone_number': phone_number})
        if doc != None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            # CHECK IF LAST CALL HAPPENED STALE_WAIT_IN_SECONDS SECONDS BEFORE, 
            # IF THIS IS THE CASE IT IS ASSUMED THAT THE DOC FOUND IS STALE
            # - DELETE THE DOC
            # - HANG UP THE CALL IN CASE ITS STILL UP : TODO
            if (datetime.now() - ivr_state.created_at).total_seconds() / 60 > \
                int(os.environ.get("STALE_WAIT_IN_SECONDS", 60)):
                await ongoing_fsm_mongo.delete(phone_number)
            
            # OTHERWISE DON'T ALLOW THE CALL TO BE STARTED
            else:
                response.status_code = 403
                return {"message": "IVR already running for phone number: " + phone_number}
            
            
        latest_fsm = fsm[latest_fsm_id]
        ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in latest_fsm.get_start_fsm_actions()])

        # ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.get_start_fsm_actions()])
        print("NCCO:", json.dumps(ncco_actions, indent=2))
        
        vonage_resp = client.voice.create_call({
            'to': [{'type': 'phone', 'number': phone_number}],
            'from': {'type': 'phone', 'number': os.getenv("VONAGE_NUMBER")},
            'ncco': ncco_actions,
            'length_timer': os.getenv("CALL_DURATION_LIMIT")
        })
        vonage_resp = VonageCallStartResponse(**vonage_resp)
        print("VONAGE RESPONSE", vonage_resp)
        
        ivr_call_state = IVRCallStateMongoDoc(_id = vonage_resp.conversation_uuid, 
                                              phone_number = phone_number,
                                              fsm_id=latest_fsm.fsm_id,
                                              current_state_id = latest_fsm.init_state_id,
                                              created_at = datetime.now())
        
        await ongoing_fsm_mongo.insert(ivr_call_state.dict())
        
        response.status_code = 200
        return {"message": "IVR started for phone number: " + phone_number}
    except Exception as e:
        error_traceback = traceback.format_exc()
        print(error_traceback)  # Log the traceback for debugging purposes
        response.status_code = 500
        return {"error": "An error occurred while processing the request.", "details": error_traceback}



@app.post("/start_bulk_calls")
async def start_bulk_calls(request: BulkCallRequest):
    global fsm
    try:
        phone_numbers = request.phone_numbers
        content_ids = request.content_ids
        # Step 3: Get the FSM using content IDs
        print(content_ids, phone_numbers)
        radio_fsm = await instantiate_from_content_ids(content_ids=content_ids)
        fsm[radio_fsm.fsm_id] = radio_fsm
        
        
        # Step 4: Store the FSM in the 'radio' collection in 'ivr' DB
        
        radio_fsm_doc = radio_fsm.serialize()
        with open("radio_fsm.json", "w") as f:
            f.write(json.dumps(radio_fsm_doc.dict(), indent=2))
        # print(json.dumps(radio_fsm_doc.dict(), indent=2))
        await radio_fsm_mongo.insert(radio_fsm_doc.dict())
        
        # Step 5: Initiate calls at a rate of one per second
        client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))
        
        count = len(phone_numbers)
        ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in radio_fsm.get_start_fsm_actions()])
        print("NCCO:", json.dumps(ncco_actions, indent=2))
        
        for phone_number in phone_numbers:
            doc = await ongoing_fsm_mongo.find({'phone_number': phone_number})
            if doc != None:
                count -= 1
                ivr_state = IVRCallStateMongoDoc(**doc)
                # CHECK IF LAST CALL HAPPENED STALE_WAIT_IN_SECONDS SECONDS BEFORE, 
                # IF THIS IS THE CASE IT IS ASSUMED THAT THE DOC FOUND IS STALE
                # - DELETE THE DOC
                # - HANG UP THE CALL IN CASE ITS STILL UP : TODO
                if (datetime.now() - ivr_state.created_at).total_seconds() / 60 > \
                    int(os.environ.get("STALE_WAIT_IN_SECONDS", 60)):
                    await ongoing_fsm_mongo.delete(phone_number)
                
                # OTHERWISE DON'T ALLOW THE CALL TO BE STARTED
                else:
                    continue
                    # response.status_code = 403
                    # return {"message": "IVR already running for phone number: " + phone_number}
            
            
            # print("NCCO:", json.dumps(ncco_actions, indent=2))
            vonage_resp = client.voice.create_call({
                'to': [{'type': 'phone', 'number': phone_number}],
                'from': {'type': 'phone', 'number': os.getenv("VONAGE_NUMBER")},
                'ncco': ncco_actions,
                'length_timer': os.getenv("CALL_DURATION_LIMIT")
            })
            vonage_resp = VonageCallStartResponse(**vonage_resp)
            
            ivr_call_state = IVRCallStateMongoDoc(_id = vonage_resp.conversation_uuid, 
                                              phone_number = phone_number,
                                              fsm_id=radio_fsm.fsm_id,
                                              current_state_id = radio_fsm.init_state_id,
                                              created_at = datetime.now())
        
            await ongoing_fsm_mongo.insert(ivr_call_state.dict())
        
            print(f"Call started for phone number {phone_number} with conversation UUID: {vonage_resp.conversation_uuid}")
            
            # Sleep for one second before initiating the next call
            await asyncio.sleep(1)
        
        # write to bulk collection, get the user id initiating the bulk call
        return {"message": f"Calls initiated for {count} phone numbers."} # Return list of conv IDs of calls
    
    except Exception as e:
        error_traceback = traceback.format_exc()
        print(error_traceback)  # Log the traceback for debugging purposes
        raise HTTPException(status_code=500, detail={"error": "An error occurred while processing the request.", "details": error_traceback})
    
@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.get("/answer")
def get_answer():
    # talk = Ncco.Talk(text='Hello from Vonage SERVER!', bargeIn=True, loop=5, premium=True)
    # ncco = Ncco.build_ncco(record, connect, talk)
    # return ncco
    ncco = [
        {
            "action": "talk",
            "text": "Hello from Vonage Answer URL!",
            "bargeIn": True,
            "loop": 5
        }
    ]
    return ncco

@app.post("/event")
async def get_event(req: Request, response: Response):
    try:
        req_data = await req.json()
        print("Raw REQUEST Data:", req_data)
        
        event_request = EventWebhookRequest(**req_data)
        print("EVENT RECEIVED: ", json.dumps(event_request.dict(), cls=CustomJSONEncoder, indent=2))
        doc = await ongoing_fsm_mongo.find_by_id(event_request.conversation_uuid)
        if doc is not None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            ivr_state.call_status_updates[event_request.timestamp] = event_request.status.value
        
        if event_request.status in CallStatus.get_end_call_enums():
            if doc is None:
                print("ERROR: NO ONGOING IVR STATE FOUND FOR CONV ID: ", event_request.conversation_uuid)
                response.status_code = 400
                return {"message": f"event received, no ongoing fsm found with id {event_request.conversation_uuid}"}
            
            
            ivr_state.stopped_at = datetime.now()
            ivr_state.duration = event_request.duration
            
            doc = await ivrv2_logs_mongo.find_by_id(ivr_state.id)
            if doc is None:
                await ivrv2_logs_mongo.insert(ivr_state.dict())
                await ongoing_fsm_mongo.delete(doc_id=event_request.conversation_uuid)
            else:
                print("DOC ALREADY EXISTS AND DUPLICATE KEY ERROR WOULD BE RAISED")
                print(doc)
        
        else:
            if doc is not None:
                await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
        
        response.status_code = 200
        return {"message": "event received"}
    except ValidationError as ve:
        error_traceback = traceback.format_exc()
        print("Validation error:", ve.errors())
        print("Request data causing the error:", req_data)
        print(error_traceback)  # Log the traceback for debugging purposes
        response.status_code = 422
        return {"error": "Validation error occurred while processing the request.", "details": ve.errors()}
    except Exception as e:
        error_traceback = traceback.format_exc()
        print(error_traceback)  # Log the traceback for debugging purposes
        response.status_code = 500
        return {"error": "An error occurred while processing the request.", "details": error_traceback}

@app.post("/conversation_events")
async def get_conv_event(req: ConversationRTCWebhookRequest):
    """
    Handles incoming RTC webhook requests related to conversation events, specifically audio play events.
    It updates IVR state documents in mongo, based on the type of audio event received.

    For an 'audio:play' event:
    - Checks if the current state of the conversation in the database, has a stream action configured
      with record playback set to true and a stream URL that matches the one received in the event payload.
    - If a match is found, a new entry of type `StreamPlaybackInfo` is appended in the `stream_playback` 
      attribute of IVR state document

    For 'audio:play:stop' and 'audio:play:done' events:
    - Checks the play ID provided in the event against the IVR state document.
    - Updates the corresponding 'stoppedAt' and 'doneAt' timestamps for the playback information in the document.
    """
    global fsm
    if req.type == ConversationRTCEventType.AUDIO_PLAY and \
        "stream_url" in req.body and \
            "play_id" in req.body:
        doc = await ongoing_fsm_mongo.find_by_id(req.conversation_id)
        if doc is not None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            fsm_in_progress = fsm[ivr_state.fsm_id]
            current_state = fsm_in_progress.get_state(ivr_state.current_state_id)
            req_stream_url = req.body["stream_url"][0]
            if current_state is not None:
                stream_actions = current_state.get_stream_action_with_record_playback_option()
                for action in stream_actions:
                    if req_stream_url.startswith(action.url): # IGNORE THE SAS PART OF THE stream URL
                        print(json.dumps(req.dict(), indent=2, cls=CustomJSONEncoder))
                        ivr_state.stream_playback.append(StreamPlaybackInfo(
                            play_id=req.body["play_id"],
                            stream_url=action.url,
                            started_at=req.timestamp
                        ))
                        await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
    elif req.type == ConversationRTCEventType.AUDIO_PLAY_STOP and \
        "play_id" in req.body:
        doc = await ongoing_fsm_mongo.find_by_id(req.conversation_id)
        if doc is not None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            req_play_id = req.body["play_id"]
            should_update = False
            for playback_info in ivr_state.stream_playback:
                if playback_info.play_id == req_play_id:
                    print(json.dumps(req.dict(), indent=2, cls=CustomJSONEncoder))
                    playback_info.stopped_at = req.timestamp
                    should_update = True
                    break
            if should_update:
                await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
    elif req.type == ConversationRTCEventType.AUDIO_PLAY_DONE and \
        "play_id" in req.body:
        doc = await ongoing_fsm_mongo.find_by_id(req.conversation_id)
        if doc is not None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            req_play_id = req.body["play_id"]
            should_update = False
            for playback_info in ivr_state.stream_playback:
                if playback_info.play_id == req_play_id:
                    print(json.dumps(req.dict(), indent=2, cls=CustomJSONEncoder))
                    playback_info.done_at = req.timestamp
                    should_update = True
                    break
            if should_update:
                await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
    return {"message": "recorded"}


@app.post("/input")
async def dtmf(input: Request):
    global fsm
    
    input_data = await input.json()
    print("INPUT DATA RAW", input_data)
    input = DTMFInput(**input_data)
    # print(f"Received request body: {input}")
    digits = input.dtmf.digits
    conv_id = input.conversation_uuid
    doc = await ongoing_fsm_mongo.find_by_id(conv_id)
    if doc == None:
        print("ERROR: NO ONGOING IVR STATE FOUND FOR CONV ID: ", conv_id)
        # Talk Action of server error bye bye
        # internal_server_action = ask Kavyansh if it's fine to import TalkAction here
        ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in [TalkAction(text="Server error. Please try again later. Bye bye.")]])
        return JSONResponse(ncco)
    
    ivr_state = IVRCallStateMongoDoc(**doc)
    
    fsm_in_progress = fsm[ivr_state.fsm_id]
    print('iS FSM IN PROGRESS NONE', fsm_in_progress == None)
    # PROCESS MULTIPLE USER INPUTS
    input_time = datetime.now()
    print("CURRENT STATE ID", ivr_state.current_state_id)
    print("INPUT DIGITS", digits)
    next_actions, next_state_id = None, None
    for digit in digits:
        pre_state_id = ivr_state.current_state_id
        next_actions, next_state_id = await fsm_in_progress.get_next_actions(digit, ivr_state)
        ivr_state.current_state_id = next_state_id
        ivr_state.user_actions.append(UserAction(key_pressed=digit, timestamp=input_time, pre_state_id = pre_state_id, post_state_id = next_state_id))
        
    if digits == '':
        pre_state_id = ivr_state.current_state_id
        next_actions, next_state_id = await fsm_in_progress.get_next_actions('', ivr_state)
        ivr_state.current_state_id = next_state_id
        ivr_state.user_actions.append(UserAction(key_pressed="empty", timestamp=input_time, pre_state_id = pre_state_id, post_state_id = next_state_id))
        
    await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
    start = time.time()
    ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in next_actions])
    print('TIME TAKEN TO CREATE NCCO ', time.time() - start)
    print("NCCO RETURNED FROM INPUT API: ", json.dumps(ncco, indent=2))
    return JSONResponse(ncco)
    
@app.post("/fallback")
def get_answer():
    return {"hello": "world"}
