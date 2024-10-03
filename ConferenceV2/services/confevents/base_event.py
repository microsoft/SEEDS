from abc import ABC, abstractmethod

class ConferenceEvent(ABC):
    @abstractmethod
    async def execute_event(self):
        pass
    