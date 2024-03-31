import os
from actions.base_actions.input_action import InputAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.talk_action import TalkAction
from fsm.fsm import FSM
from fsm.state import State
from fsm.transition import Transition
from dotenv import load_dotenv

load_dotenv()


fsm = FSM(fsm_id="dummy_fsm")
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