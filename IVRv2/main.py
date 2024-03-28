import json
from fastapi import FastAPI, Request, Response
import vonage
from pydantic import BaseModel
from typing import Optional
from fastapi.responses import JSONResponse

from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction
from actions.vonage_actions.vonage_action_factory import VonageActionFactory

from fsm.state import State
from fsm.transition import Transition
from fsm.fsm import FSM

from dotenv import load_dotenv
import os
from utils.model_classes import DTMFInput
from utils.mongodb import MongoDB
from utils.sas_gen import SASGen

load_dotenv()

application_id = os.getenv("VONAGE_APPLICATION_ID")
client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))


# state actions and menu actions (so that cat sound is not repeated on invalid input)- Roshni
# replicate Mani's IVR using FSM - Roshni
# SAS - vonage stream action - Roshni 


# current_state_id an attribute of the user and store in DB - Kavyansh
# send current_state_id to fsm class functions - Kavyansh



# JSON representation of FSM
# Web app to create FSM

# from vonage.voice import Ncco

app = FastAPI()
ongoing_fsm_mongo = MongoDB(conn_str=os.getenv("MONGO_CONN_STR"), 
                            db_name="ivr", 
                            collection_name="ongoingIVRState")

fsm = FSM(fsm_id="fsm1")
input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')
state1 = State(state_id="1", actions = [StreamAction(url = 'https://contentmenu.blob.core.windows.net/menu/WelcomeToSeedsNinad.mp3'),
                                        TalkAction(text="Press 1 to start and press 3 to exit"),
                                        input_action])

state2 = State(state_id="2", actions = [TalkAction("For cats press 1"),
                                        TalkAction("For lion press 2"),
                                        input_action])

state3 = State(state_id="3", actions = [StreamAction(url = "https://contentmenu.blob.core.windows.net/animalsounds/cat.mp3"),
                                        TalkAction("Press 1 for main menu, press 2 for previous menu, press 3 to exit"),
                                        input_action])

state4 = State(state_id="4", actions = [StreamAction(url = "https://contentmenu.blob.core.windows.net/animalsounds/lion.mp3"),
                                        TalkAction("Press 1 for main menu, press 2 for previous menu, press 3 to exit"),
                                        input_action])

state5 = State(state_id="5", actions = [TalkAction("Thank you for calling")])

t1 = Transition(source_state_id="1", dest_state_id="2", input="1", actions=[])
t2 = Transition(source_state_id="1", dest_state_id="5", input="3", actions=[])

t3 = Transition(source_state_id="2", dest_state_id="3", input="1", actions=[TalkAction("You selected cats")])
t4 = Transition(source_state_id="2", dest_state_id="4", input="2", actions=[TalkAction("You selected lion")])

t5 = Transition(source_state_id="3", dest_state_id="1", input="1", actions=[])
t6 = Transition(source_state_id="3", dest_state_id="2", input="2", actions=[])
t7 = Transition(source_state_id="3", dest_state_id="5", input="3", actions=[])

t8 = Transition(source_state_id="4", dest_state_id="1", input="1", actions=[])
t9 = Transition(source_state_id="4", dest_state_id="2", input="2", actions=[])
t10 = Transition(source_state_id="4", dest_state_id="5", input="3", actions=[])

fsm.add_state(state1)
fsm.add_state(state2)
fsm.add_state(state3)
fsm.add_state(state4)
fsm.add_state(state5)

fsm.set_init_state_id("1")

fsm.add_transition(t1)
fsm.add_transition(t2)
fsm.add_transition(t3)
fsm.add_transition(t4)
fsm.add_transition(t5)
fsm.add_transition(t6)
fsm.add_transition(t7)
fsm.add_transition(t8)
fsm.add_transition(t9)
fsm.add_transition(t10)


action_factory = VonageActionFactory()

accumulator = action_factory.get_action_accumulator_implmentation()

@app.post("/startivr")
async def start_ivr(request: Request, response: Response):
    data = await request.json()
    phone_number = data.get("phone_number")
    print(f"Received request body: {data}")
    print("PHONE NUMBER", phone_number)
    
    doc = await ongoing_fsm_mongo.find_by_id(phone_number)
    if doc != None:
        response.status_code = 403
        return {"message": "IVR already running for phone number: " + phone_number}
    current_state_id = "1" # SET TO INITIAL STATE
    doc = {"_id": phone_number, "current_state_id": current_state_id} 
    await ongoing_fsm_mongo.insert(doc)
 
    ncco_actions = accumulator.combine([action_factory.get_action_implmentation(x) for x in fsm.get_start_fsm_actions()])
    print("NCCO:", json.dumps(ncco_actions, indent=2))
    
    response = client.voice.create_call({
        'to': [{'type': 'phone', 'number': phone_number}],
        'from': {'type': 'phone', 'number': os.getenv("VONAGE_NUMBER")},
        'ncco': ncco_actions
    })
    
    print("RESPONSE", response)
    print()
    response.status_code = 200
    return {"message": "IVR started for phone number: " + phone_number}


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
def get_event():
    return {"hello": "world"}

@app.post("/conversation_events")
def get_event():
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
    ncco = accumulator.combine([action_factory.get_action_implmentation(x) 
                                for x in fsm.get_next_actions(digits, current_user_state_id)])
    print("NCCO", json.dumps(ncco, indent=2))
    return JSONResponse(ncco)
    
@app.get("/fallback")
def get_answer():
    return {"hello": "world"}
