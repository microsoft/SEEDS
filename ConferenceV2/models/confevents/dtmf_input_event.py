from pydantic import BaseModel

from models.participant import CallStatus

class DTMFInputEvent(BaseModel):
    phone_number: str = ""
    digit: str = ""