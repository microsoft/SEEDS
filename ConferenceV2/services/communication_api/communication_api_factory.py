# Enum for animal types
from enum import Enum
import os
from services.communication_api import CommunicationAPI
from dotenv import load_dotenv
load_dotenv()

class CommunicationAPIType(Enum):
    VONAGE = "vonage"

class CommunicationAPIFactory:
    @staticmethod
    def create(type: CommunicationAPIType, conf_id: str, ws_url: str) -> CommunicationAPI:
        from services.communication_api import VonageAPI
        
        if type == CommunicationAPIType.VONAGE:
            return VonageAPI(application_id=os.environ.get("VONAGE_APPLICATION_ID"),
                             private_key_path=os.environ.get("VONAGE_PRIVATE_KEY_PATH"),
                             vonage_number=os.environ.get("VONAGE_NUMBER"),
                             conf_id=conf_id, 
                             ws_server_url=ws_url)
        else:
            raise ValueError(f"Unknown COMM API type: {type}")
