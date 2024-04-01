from typing import List, Tuple
from fsm.state import State
from fsm.transition import Transition
from base_classes.action import Action
from actions.base_actions.talk_action import TalkAction

class FSM:
    def __init__(self, fsm_id: str):
        self.fsm_id = fsm_id
        self.states: dict[str, State] = {}
        self.init_state_id = "0"
        self.on_error_actions = [TalkAction("Invalid Input")]
    
    def add_state(self, state: State):
        if state.id in self.states:
            raise ValueError(f"State with id {state.id} already exists")
        self.states[state.id] = state
    
    def set_init_state_id(self, state_id: str):
        if state_id not in self.states:
            raise ValueError(f"Cannot set initial state to {state_id}, as it does not exist")
        self.init_state_id = state_id
    
    def add_transition(self, transition: Transition):
        if transition.source_state_id not in self.states:
            raise ValueError(f"Cannot add Transition for State with id {transition.source_state_id}, as it does not exist")
        self.states[transition.source_state_id].add_transition(transition)
        
    def get_start_fsm_actions(self) -> List[Action]:
        if self.init_state_id not in self.states:
            raise ValueError(f"Initial State with id {self.init_state_id} does not exist")
        return self.states[self.init_state_id].actions
    
    def get_next_actions(self, input_: str, current_state_id: str) -> Tuple[List[Action], str]:
        if current_state_id not in self.states:
            raise ValueError(f"Current State with id {current_state_id} does not exist")
        
        if input_ not in self.states[current_state_id].transition_map:
            return self.on_error_actions + self.states[current_state_id].actions, current_state_id
        
        dest_state_id = self.states[current_state_id].transition_map[input_].dest_state_id
        transition_actions = self.states[current_state_id].transition_map[input_].actions
                
        return transition_actions + self.states[dest_state_id].actions, dest_state_id
    
    def visualize_fsm(self, current_state_id=None, depth=0, visited=None):
        # Initialize the recursion
        if visited is None:
            visited = set()
        if current_state_id is None:
            current_state_id = self.init_state_id
            # Start with the initial state
            tree_str = f"FSM ID: {self.fsm_id}\n"
        else:
            tree_str = ""
        
        # Avoid revisiting states to prevent infinite loops in cyclic FSMs
        if current_state_id in visited:
            return tree_str
        visited.add(current_state_id)
        
        current_state = self.states[current_state_id]
        indent = "    " * depth  # Indentation for visualization
        
        # Add the current state to the visualization
        tree_str += f"{indent}State ID: {current_state_id}\n"
        
        # Iterate over each transition from the current state
        for input_, transition in current_state.transition_map.items():
            tree_str += f"{indent}    Transition on '{input_}' to State ID: {transition.dest_state_id}\n"
            
            # Recursively visualize the destination state
            tree_str += self.visualize_fsm(transition.dest_state_id, depth + 2, visited)
        
        return tree_str

    
    