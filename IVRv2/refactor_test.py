from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction
from actions.vonage_actions.vonage_action_factory import VonageActionFactory

# def __init__(self, text: str, level: float, bargeIn: bool, loop: int, language: str):
# def __init__(self, streamUrl: str, level: float, bargeIn: bool, loop: int):

# Instantiate with dummy values
action1 = TalkAction(text="Hello Roshni", bargeIn=True, loop=2, language="en-US")
action2 = StreamAction(url="https://example.com/stream", volume=0.5, bargeIn=False, loop=1)
action3 = InputAction(eventUrl="https://example.com/stream", type_=["dtmf"], maxDigits=5, timeOut=10, submitOnHash=True)

# print(action1.get())
# print(action2.get())
# print(action3.get())

va1 = VonageActionFactory().get_action_implmentation(action1)
va2 = VonageActionFactory().get_action_implmentation(action2)
va3 = VonageActionFactory().get_action_implmentation(action3)


print(VonageActionFactory().get_action_accumulator_implmentation().combine([va1, va2, va3]))




