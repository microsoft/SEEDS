from base_classes.base_fsm_operation import FSMOperation
from fsm.fsm import FSM

class EmptyStateOperation(FSMOperation):
    def execute(self, fsm: FSM):
        pass