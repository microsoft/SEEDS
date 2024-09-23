# schemas/conference_schemas.py

from pydantic import BaseModel
from typing import List

class CreateConferenceRequest(BaseModel):
    teacher_phone: str
    student_phones: List[str]

class StartConferenceRequest(BaseModel):
    conference_id: str

class EndConferenceRequest(BaseModel):
    conference_id: str

class SmartphoneConnectRequest(BaseModel):
    conference_id: str