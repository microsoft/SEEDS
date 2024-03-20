from base_classes.action import Action
from fsm.transition import Transition

class State:
    def __init__(self, state_id: str, actions: [Action]):
        self.id = state_id
        self.actions = actions
        self.transition_map: dict[str, Transition] = {}
        
    def add_transition(self, transition: Transition):
        if transition.input in self.transition_map:
            raise ValueError(f"Transition for input {transition.input} already exists")
        self.transition_map[transition.input] = transition