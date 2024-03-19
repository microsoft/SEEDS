from base_classes.action import Action

class InputAction(Action):
    """You can use the input action to collect digits or speech input by the person you are calling."""
    def __init__(self, eventMethod: str, eventUrl: str, **kwargs):
        self.eventMethod = eventMethod
        self.eventUrl = eventUrl
        self.extra_args = kwargs
        
    def get(self):
        raise NotImplementedError