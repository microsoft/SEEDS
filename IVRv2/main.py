import json
from fastapi import FastAPI, Request, Response
from datetime import datetime
import vonage
from fastapi.responses import JSONResponse
import traceback  
from dotenv import load_dotenv
import os

from actions.vonage_actions.vonage_action_factory import VonageActionFactory
from fsm.instantiation import fsm
from utils.model_classes import CallStatus, DTMFInput, EventWebhookRequest, IVRCallStateMongoDoc, MongoCreds, StartIVRRequest
from utils.mongodb import MongoDB

load_dotenv()

application_id = os.getenv("VONAGE_APPLICATION_ID")
client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))


# state actions and menu actions (so that cat sound is not repeated on invalid input)- Roshni
# replicate Mani's IVR using FSM - Roshni
# SAS - vonage stream action - Roshni 

# JSON representation of FSM
# Web app to create FSM

# from vonage.voice import Ncco

app = FastAPI()
mongo_creds = MongoCreds(host=os.environ.get("MONGO_HOST"),
                         password=os.environ.get("MONGO_PASSWORD"),
                         port=int(os.environ.get("MONGO_PORT")),
                         user_name=os.environ.get("MONGO_USER_NAME"))

ongoing_fsm_mongo = MongoDB(conn_creds=mongo_creds, 
                            db_name="ivr", 
                            collection_name="ongoingIVRState")

action_factory = VonageActionFactory()

accumulator = action_factory.get_action_accumulator_implmentation()

@app.post("/startivr")
async def start_ivr(request: StartIVRRequest, response: Response):
    try:
        phone_number = request.phone_number
        print(f"Received request body: {json.dumps(request.dict(), indent=2)}")
        print("PHONE NUMBER", phone_number)
        
        doc = await ongoing_fsm_mongo.find_by_id(phone_number)
        if doc != None:
            ivr_state = IVRCallStateMongoDoc(**doc)
            # CHECK IF LAST CALL HAPPENEDSTALE_WAIT_IN_SECONDS SECONDS BEFORE, 
            # IF THIS IS THE CASE IT IS ASSUMED THAT THE DOC FOUND IS STALE
            # - DELETE THE DOC
            # - HANG UP THE CALL IN CASE ITS STILL UP : TODO
            if (datetime.now() - ivr_state.createdAt).total_seconds() / 60 > \
                int(os.environ.get("STALE_WAIT_IN_SECONDS", 60)):
                await ongoing_fsm_mongo.delete(phone_number)
            
            # OTHERWISE DON'T ALLOW THE CALL TO BE STARTED
            else:
                response.status_code = 403
                return {"message": "IVR already running for phone number: " + phone_number}
        
        ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.get_start_fsm_actions()])
        print("NCCO:", json.dumps(ncco_actions, indent=2))
        
        vonage_resp = client.voice.create_call({
            'to': [{'type': 'phone', 'number': phone_number}],
            'from': {'type': 'phone', 'number': os.getenv("VONAGE_NUMBER")},
            'ncco': ncco_actions
        })
        
        print("VONAGE RESPONSE", vonage_resp)
        
        ivr_call_state = IVRCallStateMongoDoc(_id=phone_number, 
                                    createdAt=datetime.now(), 
                                    current_state_id="1")
        await ongoing_fsm_mongo.insert(ivr_call_state.dict())
        
        response.status_code = 200
        return {"message": "IVR started for phone number: " + phone_number}
    except Exception as e:
        error_traceback = traceback.format_exc()
        print(error_traceback)  # Log the traceback for debugging purposes
        response.status_code = 500
        return {"error": "An error occurred while processing the request.", "details": error_traceback}


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
async def get_event(req: EventWebhookRequest, response: Response):
    try:
        if req.status in CallStatus.get_end_call_enums():
            await ongoing_fsm_mongo.delete(doc_id=req.to)
        
        response.status_code = 200
        return {"message": "event received"}
    except Exception as e:
            error_traceback = traceback.format_exc()
            print(error_traceback)  # Log the traceback for debugging purposes
            response.status_code = 500
            return {"error": "An error occurred while processing the request.", "details": error_traceback}




@app.post("/conversation_events")
async def get_conv_event(req: Request):
    # req_json = await req.json()
    # print("CONV URL RECEIVED REQ")
    # print(json.dumps(req_json, indent=2))
    return {"hello": "world"}

@app.post("/input")
async def dtmf(input: DTMFInput):
    print(f"Received request body: {input}")
    digits = input.dtmf.digits
    print("DIGITS", digits)
    phone_number = input.to
    doc = await ongoing_fsm_mongo.find_by_id(phone_number)
    if doc == None:
        ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.on_error_actions])
        return JSONResponse(ncco)
    
    current_user_state_id = doc["current_state_id"]
    next_actions, next_state_id = fsm.get_next_actions(digits, current_user_state_id)
    doc["current_state_id"] = next_state_id
    await ongoing_fsm_mongo.update_document(doc["_id"], doc)
    
    ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in next_actions])
    print("NCCO", json.dumps(ncco, indent=2))
    return JSONResponse(ncco)
    
@app.get("/fallback")
def get_answer():
    return {"hello": "world"}
