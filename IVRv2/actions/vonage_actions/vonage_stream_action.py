from actions.base_actions.stream_action import StreamAction
from utils.sas_gen import SASGen
from dotenv import load_dotenv
import os

load_dotenv()

class VonageStreamAction(StreamAction):
    default_level = 1
    default_bargeIn = True
    default_loop = 1
    """This class is designed to handle Vonage Stream Actions.

    The VonageStreamAction allows for streaming audio files directly to a call
    or conversation, with options to set the audio level, enable barge-in behavior,
    and control the loop count of the audio stream.

    Attributes:
        streamUrl (str): An URL to an mp3 or wav (16-bit) audio file to stream.
            This is a required parameter.
        level (float, optional): Sets the audio level of the stream in the range
            -1 >= level <= 1 with a precision of 0.1. Defaults to 0.
        bargeIn (bool, optional): If set to True, the action is terminated when
            the user interacts with the application either with DTMF or ASR voice input.
            Defaults to False.
        loop (int, optional): Specifies the number of times the audio is repeated
            before the Call is closed. Set to 0 to loop infinitely. Defaults to 1.
    """

    def __init__(self, streamUrl: str, level: float, bargeIn: bool, loop: int):
        """Initializes the VonageStreamAction with the provided audio settings.

        Args:
            streamUrl (str): The URL to an mp3 or wav (16-bit) audio file to stream.
            level (float, optional): The audio level of the stream. Defaults to 0.
            bargeIn (bool, optional): Enables termination on user interaction. Defaults to False.
            loop (int, optional): The loop count for the audio stream. Defaults to 1.
        """
        self.streamUrl = streamUrl
        self.level = level
        self.bargeIn = bargeIn
        self.loop = loop
    
    def get(self):
        sas_gen_obj = SASGen(os.getenv("BLOB_STORE_CONN_STR"))
        return {
            'action': "stream",
            'streamUrl': [sas_gen_obj.get_url_with_sas(self.streamUrl)],
            'loop': self.loop,
            'bargeIn': self.bargeIn,
            'level': self.level
        }
