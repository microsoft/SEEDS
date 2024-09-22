import json
from models.participant import Participant
from utils.smartphone_connection_manager import SmartphoneConnectionManager
from azure.identity import DefaultAzureCredential
from azure.servicebus import ServiceBusClient, ServiceBusMessage
from azure.servicebus.management import ServiceBusAdministrationClient


class AzureServiceBusSmartphoneConnectionManager(SmartphoneConnectionManager):
    def __init__(self, topic_name: str, ns_name: str, conf_id: str):
        self.conf_id = conf_id
        self.topic_name = topic_name
        self.ns_name = ns_name
        self.subscription_name_base = f"conference_{self.conf_id}_subscription_participant_"

        credential = DefaultAzureCredential()

        # Initialize Service Bus Administration Client for managing topics and subscriptions
        self.admin_client = ServiceBusAdministrationClient(
            fully_qualified_namespace=topic_name,
            credential=credential
        )

        # Initialize Service Bus Client for sending messages
        self.servicebus_client = ServiceBusClient(
            fully_qualified_namespace=ns_name,
            credential=credential
        )
    
    async def connect(self, client: Participant):
        subscription_name = self.subscription_name_base + client.phone_number

        # Check if subscription already exists
        existing_subs = self.admin_client.get_subscription(self.topic_name, subscription_name)
        if existing_subs:
            return {"status": "CREATE_SUBSCRIPTION_EXISTS", 
                    "subscription_name": subscription_name}

        # Create subscription and add a filter that only allows messages with 'conference_id' property
        self.admin_client.create_subscription(
            topic_name=self.topic_name,
            subscription_name=subscription_name
        )
        
        # Add a SQL filter to deliver only messages related to this conference_id
        self.admin_client.create_rule(
            topic_name=self.topic_name,
            subscription_name=subscription_name,
            rule_name="conference_filter",
            filter="conference_id = '{conference_id}'"
        )
        
        # Remove the default rule, as we have our specific filter rule
        self.admin_client.delete_rule(
            topic_name=self.topic_name,
            subscription_name=subscription_name,
            rule_name="$Default"
        )

        return {"status": "CREATE_SUBSCRIPTION_SUCCESS", 
                "subscription_name": subscription_name}

    async def disconnect(self, client: Participant):
        subscription_name = self.subscription_name_base + client.phone_number

        # Check if the subscription exists before trying to delete
        existing_subs = self.admin_client.get_subscription(self.topic_name, subscription_name)
        if not existing_subs:
            {"status": "DELETE_SUBSCRIPTION_NOTFOUND", 
             "subscription_name": subscription_name}

        # Delete the subscription
        self.admin_client.delete_subscription(
            topic_name=self.topic_name,
            subscription_name=subscription_name
        )

        return {"status": "DELETE_SUBSCRIPTION_SUCCESS", 
                "subscription_name": subscription_name}

    async def send_message_to_client(self, client: Participant, message: dict):
        str_message = json.dumps(message)
        sender = self.servicebus_client.get_topic_sender(topic_name=self.topic_name)
        message = ServiceBusMessage(
            str_message,
            application_properties={"conference_id": self.conf_id}  # Include conference_id as a property
        )

        with sender:
            sender.send_messages(message)

        return {"status": "MESSAGE_SENT", 
                "conference_id": self.conf_id, 
                "message": str_message}