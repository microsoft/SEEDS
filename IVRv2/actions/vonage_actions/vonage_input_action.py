from actions.base_actions.input_action import InputAction

class VonageInputAction(InputAction):
    """This class is designed to handle Vonage Input Actions.

    The VonageInputAction allows for 

    Attributes:
        
    """
    def __init__(self, type_: [str], maxDigits: int, eventUrl: str, timeOut: int, submitOnHash: bool):
        """Initializes the VonageInputAction with the provided audio settings.

        Args:
            
        """
        self.type = type_
        self.maxDigits = maxDigits
        self.eventUrl = eventUrl
        self.submitOnHash = submitOnHash
        self.timeOut = timeOut
        
    def get(self):
        
        action =  {
            "type": self.type,
            "action": "input",
            "eventUrl": [self.eventUrl]
        }
        if "dtmf" in self.type:
            action["dtmf"] = {
                "maxDigits": self.maxDigits,
                "submitOnHash": self.submitOnHash,
                "timeOut": self.timeOut
            }
        return action
        
        
