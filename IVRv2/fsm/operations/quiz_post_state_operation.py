from typing import Any
from base_classes.base_fsm_operation import FSMOperation
from fsm.fsm import FSM
from utils.model_classes import IVRCallStateMongoDoc

class QuizPostStateOperation(FSMOperation):
    def __init__(self, score: int):
        self.score = score
        
    def execute(self, fsm: FSM, fsm_state_doc: IVRCallStateMongoDoc = None) -> Any:
        current_score = fsm_state_doc.experience_data['quiz']['score']
        updated_score = current_score + self.score
        fsm_state_doc.experience_data['quiz']['score'] = updated_score
        
        