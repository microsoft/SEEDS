from typing import Any
from base_classes.base_fsm_operation import FSMOperation
from fsm.fsm import FSM
from utils.model_classes import IVRCallStateMongoDoc

class QuizInitStateOperation(FSMOperation):
    def execute(self, fsm: FSM, fsm_state_doc: IVRCallStateMongoDoc = None) -> Any:
        fsm_state_doc.experience_data['quiz'] = {
            'score': 0
        }
        
        