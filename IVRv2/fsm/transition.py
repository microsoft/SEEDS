from base_classes.action import Action

class Transition:
    def __init__(self, input: str, source_state_id: str, dest_state_id: str, actions: [Action] = []):
        self.input = input
        self.source_state_id = source_state_id
        self.dest_state_id = dest_state_id
        self.actions = actions if actions is not None else []
    
    def to_json(self):
        """Serialize the Transition object to a dictionary"""
        return {
            'input': self.input,
            'source_state_id': self.source_state_id,
            'dest_state_id': self.dest_state_id,
            'actions': [action.to_json() for action in self.actions]  # Serialize each action individually
        }
        
    def __str__(self):
        return f"Transition: {self.source_state_id} to {self.dest_state_id} on key {self.input}"

    @staticmethod
    def from_json(data: dict):
        """Deserialize dictionary back into a Transition object."""
        actions = [Action.from_json(action_json) for action_json in data['actions']]
        return Transition(
            input=data['input'],
            source_state_id=data['source_state_id'],
            dest_state_id=data['dest_state_id'],
            actions=actions
        )