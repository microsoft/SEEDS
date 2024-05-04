from base_classes.action import Action
from base_classes.base_process_operation_output import ProcessOperationOutput
from utils.model_classes import IVRCallStateMongoDoc

class EmptyProcessStateOutput(ProcessOperationOutput):
    def execute(self, state, op_output, fsm_state_doc: IVRCallStateMongoDoc = None) -> [Action]:
        return state.actions