from base_classes.action import Action

class TalkAction(Action):
    """The talk action sends synthesized speech to a Conversation."""
    def __init__(self, text: str, **kwargs):
        self.text = text
        self.extra_args = kwargs
        
    def get(self):
        raise NotImplementedError("Get() Function called on Base Action `TalkAction`")
    
    def __str__(self):
        return f"TalkAction: {self.text} {self.extra_args}"