from actions.base_actions.stream_action import StreamAction
from base_classes.action import Action
from base_classes.base_fsm_operation import FSMOperation
from base_classes.base_process_operation_output import ProcessOperationOutput
from fsm.operations.empty_process_state_output import EmptyProcessStateOutput
from fsm.operations.empty_state_operation import EmptyStateOperation
from fsm.transition import Transition
from utils.model_classes import Menu

class State:
    def __init__(self, state_id: str, actions: [Action] = None, post_operation: FSMOperation = None, \
        pre_operation: FSMOperation = None, process_operation_output_into_actions: ProcessOperationOutput = None, \
        menu: Menu = None):
        self.id = state_id
        self.actions = actions if actions is not None else []
        self.transition_map: dict[str, Transition] = {}
        self.post_operation: FSMOperation = post_operation if post_operation is not None else EmptyStateOperation()
        self.pre_operation: FSMOperation = pre_operation if pre_operation is not None else EmptyStateOperation()
        self.process_operation_output_into_actions: ProcessOperationOutput = process_operation_output_into_actions \
            if process_operation_output_into_actions is not None else EmptyProcessStateOutput()
        self.menu = menu
        
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
            'actions': [action.to_json() for action in self.actions],
            'post_operation': self.post_operation.to_json(),
            'pre_operation': self.pre_operation.to_json(),
            'process_operation_output_into_actions': self.process_operation_output_into_actions.to_json(),
            'menu': self.menu.dict() if self.menu is not None else None
        }

    @staticmethod
    def from_json(data: dict):
        """Deserialize JSON string back into a State object."""
        state = State(state_id=data['id'])

        # Deserialize actions
        for action_json in data['actions']:
            action = Action.from_json(action_json) 
            state.actions.append(action)
        
        state.menu = Menu(**data['menu']) if 'menu' in data and data['menu'] is not None else None

        if 'post_operation' in data and \
            'pre_operation' in data and \
            'process_operation_output_into_actions' in data:
            # Deserialize operations 
            state.post_operation = FSMOperation.from_json(data['post_operation'])
            state.pre_operation = FSMOperation.from_json(data['pre_operation'])
            state.process_operation_output_into_actions = ProcessOperationOutput.from_json(data['process_operation_output_into_actions'])

        return state