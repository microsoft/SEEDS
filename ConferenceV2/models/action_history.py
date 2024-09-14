# models/action_history.py

from pydantic import BaseModel
from datetime import datetime
from typing import Dict


class ActionHistory(BaseModel):
    timestamp: datetime
    action_type: str
    metadata: Dict
    owner: str  # Phone number or identifier of the user who performed the action
