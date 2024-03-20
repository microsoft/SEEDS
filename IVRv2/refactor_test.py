from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction
from actions.vonage_actions.vonage_action_factory import VonageActionFactory

from fsm.state import State
from fsm.transition import Transition
from fsm.fsm import FSM

from dotenv import load_dotenv
import os
# from vonage.voice import Ncco

load_dotenv()

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


# print(VonageActionFactory().get_action_accumulator_implmentation().combine([va1, va2, va3]))

fsm = FSM(fsm_id="fsm1")
input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')
state1 = State(state_id="1", actions = [StreamAction(url = 'https://contentmenu.blob.core.windows.net/menu/WelcomeToSeedsNinad.mp3'),
                                        TalkAction(text="Press 1 to start and press 3 to exit"),
                                        input_action])

state2 = State(state_id="2", actions = [TalkAction("For cats press 1"),
                                        TalkAction("For lion press 2"),
                                        input_action])

state3 = State(state_id="3", actions = [StreamAction(url = "https://contentmenu.blob.core.windows.net/animalsounds/cat.mp3"),
                                        TalkAction("Press 1 for main menu, press 2 for previous menu, press 3 to exit"),
                                        input_action])

state4 = State(state_id="4", actions = [StreamAction(url = "https://contentmenu.blob.core.windows.net/animalsounds/lion.mp3"),
                                        TalkAction("Press 1 for main menu, press 2 for previous menu, press 3 to exit"),
                                        input_action])

state5 = State(state_id="5", actions = [TalkAction("Thank you for calling")])

t1 = Transition(source_state_id="1", dest_state_id="2", input="1", actions=[])
t2 = Transition(source_state_id="1", dest_state_id="5", input="3", actions=[])

t3 = Transition(source_state_id="2", dest_state_id="3", input="1", actions=[TalkAction("You selected cats")])
t4 = Transition(source_state_id="2", dest_state_id="4", input="2", actions=[TalkAction("You selected lion")])

t5 = Transition(source_state_id="3", dest_state_id="1", input="1", actions=[])
t6 = Transition(source_state_id="3", dest_state_id="2", input="2", actions=[])
t7 = Transition(source_state_id="3", dest_state_id="5", input="3", actions=[])

t8 = Transition(source_state_id="4", dest_state_id="1", input="1", actions=[])
t9 = Transition(source_state_id="4", dest_state_id="2", input="2", actions=[])
t10 = Transition(source_state_id="4", dest_state_id="5", input="3", actions=[])

fsm.add_state(state1)
fsm.add_state(state2)
fsm.add_state(state3)
fsm.add_state(state4)
fsm.add_state(state5)

fsm.set_init_state_id("1")

fsm.add_transition(t1)
fsm.add_transition(t2)
fsm.add_transition(t3)
fsm.add_transition(t4)
fsm.add_transition(t5)
fsm.add_transition(t6)
fsm.add_transition(t7)
fsm.add_transition(t8)
fsm.add_transition(t9)
fsm.add_transition(t10)

print(fsm.get_start_fsm_actions()) # S1
print("PRESSING 1")
print(fsm.get_next_actions("1")) # S2
print("PRESSING 2")
print(fsm.get_next_actions("2")) # S4
print("PRESSING 2")
print(fsm.get_next_actions("2")) # S2
print("PRESSING 1")
print(fsm.get_next_actions("1")) # S3
print("PRESSING 3")
print(fsm.get_next_actions("3")) # S5




