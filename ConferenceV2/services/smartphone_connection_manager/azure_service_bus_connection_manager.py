import json
import random
from models.participant import Participant
from services.smartphone_connection_manager.base_smartphone_connection_manager import SmartphoneConnectionManager
from azure.identity import DefaultAzureCredential
from azure.servicebus import ServiceBusClient, ServiceBusMessage
from azure.servicebus.management import ServiceBusAdministrationClient
from azure.servicebus.management import SqlRuleFilter


class AzureServiceBusSmartphoneConnectionManager(SmartphoneConnectionManager):
    def __init__(self, topic_name: str, ns_name: str, conf_id: str):
        self.conf_id = conf_id
        self.topic_name = topic_name
        self.ns_name = ns_name
        conf_id_part = ''.join(random.sample(self.conf_id.replace('-', ''), 10))
        self.subscription_name = f"conference_{conf_id_part}_subscription"

        credential = DefaultAzureCredential()

        # Initialize Service Bus Administration Client for managing topics and subscriptions
        self.admin_client = ServiceBusAdministrationClient(
            fully_qualified_namespace=self.ns_name,
            credential=credential
        )

        # Initialize Service Bus Client for sending messages
        self.servicebus_client = ServiceBusClient(
            fully_qualified_namespace=ns_name,
            credential=credential
        )
    
    async def connect(self, client: Participant):
        # Create subscription and add a filter that only allows messages with 'conference_id' property
        self.admin_client.create_subscription(
            topic_name=self.topic_name,
            subscription_name=self.subscription_name
        )
        
        # Use SQLRuleFilter instead of plain string for the filter
        filter_expression = SqlRuleFilter(f"conference_id = '{self.conf_id}'")
        
        # Add the SQL filter to the rule
        self.admin_client.create_rule(
            topic_name=self.topic_name,
            subscription_name=self.subscription_name,
            rule_name="conference_filter",
            filter=filter_expression
        )
        
        # Remove the default rule, as we have our specific filter rule
        self.admin_client.delete_rule(
            topic_name=self.topic_name,
            subscription_name=self.subscription_name,
            rule_name="$Default"
        )

        return {"status": "CREATE_SUBSCRIPTION_SUCCESS", 
                "subscription_name": self.subscription_name}

    async def disconnect(self, client: Participant):
        # Check if the subscription exists before trying to delete
        existing_subs = self.admin_client.get_subscription(self.topic_name, self.subscription_name)
        if not existing_subs:
            {"status": "DELETE_SUBSCRIPTION_NOTFOUND", 
             "subscription_name": self.subscription_name}

        # Delete the subscription
        self.admin_client.delete_subscription(
            topic_name=self.topic_name,
            subscription_name=self.subscription_name
        )

        return {"status": "DELETE_SUBSCRIPTION_SUCCESS", 
                "subscription_name": self.subscription_name}

    async def send_message_to_client(self, client: Participant, message: dict):
        str_message = json.dumps(message)
        sender = self.servicebus_client.get_topic_sender(topic_name=self.topic_name)
        service_bus_message = ServiceBusMessage(
            str_message,
            application_properties={"conference_id": self.conf_id}  # Include conference_id as a property
        )

        with sender:
            sender.send_messages(service_bus_message)

        return {"status": "MESSAGE_SENT", 
                "conference_id": self.conf_id, 
                "message": str_message}