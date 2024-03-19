from fastapi import FastAPI, Request
import vonage
from pydantic import BaseModel
from typing import Optional
from fastapi.responses import JSONResponse
# from vonage.voice import Ncco

app = FastAPI()

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

class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails

@app.post("/input")
async def dtmf(input: DTMFInput):
    print(f"Received request body: {input}")
    
    ncco = [
        {
            "action": "talk",
            "text": f"You pressed {input.dtmf.digits}, goodbye"
        }
    ]
    return JSONResponse(ncco)
    

@app.get("/fallback")
def get_answer():
    return {"hello": "world"}
