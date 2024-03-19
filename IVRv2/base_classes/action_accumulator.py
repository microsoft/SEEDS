from abc import ABC, abstractmethod
from base_classes.action import Action

class ActionAccumulator(ABC):
    @abstractmethod
    def combine(self, actions: list[Action]):
        pass