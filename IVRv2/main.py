import json
from fastapi import FastAPI, Request, Response, HTTPException, Form
from pydantic import BaseModel
from datetime import datetime
import vonage
from fastapi.responses import JSONResponse
import traceback  
from dotenv import load_dotenv
import os

from actions.vonage_actions.vonage_action_factory import VonageActionFactory
from fsm.instantiation import instantiate_from_latest_content, instantitate_from_json
from utils.model_classes import CallStatus, DTMFInput, EventWebhookRequest, IVRCallStateMongoDoc, MongoCreds, StartIVRFormData, VonageCallStartResponse
from utils.mongodb import MongoDB
from fastapi.responses import HTMLResponse
from fsm.visualiseIVR import get_latest_content, process_content

load_dotenv()

application_id = os.getenv("VONAGE_APPLICATION_ID")
print("APP ID", application_id)
client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))


# state actions and menu actions (so that cat sound is not repeated on invalid input)- Roshni
# replicate Mani's IVR using FSM - Roshni
# SAS - vonage stream action - Roshni 

# JSON representation of FSM
# Web app to create FSM

# from vonage.voice import Ncco

fsm = None
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

@app.get("/ivr_structure", response_class=HTMLResponse)
async def get_ivr_structure():
    content = await get_latest_content()
    structured_content = process_content(content)
    print(structured_content)
    html_content = format_data_html(structured_content)
    return html_content

def format_data_html(data, level=0):
    if isinstance(data, dict):
        role = 'group' if level > 0 else 'tree'  # Use 'tree' role for the top level and 'group' for nested lists
        items = f'<ul role="{role}">'
        for key, value in data.items():
            # Using 'treeitem' role for items and specifying 'aria-level' for better depth understanding
            items += f'<li role="treeitem" aria-level="{level + 1}"><strong>{key}</strong>{format_data_html(value, level + 1)}</li>'
        items += '</ul>'
        return items
    elif isinstance(data, set):
        # Leaf nodes are simple list items without a specific role needed as they do not expand further
        items = '<ul>'
        for item in data:
            items += f'<li>{item}</li>'
        items += '</ul>'
        return items
    return ''

@app.on_event("startup")
async def startup_event():
    resp = await update_ivr(None, Response())
    if resp["status_code"] != 200:
        raise ValueError("CANNOT INIT IVR FSM")

@app.post("/updateivr")
async def update_ivr(request: Request, response: Response):
    global fsm
    
    # FIND ONGOING FSM COUNT
    docs = await ongoing_fsm_mongo.find_all()
    if len(docs) > 0:
        response.status_code = 409
        return {"message": f"Cannot Update IVR right now. {len(docs)} users are currently using it. Please try again after an hour.", \
            "status_code": response.status_code}
        
    fsm = await instantiate_from_latest_content()
    # fsm = instantitate_from_json()
    # print(fsm.visualize_fsm())
    response.status_code = 200
    return {"message": "SUCCESS", "status_code": response.status_code}
    
@app.post("/startivr")
async def start_ivr(response: Response, sender: str = Form(...)):
    try:
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
            # CHECK IF LAST CALL HAPPENEDSTALE_WAIT_IN_SECONDS SECONDS BEFORE, 
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
        
        ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.get_start_fsm_actions()])
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
                                              created_at = datetime.now(), 
                                              current_state_id = fsm.init_state_id)
        
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
        print("EVENT RECEIVED : ", req)
        if req.status in CallStatus.get_end_call_enums():
            await ongoing_fsm_mongo.delete(doc_id=req.conversation_uuid)
        
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
async def dtmf(input: Request):
    global fsm
    
    input_data = await input.json()
    print("INPUT DATA", input_data)
    input = DTMFInput(**input_data)
    print(f"Received request body: {input}")
    digits = input.dtmf.digits
    print("DIGITS", digits)
    conv_id = input.conversation_uuid
    doc = await ongoing_fsm_mongo.find_by_id(conv_id)
    if doc == None:
        print("ERROR: NO ONGOING IVR STATE FOUND FOR CONV ID: ", conv_id)
        #Called even after cutting the call
        ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.on_error_actions])
        return JSONResponse(ncco)
    
    ivr_state = IVRCallStateMongoDoc(**doc)
    current_user_state_id = ivr_state.current_state_id
    next_actions, next_state_id = fsm.get_next_actions(digits, current_user_state_id)
    
    ivr_state.current_state_id = next_state_id
    await ongoing_fsm_mongo.update_document(ivr_state.id, ivr_state.dict())
    
    ncco = accumulator.combine([action_factory.get_action_implmentation(x) for x in next_actions])
    print("NCCO", json.dumps(ncco, indent=2))
    return JSONResponse(ncco)
    
@app.get("/fallback")
def get_answer():
    return {"hello": "world"}
