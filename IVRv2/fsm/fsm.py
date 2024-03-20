from fsm.state import State
from fsm.transition import Transition
from base_classes.action import Action

class FSM:
    def __init__(self, fsm_id: str):
        self.fsm_id = fsm_id
        self.states: dict[str, State] = {}
        self.init_state_id = "0"
        self.current_state_id = self.init_state_id
    
    def add_state(self, state: State):
        if state.id in self.states:
            raise ValueError(f"State with id {state.id} already exists")
        self.states[state.id] = state
    
    def set_init_state_id(self, state_id: str):
        if state_id not in self.states:
            raise ValueError(f"Cannot set initial state to {state_id}, as it does not exist")
        self.init_state_id = state_id
        self.current_state_id = state_id
    
    def add_transition(self, transition: Transition):
        if transition.source_state_id not in self.states:
            raise ValueError(f"Cannot add Transition for State with id {transition.source_state_id}, as it does not exist")
        self.states[transition.source_state_id].add_transition(transition)
        
    def get_start_fsm_actions(self) -> [Action]:
        if self.init_state_id not in self.states:
            raise ValueError(f"Initial State with id {self.init_state_id} does not exist")
        return self.states[self.init_state_id].actions
    
    def get_next_actions(self, input_: str) -> [Action]:
        if self.current_state_id not in self.states:
            raise ValueError(f"Current State with id {self.current_state_id} does not exist")
        
        if input_ not in self.states[self.current_state_id].transition_map:
            return [TalkAction("Invalid Input")] + self.states[self.current_state_id].actions
        
        dest_state_id = self.states[self.current_state_id].transition_map[input_].dest_state_id
        transition_actions = self.states[self.current_state_id].transition_map[input_].actions
        self.current_state_id = dest_state_id
                
        return transition_actions + self.states[self.current_state_id].actions
    
    