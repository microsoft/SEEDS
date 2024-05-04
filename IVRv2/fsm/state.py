from actions.base_actions.stream_action import StreamAction
from base_classes.action import Action
from base_classes.base_fsm_operation import FSMOperation
from fsm.operations.empty_process_state_output import EmptyProcessStateOutput
from fsm.operations.empty_state_operation import EmptyStateOperation
from fsm.transition import Transition

class State:
    def __init__(self, state_id: str, actions: [Action] = None):
        self.id = state_id
        self.actions = actions if actions is not None else []
        self.transition_map: dict[str, Transition] = {}
        self.post_operation: FSMOperation = EmptyStateOperation()
        self.pre_operation: FSMOperation = EmptyStateOperation()
        self.process_operation_output_into_actions =  EmptyProcessStateOutput()
        
    def add_transition(self, transition: Transition):
        if transition.input in self.transition_map:
            raise ValueError(f"Transition for input {transition.input} already exists")
        self.transition_map[transition.input] = transition
    
    def get_stream_action_with_record_playback_option(self):
        return [
            action
            for action in self.actions
            if isinstance(action, StreamAction) and action.record_playback_time
        ]
                
    def serialize_transitions(self):
        """Serialize the Transition objects to a list of dictionaries"""
        return [ transition_obj.to_json() for transition_obj in self.transition_map.values() ]
    
    def serialize(self):
        """Serialize the State object to a dictionary (without transitions)"""
        return {
            'id': self.id,
            'actions': [action.to_json() for action in self.actions]
        }

    @staticmethod
    def from_json(data: dict):
        """Deserialize JSON string back into a State object."""
        state = State(state_id=data['id'])

        # Deserialize actions
        for action_json in data['actions']:
            action = Action.from_json(action_json) 
            state.actions.append(action)

        return state