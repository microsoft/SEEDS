
from pydantic import BaseModel

from models.participant import CallStatus


class CallStatusChangeEvent(BaseModel):
    phone_number: str = ""
    status: CallStatus = CallStatus.DISCONNECTED