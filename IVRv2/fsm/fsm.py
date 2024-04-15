from typing import List, Tuple
from fsm.state import State
from fsm.transition import Transition
from base_classes.action import Action
from actions.base_actions.talk_action import TalkAction

class FSM:
    def __init__(self, fsm_id: str):
        self.fsm_id = fsm_id
        self.states: dict[str, State] = {}
        self.init_state_id = "LA0"
        self.on_error_actions = [TalkAction("Invalid Input")]
        self.end_state = State(state_id="END", actions=[TalkAction("Bye bye")])
        self.add_state(self.end_state)
    
    def add_state(self, state: State):
        if state.id in self.states:
            raise ValueError(f"State with id {state.id} already exists")
        self.states[state.id] = state
    
    def set_init_state_id(self, state_id: str):
        if state_id not in self.states:
            raise ValueError(f"Cannot set initial state to {state_id}, as it does not exist")
        self.init_state_id = state_id
    
    def add_transition(self, transition: Transition):
        if transition.source_state_id not in self.states or transition.dest_state_id not in self.states:
            raise ValueError(f"Cannot add Transition for State with id {transition.source_state_id}, as it does not exist")
        self.states[transition.source_state_id].add_transition(transition)
        
    def get_start_fsm_actions(self) -> List[Action]:
        if self.init_state_id not in self.states:
            raise ValueError(f"Initial State with id {self.init_state_id} does not exist")
        return self.states[self.init_state_id].actions
    
    def get_next_actions(self, input_: str, current_state_id: str) -> Tuple[List[Action], str]:
        print("Input", input_)
        print("Current State", current_state_id)
        self.print_state_transitions(current_state_id)
        
        if current_state_id not in self.states:
            raise ValueError(f"Current State with id {current_state_id} does not exist")
        
        if input_ == '':
            input_ = "empty"
        
        if input_ not in self.states[current_state_id].transition_map:
            
            print("Invalid Input", input_)
            return self.on_error_actions + self.states[current_state_id].actions, current_state_id
        
        dest_state_id = self.states[current_state_id].transition_map[input_].dest_state_id
        transition_actions = self.states[current_state_id].transition_map[input_].actions
                
        return transition_actions + self.states[dest_state_id].actions, dest_state_id

    def print_states(self):
        for state_id, state in self.states.items():
            print(f"State {state_id}")
            print("Actions:")
            for action in state.actions:
                print(action)
                
    def print_transitions(self):
        for state_id, state in self.states.items():
            # print(f"State {state_id}")
            # print("Transitions:")
            for transition in state.transition_map.values():
                print(f"From {state_id} to {transition.dest_state_id} on {transition.input}")
            
            # print("\n\n")
        print("#################")
    
    def print_state_transitions(self, state_id):
        state = self.states[state_id]
        print(f"State {state_id}")
        print("Transitions:")
        for transition in state.transition_map.values():
            print(f"From {state_id} to {transition.dest_state_id} on {transition.input}")
        print("\n\n")
    
    def visualize_fsm(self, current_state_id=None, depth=0, visited=None, parent_prefix=''):
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
        indent = "│   " * depth  # Indentation for visualization with vertical lines
        
        # Add the current state to the visualization
        tree_str += f"{parent_prefix}State ID: {current_state_id}\n"
        
        transitions = list(current_state.transition_map.items())
        for index, (input_, transition) in enumerate(transitions):
            # Check if this is the last transition to adjust the prefix accordingly
            is_last_transition = index == len(transitions) - 1
            transition_prefix = "└── " if is_last_transition else "├── "
            next_parent_prefix = parent_prefix + ("    " if is_last_transition else "│   ")
            
            tree_str += f"{parent_prefix}{transition_prefix}Transition on '{input_}' to State ID: {transition.dest_state_id}\n"
            
            # Recursively visualize the destination state with updated parent prefix for proper indentation
            tree_str += self.visualize_fsm(transition.dest_state_id, depth + 1, visited, next_parent_prefix)
        
        return tree_str



    
    