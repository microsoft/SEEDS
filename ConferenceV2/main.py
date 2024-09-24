# main.py

from fastapi import FastAPI
from routers import conference, webhooks, websocket

app = FastAPI(title="Conference Call System")

app.include_router(conference.router, prefix="/conference", tags=["Conference"])
app.include_router(webhooks.router, prefix="/webhooks",  tags=["Webhooks"])
app.include_router(websocket.router, prefix="/websocket", tags=["Websocket for Comm API"])
