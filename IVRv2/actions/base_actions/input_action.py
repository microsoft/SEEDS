from base_classes.action import Action

class InputAction(Action):
    """You can use the input action to collect digits or speech input by the person you are calling."""
    def __init__(self, type_: [str], eventApi: str, **kwargs):
        self.type = type_
        self.eventApi = eventApi
        self.extra_args = kwargs
        
    def get(self, sas_gen_obj):
        raise NotImplementedError("Get() Function called on Base Action `InputAction`")
    
    def __str__(self):
        return f"InputAction: {self.type} {self.eventApi} {self.extra_args}"