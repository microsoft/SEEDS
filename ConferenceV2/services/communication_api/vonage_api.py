import asyncio
from enum import Enum
import os
import time
import uuid
from models.webhook_event import WebHookEvent
from services.communication_api import CommunicationAPI
from typing import Any, Dict, List, Optional
import json
from dotenv import load_dotenv
import vonage
from typing import Dict
from pydantic import BaseModel

class VonageParticipantInfo(BaseModel):
    phone_number: str
    call_leg_id: str
    conv_id: str

load_dotenv()

class VonageAPI(CommunicationAPI):
# class VonageAPI:
    def __init__(self, application_id: str, private_key_path: str, vonage_number: str, conf_id: str, ws_server_url: str = ""):
        self.ws_server_url = ws_server_url
        self.events_webhook_url = os.environ.get("EVENTS_WEBHOOK_EP", "")
        self.application_id = application_id
        self.private_key_path = private_key_path
        self.vonage_number = vonage_number
        self.conf_id = conf_id
        self.client = vonage.Client(application_id=self.application_id, private_key=self.private_key_path)
        self.participant_info_map: Dict[str, VonageParticipantInfo] = {}
        self.teacher_phone_number = None
    
    # TODO: Connect a websocket to the call
    async def start_conf(self, teacher_phone: str, student_phones: List[str]):
        """
        Starts a conference call between a teacher and students using Vonage API.
        """
        call_payload = {"type": "phone", "number": teacher_phone}
        call_data = {
            "to": [call_payload],
            "from": {
                "type": "phone", 
                "number": self.vonage_number
            },
            "event_url": [
                self.events_webhook_url + f"/{self.conf_id}"
            ],
            "ncco": [
                {
                    "action": "conversation", 
                    "name": self.conf_id
                }
            ]
        }
        vonage_resp = self.client.voice.create_call(call_data)
        print("VONAGE TEACHER RESPONSE", json.dumps(vonage_resp, indent=2))
        self.teacher_phone_number = teacher_phone
        self.participant_info_map[teacher_phone] = VonageParticipantInfo(
                                                        phone_number=teacher_phone,
                                                        call_leg_id=vonage_resp['uuid'],
                                                        conv_id=vonage_resp['conversation_uuid'])
        
        print("CONNECTING CALL TO WEBSOCKET IN BACKGROUND")
        asyncio.create_task(self.connect_websocket())

        for student_phone in student_phones:
            call_payload = {"type": "phone", "number": student_phone}
            call_data = {
                "to": [call_payload],
                "from": {"type": "phone", "number": self.vonage_number},
                "event_url": [
                    self.events_webhook_url + f"/{self.conf_id}"
                ],
                "ncco": [{"action": "conversation", "name": self.conf_id}]
            }
            vonage_resp = self.client.voice.create_call(call_data)
            print("VONAGE STUDENT RESPONSE", json.dumps(vonage_resp, indent=2))
            self.participant_info_map[student_phone] = VonageParticipantInfo(
                                                            phone_number=student_phone,
                                                            call_leg_id=vonage_resp['uuid'],
                                                            conv_id=vonage_resp['conversation_uuid'])

    # client.update_call()
    async def end_conf(self):
        """
        Ends a call by its conference ID using the Vonage API.
        """
        for participant in self.participant_info_map.values():
            call_details = self.client.voice.get_call(uuid=participant.call_leg_id)
            if call_details['status'] == 'answered':
                print("ENDING CALL FOR PARTICIPANT", participant.phone_number)
                self.client.voice.update_call(uuid=participant.call_leg_id, action="hangup")
            else:
                print("CALL ALREADY ENDED FOR PARTICIPANT", participant.phone_number)
    
    async def connect_websocket(self):
        connected_ws = False
        while not connected_ws:
            # IF ANY PARTICIPANT PICKED UP THE CALL: CONNECT THE WEBSOCKET AND PUT BOTH OF THEM BACK INTO THE CONVERSATION. 
            # THIS IS BECAUSE ATLEAST ONE answered CALL LEG IS REQUIRED TO EXECUTE THE transfer ACTION. 
            for participant_ph_number in self.participant_info_map:
                participant = self.participant_info_map[participant_ph_number]
                call = self.client.voice.get_call(uuid=participant.call_leg_id)
                
                if call['status'] == 'answered':
                    print(f"{participant_ph_number} HAS ANSWERED THE CALL, CONNECTING WEBSOCKET NOW... URL:", self.ws_server_url)
                    self.client.voice.update_call(uuid=participant.call_leg_id, 
                                                    params={
                                                        "action": "transfer",
                                                        "destination": {
                                                            "type": "ncco",
                                                            "ncco": [
                                                                # {
                                                                #     "action": "talk",
                                                                #     "text": "Connecting websocket"
                                                                # },
                                                                {
                                                                    "action": "connect",
                                                                    "from": "SEEDS-ConfV2",
                                                                    "endpoint": [
                                                                        {
                                                                            "type": "websocket",
                                                                            "uri": self.ws_server_url,
                                                                            "content-type": "audio/l16;rate=8000",
                                                                        }
                                                                    ],
                                                                },
                                                                {
                                                                    "action": "conversation", 
                                                                    "name": self.conf_id
                                                                }
                                                            ]
                                                        }
                                                    })
                    connected_ws = True
                    break     

    # client.create_call()
    async def add_participant(self, phone_number: str):
        """
        Adds a participant to an ongoing call.
        """
        call_payload = {"type": "phone", "number": phone_number}
        call_data = {
            "to": [call_payload],
            "from": {"type": "phone", "number": self.vonage_number},
            "event_url": [
                self.events_webhook_url + f"/{self.conf_id}"
            ],
            "ncco": [{"action": "conversation", "name": self.conf_id}]
        }
        vonage_resp = self.client.voice.create_call(call_data)
        print("VONAGE ADD PARTICIPANT RESPONSE", json.dumps(vonage_resp, indent=2))
        self.participant_info_map[phone_number] = VonageParticipantInfo(
                                                        phone_number=phone_number,
                                                        call_leg_id=vonage_resp['uuid'],
                                                        conv_id=vonage_resp['conversation_uuid'])

    # client.update_call()
    async def remove_participant(self, phone_number: str):
        """
        Removes a participant from an ongoing call.
        """
        if phone_number in self.participant_info_map:
            participant_info = self.participant_info_map[phone_number]
            self.client.voice.update_call(uuid=participant_info.call_leg_id, action="hangup")
            del self.participant_info_map[phone_number]

    # client.update_call()
    async def mute_participant(self, phone_number: str):
        """
        Mutes a participant in the call.
        """
        if phone_number in self.participant_info_map:
            participant_info = self.participant_info_map[phone_number]
            self.client.voice.update_call(uuid=participant_info.call_leg_id, action="mute")

    # client.update_call()
    async def unmute_participant(self, phone_number: str):
        """
        Unmutes a participant in the call.
        """
        if phone_number in self.participant_info_map:
            participant_info = self.participant_info_map[phone_number]
            self.client.voice.update_call(uuid=participant_info.call_leg_id, action="unmute")
