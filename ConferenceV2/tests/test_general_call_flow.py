import pytest
import asyncio
from fastapi.testclient import TestClient
import os
import sys
from azure.identity.aio import DefaultAzureCredential
from azure.servicebus.aio import ServiceBusClient
from datetime import datetime, time
from dotenv import load_dotenv
import time

sys.path.append(os.path.dirname(os.path.abspath(__file__)) + "/../")

from main import app
from schemas.conference_schemas import CreateConferenceRequest, EndConferenceRequest, StartConferenceRequest

load_dotenv()

client = TestClient(app)

# Define your Azure Service Bus details
SERVICE_BUS_NAMESPACE = os.environ.get("SERVICE_BUS_NS_NAME") 
TOPIC_NAME = os.environ.get("SERVICE_BUS_TOPIC_NAME")
MY_NUMBER = os.environ.get("MY_NUMBER")
FEATURE_PH = os.environ.get("FEATURE_PH")

BASE_API = "conference"

# Function to listen to messages from Azure Service Bus
async def listen_to_service_bus(subscription_name: str):
    credential = DefaultAzureCredential()
    fully_qualified_namespace = SERVICE_BUS_NAMESPACE
    
    # Use the async version of ServiceBusClient
    async with ServiceBusClient(fully_qualified_namespace, credential) as servicebus_client:
        # Use async receiver
        async with servicebus_client.get_subscription_receiver(
                topic_name=TOPIC_NAME,
                subscription_name=subscription_name) as receiver:
            
            print(f"Listening to messages from topic '{TOPIC_NAME}' and subscription '{subscription_name}'...")
            
            async for msg in receiver:
                timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                message_body = ''.join([str(b) for b in msg.body])
                print(f"[{timestamp}] Received message: {message_body}")
                await receiver.complete_message(msg)

async def create_conf():
    response = client.post(f"{BASE_API}/create", json=CreateConferenceRequest(teacher_phone=MY_NUMBER, 
                                                                              student_phones=[FEATURE_PH]).model_dump())
    return response.json()

async def smartphone_connect(conf_id: str):
    smartphoneconnect_data = {"conference_id": conf_id}
    response = client.post(f"{BASE_API}/smartphoneconnect", json=smartphoneconnect_data)
    return response.json()

async def start_call(conf_id: str):
    response = client.post(f"{BASE_API}/start", json=StartConferenceRequest(conference_id=conf_id).model_dump())
    return response.json()

async def end_call(conf_id: str):
    response = client.put(f"{BASE_API}/end", json=EndConferenceRequest(conference_id=conf_id).model_dump(exclude_unset=True))
    return response.json()

async def remove_participant(conference_id: str, phone_number: str):
    response = client.put(f"{BASE_API}/removeparticipant", params={
        "conference_id": conference_id,
        "phone_number": phone_number
    })
    return response.json()

async def add_participant(conference_id: str, phone_number: str):
    response = client.put(f"{BASE_API}/addparticipant", params={
        "conference_id": conference_id,
        "phone_number": phone_number
    })
    return response.json()

async def mute_participant(conference_id: str, phone_number: str):
    response = client.put(f"{BASE_API}/muteparticipant", params={
        "conference_id": conference_id,
        "phone_number": phone_number
    })
    return response.json()

async def unmute_participant(conference_id: str, phone_number: str):
    response = client.put(f"{BASE_API}/unmuteparticipant", params={
        "conference_id": conference_id,
        "phone_number": phone_number
    })
    return response.json()

async def api_calls(conf_id: str):
    WAIT_TIME = 10

    print('STARTING CALL')
    await start_call(conf_id)
    print(f'SLEEPING FOR {WAIT_TIME} secs')
    await asyncio.sleep(WAIT_TIME)

    print('REMOVING PARTICIPANT', FEATURE_PH)
    await remove_participant(conf_id, FEATURE_PH)
    print(f'SLEEPING FOR {WAIT_TIME} secs')
    await asyncio.sleep(WAIT_TIME)

    print('ADDING PARTICIPANT', FEATURE_PH)
    await add_participant(conf_id, FEATURE_PH)
    print(f'SLEEPING FOR {WAIT_TIME} secs')
    await asyncio.sleep(WAIT_TIME)

    print('MUTING PARTICIPANT', MY_NUMBER)
    await mute_participant(conf_id, MY_NUMBER)
    print(f'SLEEPING FOR {WAIT_TIME} secs')
    await asyncio.sleep(WAIT_TIME)

    print('UNMUTING PARTICIPANT', MY_NUMBER)
    await unmute_participant(conf_id, MY_NUMBER)
    print(f'SLEEPING FOR {WAIT_TIME} secs')
    await asyncio.sleep(WAIT_TIME)

    print('ENDING...')
    await end_call(conf_id)

    return True
    

@pytest.mark.asyncio
async def test_api_calls_and_listen_to_messages():
    create_conf_response = await create_conf()
    conf_id = create_conf_response['id']
    print('CREATE CONF RESPONSE', create_conf_response)
    smartphone_connect_resp = await smartphone_connect(conf_id)
    print('SMARTPHONE CONNECT RESPONSE', smartphone_connect_resp)
    subs_name = smartphone_connect_resp['subscription_name']

    done, pending = await asyncio.wait(
                        [
                            asyncio.create_task(api_calls(conf_id)), 
                            asyncio.create_task(listen_to_service_bus(subs_name))
                         ],
                        return_when=asyncio.FIRST_COMPLETED
                    )

