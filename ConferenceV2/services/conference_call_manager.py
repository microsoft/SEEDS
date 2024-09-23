# services/conference_call_manager.py

from typing import Dict, List
import uuid
from services.communication_api import CommunicationAPIFactory, CommunicationAPIType
from services.storage_manager import StorageManager
from services.smartphone_connection_manager import SmartphoneConnectionManagerType, SmartphoneConnectionManagerFactory
from services.conference_call import ConferenceCall


class ConferenceCallManager:
    def __init__(
        self,
        communication_api_type: CommunicationAPIType,
        smartphone_connection_manager_type: SmartphoneConnectionManagerType,
        storage_manager: StorageManager,
    ):
        self.communication_api_type = communication_api_type
        self.smartphone_connection_manager_type = smartphone_connection_manager_type
        self.storage_manager = storage_manager
        self.communication_api_factory = CommunicationAPIFactory()
        self.smartphone_connection_manager_factory = SmartphoneConnectionManagerFactory()
        self.conferences: Dict[str, ConferenceCall] = {}

    def create_conference(self, teacher_phone: str, student_phones: List[str]) -> ConferenceCall:
        conf_id = str(uuid.uuid4())
        conference_call = ConferenceCall(
            conf_id=conf_id,
            communication_api=self.communication_api_factory.create(self.communication_api_type, conf_id),
            connection_manager=self.smartphone_connection_manager_factory.create(self.smartphone_connection_manager_type, conf_id),
            storage_manager=self.storage_manager
        )
        conference_call.set_participant_state(teacher_phone, student_phones)
        self.conferences[conf_id] = conference_call
        return conf_id
       
    async def start_conference_call(self, conf_id: str, ) -> ConferenceCall:
        conf: ConferenceCall = self.get_conference(conf_id)
        if not conf:
            raise ValueError(f"No such conference has been created; ID: {conf_id}")
        
        await conf.start_conference()
        
    async def end_conference(self, conference_id: str):
        conf: ConferenceCall = self.get_conference(conference_id)
        if conf:
            print('ENDING CONF', conference_id)
            await conf.end_conference()
            del self.conferences[conference_id]
        return conf

    def get_conference(self, conference_id: str) -> ConferenceCall | None:
        return self.conferences.get(conference_id, None)
