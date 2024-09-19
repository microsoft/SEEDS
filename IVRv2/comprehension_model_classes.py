from typing import List
import uuid
from pydantic import BaseModel, Field
from fsm.state import State
from fsm.transition import Transition
from fsm.fsm import FSM
import os
from dotenv import load_dotenv
import json
import re
import requests
import aiohttp
import asyncio
import uuid
from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction

class URLTextEntity(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    url: str
    text: str

class ComprehensionOption(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    exploreOption: URLTextEntity
    exploreValue: URLTextEntity

class SubComprehension(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    passage: URLTextEntity
    options: List[ComprehensionOption]

class ComprehensionData(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    input_action: InputAction = InputAction(type_=["dtmf"], eventApi='/input')
    intro: URLTextEntity
    conclusion: URLTextEntity
    data: List[SubComprehension]

    class Config:
        arbitrary_types_allowed = True

class Comprehension:
    def __init__(self, comprehension_data: ComprehensionData):
        self.comprehension_data = comprehension_data
        self.input_action = comprehension_data.input_action

    def generate_states(self, fsm, prefix_state_id):
        initial_quiz_state_id = f"{prefix_state_id}_ComprehensionStart"
        initial_state = self.get_initial_state(initial_quiz_state_id)
        fsm.add_state(initial_state)
        fsm.set_init_state_id(initial_quiz_state_id)

        end_state = self.get_end_state()
        # print("END STATE")
        # print(end_state.id)
        # fsm.add_state(end_state)
        fsm.set_end_state(end_state)

        comprehension_state_ids = []
        for i, stage in enumerate(self.comprehension_data.data):
            comprehension_state_ids.append(self.generate_sub_comprehension_states(fsm, stage, i))

        print(comprehension_state_ids)

        for i in range(len(comprehension_state_ids)):
            if i == 0:
                fsm.add_transition(Transition(input="1", source_state_id=initial_quiz_state_id, dest_state_id=comprehension_state_ids[i]["question"]))
            if i < len(comprehension_state_ids)-1:
                print("OPTIONS", comprehension_state_ids[i]["options"])
                for option_state_id in comprehension_state_ids[i]["options"]:
                    fsm.add_transition(Transition(input="1", source_state_id=option_state_id, dest_state_id=comprehension_state_ids[i+1]["question"]))
            if i == len(comprehension_state_ids)-1:
                print("SETTING UP THE END STATE")
                for option_state_id in comprehension_state_ids[i]["options"]:
                    fsm.add_transition(Transition(input="1", source_state_id=option_state_id, dest_state_id=end_state.id))

    def generate_sub_comprehension_states(self, fsm, sub_comprehension, index):
        subcomprehension_state_ids = {}
        question_state = self.generate_sub_comprehension_question_state(sub_comprehension, index)
        fsm.add_state(question_state)

        option_state_ids = []
        for i, option in enumerate(sub_comprehension.options):
            actions = []
            actions.append(StreamAction(url=option.exploreValue.url))
            actions.append(self.input_action)
            option_state_id = f"SubComprehensionOption_SC{index}_O{i}"
            option_state = State(state_id=option_state_id, actions=actions)
            option_state_ids.append(option_state_id)
            fsm.add_state(option_state)
            fsm.add_transition(Transition(input=str(i+1), source_state_id=question_state.id, dest_state_id=option_state_id))
        subcomprehension_state_ids["question"] = question_state.id
        subcomprehension_state_ids["options"] = option_state_ids
        return subcomprehension_state_ids

    def generate_sub_comprehension_question_state(self, sub_comprehension: SubComprehension, index: int):
        actions = []
        actions.append(StreamAction(url=sub_comprehension.passage.url))
        # for i, option in enumerate(sub_comprehension.options):
        #     actions.append(TalkAction(text=f"Press {i + 1}"))
        #     actions.append(TalkAction(text="for " + option.exploreOption.text))
        actions.append(self.input_action)

        question_state = State(state_id="SubComprehensionQuestion_" + str(index), actions=actions)
        return question_state



        # fsm.set_end_state(State(state_id="END", actions=[TalkAction(text="You didn't choose a valid option. Bye bye.", bargeIn=False)], menu=menu))
    def get_end_state(self):
        actions = []
        actions.append(StreamAction(url=self.comprehension_data.conclusion.url, bargeIn=False))
        end_state = State(state_id="ComprehensionEnd", actions=actions)
        return end_state


    def get_initial_state(self, initial_quiz_state_id):
        """
        Returns the initial state of the comprehension.
        """
        actions = []
        actions.append(StreamAction(url=self.comprehension_data.intro.url))
        # actions.append(TalkAction(text=f"Welcome to the Water Cycle Adventure! You are about to embark on a journey as a water droplet. Along the way, you’ll make choices to learn how water moves through nature and how we can conserve it."))
        # actions.append(TalkAction(text=f"Press 1 to get started."))
        actions.append(self.input_action)
        
        initial_state = State(state_id=initial_quiz_state_id, actions=actions)
        
        return initial_state


sub_comprehension_evaporation = SubComprehension(
    passage=URLTextEntity(
        url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Evaporation%20-%20intro%20123%20optionsFINAL.mp3',
        text="Imagine it’s a hot day, and the sun is shining. Water can evaporate from different places. Where do you want to start evaporating from?"
    ),
    options=[
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/evaporation/lake",
                text="Start from a lake."
            ),
            exploreValue=URLTextEntity(
                url='https://seedsblob.blob.core.windows.net/output-container/comprehension/Evaporation%20choice%201%20-%20lake.mp3',
                text="You chose to start evaporating from a lake! As the sun shines, the water from the lake turns into vapor and rises up into the air. Did you know that large water bodies like lakes and rivers lose a lot of water this way? Let's see what happens next. Press any key to continue!"
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/evaporation/puddle",
                text="Start from a puddle on the road."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Evaporation%20choice%202%20-%20puddle.mp3',
                text="You chose to start evaporating from a puddle! After a rainy day, water on the road can evaporate quickly when the sun comes out. But do you know? Rainwater from the road can be collected and used for things like watering plants! Press any key to continue!"
            )
        ),
    ]
)

sub_comprehension_condensation = SubComprehension(
    passage=URLTextEntity(
        url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Condesation%20-%20intro%20123%20options.mp3',
        text="Now you’re high up in the sky, and it’s getting cooler. The water vapor turns into tiny droplets and forms clouds. This process is called condensation. Let’s continue your journey as a droplet in a cloud. But where do you want to go next?"
    ),
    options=[
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/crops",
                text="Join a big dark cloud over a field of crops."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Condensation%20-%20Choice%201%20-%20field%20of%20crops.mp3',
                text="You joined a big dark cloud over a field of crops. Farmers depend on rain from clouds like this to water their crops. But in some places, people use too much water for farming, and it runs out quickly. That’s why conserving water is so important! Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/city",
                text="Join a small white cloud over a city."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Condensation%20-%20Choice%202%20-%20City.mp3',
                text="You joined a small white cloud over a city. In cities, people often use a lot of water in homes, schools, and offices. Collecting rainwater in the city can help save water. Let’s move on to see what happens next. Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/park",
                text="Join a fluffy cloud over a park."
            ),
            exploreValue=URLTextEntity(
                url='https://seedsblob.blob.core.windows.net/output-container/comprehension/Condensation%20-%20Choice%203%20-%20Park.mp3',
                text="You joined a fluffy cloud over a park. Parks need water to keep their grass and plants green. But using sprinklers all day wastes a lot of water. It's better to water plants early in the morning or late at night. Press any key to continue."
            )
        )
    ]
)

