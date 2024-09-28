# main.py

from fastapi import FastAPI
import socketio
from routers import conference, webhooks, websocket
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Conference Call System")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Adjust as needed
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(conference.router, prefix="/conference", tags=["Conference"])
app.include_router(webhooks.router, prefix="/webhooks",  tags=["Webhooks"])
app.include_router(websocket.router, prefix="/websocket", tags=["Websocket for Comm API"])
