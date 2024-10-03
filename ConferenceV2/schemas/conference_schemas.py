# schemas/conference_schemas.py

import os
from dotenv import load_dotenv
from pydantic import BaseModel
from typing import List

load_dotenv()

class CreateConferenceRequest(BaseModel):
    teacher_phone: str = os.environ.get("MY_NUMBER", "")
    student_phones: List[str] = [os.environ.get("FEATURE_PH", "")]