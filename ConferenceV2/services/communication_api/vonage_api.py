import asyncio
import os
import time
import uuid
from models.webhook_event import WebHookEvent
from services.communication_api import CommunicationAPI
from typing import Dict, List, Optional
import aiohttp
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
    def __init__(self, application_id: str, private_key_path: str, vonage_number: str, conf_id: str):
        self.application_id = application_id
        self.private_key_path = private_key_path
        self.vonage_number = vonage_number
        self.conf_id = conf_id
        self.client = vonage.Client(application_id=self.application_id, private_key=self.private_key_path)
        self.participant_info_map: Dict[str, VonageParticipantInfo] = {}
    
    # TODO: Connect a websocket to the call
    async def start_conf(self, teacher_phone: str, student_phones: List[str]):
        """
        Starts a conference call between a teacher and students using Vonage API.
        """
        call_payload = {"type": "phone", "number": teacher_phone}
        call_data = {
            "to": [call_payload],
            "from": {"type": "phone", "number": self.vonage_number},
            "ncco": [{"action": "conversation", "name": self.conf_id}]
        }
        vonage_resp = self.client.voice.create_call(call_data)
        print("VONAGE TEACHER RESPONSE", json.dumps(vonage_resp, indent=2))
        self.participant_info_map[teacher_phone] = VonageParticipantInfo(
                                                        phone_number=teacher_phone,
                                                        call_leg_id=vonage_resp['uuid'],
                                                        conv_id=vonage_resp['conversation_uuid'])

        for student_phone in student_phones:
            call_payload = {"type": "phone", "number": student_phone}
            call_data = {
                "to": [call_payload],
                "from": {"type": "phone", "number": self.vonage_number},
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
            self.client.voice.update_call(uuid=participant.call_leg_id, action="hangup")

    # client.create_call()
    async def add_participant(self, phone_number: str):
        """
        Adds a participant to an ongoing call.
        """
        call_payload = {"type": "phone", "number": phone_number}
        call_data = {
            "to": [call_payload],
            "from": {"type": "phone", "number": self.vonage_number},
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

    # client.update_call()
    async def mute_participant(self, phone_number: str):
        """
        Mutes a participant in the call.
        """
        if phone_number in self.participant_info_map:
            participant_info = self.participant_info_map[phone_number]
            self.client.voice.update_call(uuid=participant_info.call_leg_id, action="mute")

    # client.update_call()
    async def unmute_participant(self, conference_id: str, phone_number: str):
        """
        Unmutes a participant in the call.
        """
        if phone_number in self.participant_info_map:
            participant_info = self.participant_info_map[phone_number]
            self.client.voice.update_call(uuid=participant_info.call_leg_id, action="unmute")

    # TODO: Start streaming audio bytes to the call websocket
    async def play_audio(self, conference_id: str, url: str):
        """
        Plays an audio file into the conference.
        """
        pass
        # api_url = f"{self.base_url}/calls/{conference_id}/stream"
        # data = {
        #     "stream_url": [url]
        # }
        # async with aiohttp.ClientSession() as session:
        #     async with session.put(api_url, headers=self.headers, json=data) as response:
        #         if response.status == 200:
        #             return await response.json()
        #         else:
        #             raise Exception(f"Error playing audio: {response.status}, {await response.text()}")

    # TODO: Stop streaming audio bytes to the call websocket
    async def pause_audio(self, conference_id: str):
        """
        Pauses the currently playing audio.
        """
        pass
        # api_url = f"{self.base_url}/calls/{conference_id}/stream"
        # async with aiohttp.ClientSession() as session:
        #     async with session.delete(api_url, headers=self.headers) as response:
        #         if response.status == 200:
        #             return await response.json()
        #         else:
                    # raise Exception(f"Error pausing audio: {response.status}, {await response.text()}")

    def parse_event_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        # Parse the incoming event webhook (e.g., for call status updates)
        pass

    def parse_conversation_event_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        # Parse conversation-related webhook events
        pass

    def parse_input_webhook(self, request_data: dict) -> Optional[WebHookEvent]:
        # Parse input-related webhook events (e.g., DTMF input)
        pass


# # Run the async function
# if __name__ == "__main__":
#     async def execute():
#         instance = VonageAPI(application_id=os.environ.get("VONAGE_APPLICATION_ID"), 
#                             private_key_path=os.environ.get("VONAGE_PRIVATE_KEY_PATH"))
#         await instance.start_conf("917999435373", ["919606612444"], str(uuid.uuid4()))
#         time.sleep(10)
#         print("ENDING CALLS NOW...")
#         await instance.end_conf()

#     asyncio.run(execute())
