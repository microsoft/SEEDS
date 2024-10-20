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
        await websocket.accept()
        conf.set_websocket(websocket)
        print("WEBSOCKET ACCEPTED FOR CONF: ", conference_id)
        try:
            while True:
                try:
                    msg = await websocket.receive()  # Use receive_text or receive_json based on your message type
                    # Check if the message type indicates the connection is closing
                    if msg['type'] == 'websocket.disconnect':
                        print(f"WebSocket Client disconnected for {conference_id}")
                        conf.set_websocket(None)  # Clear the WebSocket on disconnection
                        break  # Exit the loop to stop processing once disconnected

                    print('RECEIVED WEBSOCKET MSG for conf ID: ', conference_id)
                    # Handle incoming messages or keep the connection alive
                    await asyncio.sleep(1)  # Simulate activity to keep connection open
                except WebSocketDisconnect:
                    print(f"WebSocket Client disconnected for {conference_id}")
                    conf.set_websocket(None)  # Clear the WebSocket on disconnection
                    break  # Exit the loop to stop processing once disconnected
                except Exception as e:
                    print(f"An error occurred while receiving message: {e}")
                    traceback.print_exc()
                    conf.set_websocket(None)
                    break  # Exit the loop to stop processing on error
        except Exception as e:
            print(f"An error occurred in websocket router: {e}")
            traceback.print_exc()
            conf.set_websocket(None)  # Ensure cleanup in case of error
    
