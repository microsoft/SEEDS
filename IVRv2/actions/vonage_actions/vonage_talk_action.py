from actions.base_actions.talk_action import TalkAction

class VonageTalkAction(TalkAction):
    default_bargeIn = True
    default_level = 1
    default_loop = 1
    default_language = "en-US"
    
    """This class is designed to handle Vonage Talk Actions.

    The VonageTalkAction  action sends synthesized speech to a Conversation.

    The text provided in the talk action can either be plain, or formatted 
    using SSML. SSML tags provide further instructions to the text-to-speech 
    synthesizer which allow you to set pitch, pronunciation and to combine 
    together text in multiple languages. SSML tags are XML-based and sent
    inline in the JSON string.

    Attributes:
        text (str): An URL to an mp3 or wav (16-bit) audio file to stream.
            This is a required parameter.
        level (float, optional): Sets the audio level of the stream in the range
            -1 >= level <= 1 with a precision of 0.1. Defaults to 0.
        bargeIn (bool, optional): If set to True, the action is terminated when
            the user interacts with the application either with DTMF or ASR voice input.
            Defaults to False.
        loop (int, optional): Specifies the number of times the audio is repeated
            before the Call is closed. Set to 0 to loop infinitely. Defaults to 1.
        language (str, optional): The language of the text to be spoken. Defaults to "en-US".
        style (str, optional): The vocal style (vocal range, tessitura and timbre). Default: 0.
    """

    def __init__(self, text: str, level: float, bargeIn: bool, loop: int, language: str):
        """Initializes the VonageTalkAction with the provided audio settings.

        Args:
            text (str): The text to be spoken by the TTS engine.
            level (float, optional): The audio level of the stream. Defaults to 0.
            bargeIn (bool, optional): Enables termination on user interaction. Defaults to False.
            loop (int, optional): The loop count for the audio stream. Defaults to 1.
            language (str, optional): The language of the text to be spoken. Defaults to "en-US".
            style (str, optional): The vocal style (vocal range, tessitura and timbre). Default: 0.
        """
        self.text = text
        self.level = level
        self.bargeIn = bargeIn
        self.loop = loop
        self.language = language
       
        
    
    def get(self):
        return {
            'action': "talk",
            'text': self.text,
            'loop': self.loop,
            'bargeIn': self.bargeIn,
            'level': self.level,
            'language': self.language
        }
