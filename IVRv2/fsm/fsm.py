from datetime import datetime
from typing import List, Tuple
from actions.base_actions.stream_action import StreamAction
from fsm.state import State
from fsm.transition import Transition
from base_classes.action import Action
from actions.base_actions.talk_action import TalkAction
from utils.model_classes import IVRCallStateMongoDoc, IVRfsmDoc

class FSM:
    NO_OPTION_CHOSEN_AUDIO_URL = "https://seedsblob.blob.core.windows.net/pull-model-menus/chosenNoOptionDialog/kannada/Sorry,%20you%20have%20not%20chosen%20any%20option/1.0.mp3"
    WRONG_INPUT_AUDIO_URL = "https://seedsblob.blob.core.windows.net/pull-model-menus/chosenWrongOptionDialog/kannada/Sorry,%20you%20have%20chosen%20the%20wrong%20option/1.0.mp3"
    
    def __init__(self, fsm_id: str):
        """
        Initializes a new instance of the FSM class.

        Args:
        fsm_id (str): A unique identifier for the FSM instance.

        Attributes:
        states (dict[str, State]): A dictionary mapping state IDs to State objects.
        init_state_id (str): The initial state ID of the FSM.
        invalid_input_error_actions (list[Action]): Actions to perform when an invalid input is received.
        empty_input_error_actions (list[Action]): Actions to perform when no input is received.
        end_state (State): A predefined end state with a termination action.
        """
        self.fsm_id = fsm_id
        self.states: dict[str, State] = {}
        self.init_state_id = "LA0"
        self.invalid_input_error_actions = [StreamAction(self.WRONG_INPUT_AUDIO_URL)]
        self.empty_input_error_actions = [StreamAction(self.NO_OPTION_CHOSEN_AUDIO_URL)]
        # self.end_state = State(state_id="END", actions=[TalkAction("Bye bye")])
        # self.add_state(self.end_state)
    
    def serialize(self) -> IVRfsmDoc:
        states = [state.serialize() for state in self.states.values()]
        transitions = []
        for state in self.states.values():
            print(state.serialize_transitions(), type(state.serialize_transitions()))
            transitions.extend(state.serialize_transitions())
        
        return IVRfsmDoc(
            id=self.fsm_id,
            init_state_id=self.init_state_id,
            states=states,
            transitions=transitions,
            created_at=int(datetime.now().timestamp())
        )
    
    @staticmethod
    def deserialize(data: IVRfsmDoc):
        fsm = FSM(data.id)
        for state_json in data.states:
            state_obj = State.from_json(state_json)
            fsm.add_state(state_obj)
        fsm.set_init_state_id(data.init_state_id)
        
        for transition_json in data.transitions:
            transition_obj = Transition.from_json(transition_json)
            fsm.add_transition(transition_obj)
        
        return fsm

    def get_state(self, state_id: str):
        if state_id in self.states:
            return self.states[state_id]
        return None
        
    def set_end_state(self, state: State):
        self.end_state = state
        self.add_state(state)
    
    def add_state(self, state: State):
        """
        Adds a state to the FSM if it does not already exist.

        Args:
        state (State): The state to be added to the FSM.

        Raises:
        ValueError: If a state with the same ID already exists in the FSM.
        """
        if state.id in self.states:
            raise ValueError(f"State with id {state.id} already exists")
        self.states[state.id] = state
    
    def set_init_state_id(self, state_id: str):
        """
        Sets the initial state of the FSM.

        Args:
        state_id (str): The ID of the state to set as initial.

        Raises:
        ValueError: If the specified state ID does not exist in the FSM.
        """
        if state_id not in self.states:
            raise ValueError(f"Cannot set initial state to {state_id}, as it does not exist")
        self.init_state_id = state_id
    
    def add_transition(self, transition: Transition):
        """
        Adds a transition to the FSM between states.

        Args:
        transition (Transition): The transition to be added.

        Raises:
        ValueError: If the source or destination state does not exist.
        """
        if transition.source_state_id not in self.states or transition.dest_state_id not in self.states:
            raise ValueError(f"Cannot add Transition for State with id {transition.source_state_id}, as it does not exist")
        self.states[transition.source_state_id].add_transition(transition)
        
    def get_start_fsm_actions(self) -> List[Action]:
        """
        Retrieves the actions associated with the initial state of the FSM.

        Returns:
        List[Action]: A list of actions associated with the initial state.

        Raises:
        ValueError: If the initial state does not exist.
        """
        if self.init_state_id not in self.states:
            raise ValueError(f"Initial State with id {self.init_state_id} does not exist")
        return self.states[self.init_state_id].actions
    
    async def get_next_actions(self, input_: str, ivr_state_doc: IVRCallStateMongoDoc) -> Tuple[List[Action], str]:
        """
        Determines the next actions and state based on the input and current state.

        Args:
        input_ (str): The input provided to the FSM.
        current_state_id (str): The ID of the current state.

        Returns:
        Tuple[List[Action], str]: A tuple containing the list of actions to be performed next and the next state ID.

        Raises:
        ValueError: If the current state does not exist.
        """
        print("Input", input_)
        current_state_id = ivr_state_doc.current_state_id
        print("Current State", current_state_id)
        self.print_state_transitions(current_state_id)
        
        if current_state_id not in self.states:
            raise ValueError(f"Current State with id {current_state_id} does not exist")
        
        current_state = self.states[current_state_id]
        if input_ == '':
            input_ = "empty"
        
        if input_ not in current_state.transition_map:
            # SEND APPROPRIATE ERROR MESSAGE WITH CURRENT STATE ACTIONS
            print("Invalid Input", input_)
            error_actions = []
            if input_ == "empty":
                error_actions = self.empty_input_error_actions
            else:
                error_actions = self.invalid_input_error_actions
            return error_actions + current_state.actions, current_state_id
        
        await current_state.post_operation.execute(self, ivr_state_doc)
        dest_state_id = current_state.transition_map[input_].dest_state_id
        
        dest_state = self.states[dest_state_id]
        await dest_state.pre_operation.execute(self, ivr_state_doc)
        
        transition_actions = current_state.transition_map[input_].actions        
        return transition_actions + dest_state.actions, dest_state_id

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
        """
        Visualizes the structure of the FSM starting from the initial or specified state.

        Args:
        current_state_id (str, optional): The state ID from which to start visualization. Defaults to initial state if not specified.
        depth (int): The current depth in the recursive call stack, used for indentation.
        visited (set, optional): A set to keep track of visited states to avoid infinite loops.
        parent_prefix (str): A string prefix used to format the tree structure visually.

        Returns:
        str: A string representation of the FSM structure.
        """
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



    
    