from base_classes.action import Action
from base_classes.base_process_operation_output import ProcessOperationOutput
from utils.model_classes import IVRCallStateMongoDoc
from actions.base_actions.talk_action import TalkAction

class QuizProcessFinalStateOutput(ProcessOperationOutput):
    def execute(self, state, op_output, fsm_state_doc: IVRCallStateMongoDoc = None) -> [Action]:
        current_score = fsm_state_doc.experience_data['quiz']['score']
        text = f"Your final score is {current_score}."
        action_final_score = TalkAction(text=text)
        final_actions = [state.actions[0]] + [action_final_score] + state.actions[1:]
        state.actions = final_actions
        return state.actions