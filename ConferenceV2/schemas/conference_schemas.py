# schemas/conference_schemas.py

from pydantic import BaseModel
from typing import List


class StartConferenceRequest(BaseModel):
    teacher_phone: str
    student_phones: List[str]
