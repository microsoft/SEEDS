from base_classes.action import Action

class Transition:
    def __init__(self, input: str, source_state_id: str, dest_state_id: str, actions: [Action] = []):
        self.input = input
        self.source_state_id = source_state_id
        self.dest_state_id = dest_state_id
        self.actions = actions