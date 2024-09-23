import pytest
import asyncio
from fastapi.testclient import TestClient
import os
import sys
from azure.identity.aio import DefaultAzureCredential
from azure.servicebus.aio import ServiceBusClient
from datetime import datetime
from dotenv import load_dotenv

sys.path.append(os.path.dirname(os.path.abspath(__file__)) + "/../")

from main import app
from schemas.conference_schemas import CreateConferenceRequest, StartConferenceRequest

load_dotenv()

client = TestClient(app)

# Define your Azure Service Bus details
SERVICE_BUS_NAMESPACE = os.environ.get("SERVICE_BUS_NS_NAME") 
TOPIC_NAME = os.environ.get("SERVICE_BUS_TOPIC_NAME")

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
    response = client.post(f"{BASE_API}/create", json=CreateConferenceRequest(teacher_phone="917999435373", student_phones=["918904954955"]).model_dump())
    return response.json()

async def smartphone_connect(conf_id: str):
    smartphoneconnect_data = {"conference_id": conf_id}
    response = client.post(f"{BASE_API}/smartphoneconnect", json=smartphoneconnect_data)
    return response.json()

async def start_call(conf_id: str):
    response = client.post(f"{BASE_API}/start", json=StartConferenceRequest(conference_id=conf_id).model_dump())
    return response.json()

@pytest.mark.asyncio
async def test_api_calls_and_listen_to_messages():
    create_conf_response = await create_conf()
    print('CREATE CONF RESPONSE', create_conf_response)
    smartphone_connect_resp = await smartphone_connect(create_conf_response['id'])
    print('SMARTPHONE CONNECT RESPONSE', smartphone_connect_resp)
    subs_name = smartphone_connect_resp['subscription_name']
    await asyncio.gather(
        start_call(create_conf_response['id']),
        listen_to_service_bus(subs_name)
    )
