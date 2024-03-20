from abc import ABC, abstractmethod

class Action(ABC):
    @abstractmethod
    def get(self):
        pass
    
    def __repr__(self):
        return self.__str__()
    