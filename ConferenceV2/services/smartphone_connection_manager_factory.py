# Enum for animal types
from enum import Enum
import os
from dotenv import load_dotenv
from utils.azure_service_bus_connection_manager import AzureServiceBusSmartphoneConnectionManager
from utils.smartphone_connection_manager import SmartphoneConnectionManager
from services.vonage_api import VonageAPI

load_dotenv()

class SmartphoneConnectionManagerType(Enum):
    AZURE_SERVICE_BUS = "service_bus"

class SmartphoneConnectionManagerFactory:
    @staticmethod
    def create(type: SmartphoneConnectionManagerType, conf_id: str) -> SmartphoneConnectionManager:
        if type == SmartphoneConnectionManagerType.AZURE_SERVICE_BUS:
            return AzureServiceBusSmartphoneConnectionManager(
                        topic_name=os.environ.get("SERVICE_BUS_TOPIC_NAME"),
                        ns_name=os.environ.get("SERVICE_BUS_NS_NAME"),
                        conf_id=conf_id,
                    )
        else:
            raise ValueError(f"Unknown COMM API type: {type}")
