# routers/conference.py

import asyncio
import traceback
import uuid
from fastapi import APIRouter, Request, WebSocket, WebSocketDisconnect
from typing import List

from routers.conference import conference_manager

router = APIRouter()
# settings = get_settings()

# TODO: CLOSE WEBSOCKET CONNECTION WHEN CONF ENDS
@router.websocket("/{conference_id}")
async def websocket_endpoint(websocket: WebSocket, conference_id: str):
    conf = conference_manager.get_conference(conference_id)
    if conf:
        print("WEBSOCKET ACCEPTED FOR CONF: ", conference_id)
        await websocket.accept()
        conf.set_websocket(websocket)
        # asyncio.create_task(conf.websocket_service.ensure_connection())
        try:
            while True:
                msg = await websocket.receive()
                print('RECEIVED WEBSOCKET MSG')
                # Keep the connection alive or handle incoming messages here if needed
                await asyncio.sleep(10)  # Simulate some activity to keep connection open
        except WebSocketDisconnect:
            print(f"Websocket Client disconnected for {conference_id}")
            conf.set_websocket(None)  # Clear the WebSocket on disconnection
        except Exception as e:
            print(f"An error occurred in websocket router: {e}")
            # Log the full stack trace
            traceback.print_exc()
            conf.set_websocket(None)  # Ensure cleanup in case of error
    
