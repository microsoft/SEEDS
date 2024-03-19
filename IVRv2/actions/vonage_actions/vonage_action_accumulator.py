from base_classes.action import Action
from base_classes.action_accumulator import ActionAccumulator

class VonageActionAccumulator(ActionAccumulator):
    def combine(self, actions: list[Action]):
        raise NotImplementedError