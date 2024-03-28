from pydantic import BaseModel


class DTMFDetails(BaseModel):
    digits: str
    timed_out: bool

class DTMFInput(BaseModel):
    dtmf: DTMFDetails
    to: str