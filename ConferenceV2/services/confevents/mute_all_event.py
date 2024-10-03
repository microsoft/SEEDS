import asyncio
from datetime import datetime
from pydantic import BaseModel
from models.action_history import ActionHistory, ActionType
from services.confevents.base_event import ConferenceEvent
from models.participant import Role
from services.conference_call import ConferenceCall


class MuteAllEvent(ConferenceEvent, BaseModel):
    pass

