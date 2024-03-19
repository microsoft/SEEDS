from abc import ABC, abstractmethod
from base_classes.action import Action
from base_classes.action_accumulator import ActionAccumulator

class ActionFactory(ABC):
    @abstractmethod
    def get_action_implmentation(self, action: Action):
        pass
    
    @abstractmethod
    def get_action_accumulator_implmentation(self):
        pass