sub_comprehension_precipitation = SubComprehension(
    passage=URLTextEntity(
        url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Precipitation%20-%20intro%20123%20options.mp3',
        text="Now you’re high up in the sky, and it’s getting cooler. The water vapor turns into tiny droplets and forms clouds. This process is called condensation. Let’s continue your journey as a droplet in a cloud. But where do you want to go next?"
    ),
    options=[
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/crops",
                text="Join a big dark cloud over a field of crops."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Precipitation%20-%20Choice%201%20-%20rain%20over%20garden.mp3',
                text="You joined a big dark cloud over a field of crops. Farmers depend on rain from clouds like this to water their crops. But in some places, people use too much water for farming, and it runs out quickly. That’s why conserving water is so important! Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/city",
                text="Join a small white cloud over a city."
            ),
            exploreValue=URLTextEntity(
                url=  'https://seedsblob.blob.core.windows.net/output-container/comprehension/Precipitation%20-%20Choice%202%20-%20Snow%20in%20mountains.mp3',
                text="You joined a small white cloud over a city. In cities, people often use a lot of water in homes, schools, and offices. Collecting rainwater in the city can help save water. Let’s move on to see what happens next. Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/park",
                text="Join a fluffy cloud over a park."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Precipitation%20-%20choice%203%20-%20rain%20over%20playground.mp3',
                text="You joined a fluffy cloud over a park. Parks need water to keep their grass and plants green. But using sprinklers all day wastes a lot of water. It's better to water plants early in the morning or late at night. Press any key to continue."
            )
        )
    ]
)

