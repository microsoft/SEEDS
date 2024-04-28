from base_classes.action import Action

class StreamAction(Action):
    """The stream action allows you to send an audio stream to a Conversation"""
    def __init__(self, url: str, record_playback_time: bool = False, **kwargs):
        self.url = url
        self.record_playback_time = record_playback_time
        self.extra_args = kwargs
        
    def get(self):
        raise NotImplementedError("Get() Function called on Base Action `StreamAction`")
    
    def __str__(self):
        return f"StreamAction: {self.url} {self.extra_args}"