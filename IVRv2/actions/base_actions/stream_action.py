from base_classes.action import Action

class StreamAction(Action):
    """The stream action allows you to send an audio stream to a Conversation"""
    def __init__(self, url: str, **kwargs):
        self.url = url
        self.extra_args = kwargs
        
    def get(self):
        raise NotImplementedError("Get() Function called on Base Action `StreamAction`")