sub_comprehension_collection = SubComprehension(
    passage=URLTextEntity(
        url=  'https://seedsblob.blob.core.windows.net/output-container/comprehension/Collection%20-%20intro%20123%20options.mp3',
        text="Now you’re high up in the sky, and it’s getting cooler. The water vapor turns into tiny droplets and forms clouds. This process is called condensation. Let’s continue your journey as a droplet in a cloud. But where do you want to go next?"
    ),
    options=[
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/crops",
                text="Join a big dark cloud over a field of crops."
            ),
            exploreValue=URLTextEntity(
                url='https://seedsblob.blob.core.windows.net/output-container/comprehension/Collection%20-%20choice%201%20-%20cleaning%20the%20house.mp3',
                text="You joined a big dark cloud over a field of crops. Farmers depend on rain from clouds like this to water their crops. But in some places, people use too much water for farming, and it runs out quickly. That’s why conserving water is so important! Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/city",
                text="Join a small white cloud over a city."
            ),
            exploreValue=URLTextEntity(
                url= 'https://seedsblob.blob.core.windows.net/output-container/comprehension/Collection%20-%20choice%202%20-%20storing%20the%20water.mp3',
                text="You joined a small white cloud over a city. In cities, people often use a lot of water in homes, schools, and offices. Collecting rainwater in the city can help save water. Let’s move on to see what happens next. Press any key to continue."
            )
        ),
        ComprehensionOption(
            exploreOption=URLTextEntity(
                url="https://example.com/watercycle/condensation/park",
                text="Join a fluffy cloud over a park."
            ),
            exploreValue=URLTextEntity(
                url=  'https://seedsblob.blob.core.windows.net/output-container/comprehension/Collection%20-%20choice%203%20-%20wasting%20the%20water.mp3',
                text="You joined a fluffy cloud over a park. Parks need water to keep their grass and plants green. But using sprinklers all day wastes a lot of water. It's better to water plants early in the morning or late at night. Press any key to continue."
            )
        )
    ]
)

WaterCycleComprehension = ComprehensionData(
    intro=URLTextEntity(
        url="https://seedsblob.blob.core.windows.net/output-container/comprehension/Intro%20to%20adventure.mp3",
        text="Welcome to the Water Cycle Adventure! You are a drop of water on a journey through nature. Along the way, you’ll make choices to learn how water moves through nature and how we can conserve it. Press 1 to get started."
    ),
    conclusion=URLTextEntity(
        url='https://seedsblob.blob.core.windows.net/output-container/comprehension/Conclusion.mp3',
        text="Congratulations! You have completed the Water Cycle Adventure. You have learned how water moves through nature and how we can conserve it. Bye Bye."),
    data=[sub_comprehension_evaporation, sub_comprehension_condensation, sub_comprehension_precipitation, sub_comprehension_collection]
)

comprehension = Comprehension(WaterCycleComprehension)
fsm = FSM(fsm_id=str(uuid.uuid4()))
comprehension.generate_states(fsm, "WaterCycle")

visualised = fsm.visualize_fsm()
print(visualised)

# fsm.print_states()
