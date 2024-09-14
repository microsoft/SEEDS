# services/conference_call_manager.py

from typing import Dict, List
from services.conference_call import ConferenceCall
from services.communication_api import CommunicationAPI
from services.storage_manager import StorageManager
from utils.smartphone_connection_manager import SmartphoneConnectionManager


class ConferenceCallManager:
    def __init__(
        self,
        communication_api: CommunicationAPI,
        storage_manager: StorageManager,
    ):
        self.communication_api = communication_api
        self.storage_manager = storage_manager
        self.conferences: Dict[str, ConferenceCall] = {}

    async def create_conference(self, teacher_phone: str, student_phones: List[str], smartphone_connection_manager: SmartphoneConnectionManager) -> ConferenceCall:
        # Create a new connection manager for this conference
        conference_call = ConferenceCall(
            communication_api=self.communication_api,
            storage_manager=self.storage_manager,
            connection_manager=smartphone_connection_manager,
        )
        await conference_call.start_conference(teacher_phone, student_phones)
        self.conferences[conference_call.conference_id] = conference_call
        return conference_call

    async def end_conference(self, conference_id: str):
        conf = self.get_conference(conference_id)
        if conf:
            await conf.end_conference()
            del self.conferences[conference_id]
        return conf

    def get_conference(self, conference_id: str) -> ConferenceCall:
        return self.conferences.get(conference_id, None)
