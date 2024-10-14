# Enum for animal types
from enum import Enum
import os
from dotenv import load_dotenv
from services.smartphone_connection_manager import AzureServiceBusSmartphoneConnectionManager, SmartphoneConnectionManager
from services.smartphone_connection_manager.sse_connection_manager import SSEConnectionManager

load_dotenv()

class SmartphoneConnectionManagerType(Enum):
    AZURE_SERVICE_BUS = "service_bus"
    SSE = "sse"

class SmartphoneConnectionManagerFactory:
    @staticmethod
    def create(type: SmartphoneConnectionManagerType, conf_id: str) -> SmartphoneConnectionManager:
        if type == SmartphoneConnectionManagerType.AZURE_SERVICE_BUS:
            return AzureServiceBusSmartphoneConnectionManager(
                        topic_name=os.environ.get("SERVICE_BUS_TOPIC_NAME"),
                        ns_name=os.environ.get("SERVICE_BUS_NS_NAME"),
                        conf_id=conf_id,
                    )
        if type == SmartphoneConnectionManagerType.SSE:
            return SSEConnectionManager()
        else:
            raise ValueError(f"Unknown COMM API type: {type}")
