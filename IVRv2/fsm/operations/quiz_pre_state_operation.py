from typing import Any
from base_classes.base_fsm_operation import FSMOperation
from fsm.fsm import FSM
from utils.model_classes import IVRCallStateMongoDoc

class QuizPreStateOperation(FSMOperation):
    async def execute(self, fsm: FSM, fsm_state_doc: IVRCallStateMongoDoc = None) -> Any:
        pass