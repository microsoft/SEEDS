from dataclasses import dataclass
from pydantic import BaseModel


class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails
    to: str
    
class StartIVRRequest(BaseModel):
    phone_number: str
    
@dataclass
class MongoCreds:
    host: str
    password: str
    port: int
    user_name: str
