import uuid
from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction

from fsm.state import State
from fsm.transition import Transition
from fsm.fsm import FSM
from fsm.quiz import Quiz
import os
from dotenv import load_dotenv
import json
import re
import requests
import aiohttp
import asyncio
from typing import List, Optional

from utils.model_classes import IVRfsmDoc
from utils.sas_gen import SASGen
from utils.model_classes import Menu
from utils.model_classes import Option
from utils.quiz_model_classes import QuizData
from utils.quiz_model_classes import QuizQuestion
from utils.quiz_model_classes import URLTextEntity

load_dotenv()

# pullMenuMainUrl = ""
pullMenuMainUrl = "https://seedsblob.blob.core.windows.net/pull-model-menus/"
content_url = "https://seedsblob.blob.core.windows.net/output-container/"
url = 'https://seeds-teacherapp.azurewebsites.net/content'
headers = {
    'authToken': 'postman'
}

repeat_current_categories_key = "8"
previous_category_level_key = "9"

speechRate = "1.0"
language = "kannada"
audioGoingTobePlayedDialogUrl = 'audioDialogs/{language}/audioGoingToBePlayedDialog/{speechRate}.mp3'
audioFinishedMessageUrl = 'audioDialogs/{language}/audioFinishedDialog/{speechRate}.mp3' #includes 'to repeat, press 8 and to go back press 9'

pressKeyMessageUrl = 'pressKeysDialog/{language}/{key}/{speechRate}.mp3'

quiz_new = {
  "id": "e3d1db09-f5fd-44b2-8244-86ea61619175",
  "language": "kannada",
  "theme": "water",
  "themeAudio": "https://seedsblob.blob.core.windows.net/theme-titles/Water/kannada",
  "title": "Punyakoti",
  "titleAudio": "https://seedsblob.blob.core.windows.net/experience-titles/quiz/12f77743-4255-48a8-855b-2f4d7b635c95/1.0.mp3",
  "localTitle": "ಮೊಲದ ಮರಿ",
  "positiveMarks": 1,
  "negativeMarks": 0,
  "type": "quiz",
  "questions": [
    {
      "question": {
        "id": "f8210aca-d2e0-445e-bb92-9e673bb92158",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_1/1.0.mp3",
        "text": "ಪುಣ್ಯಕೋಟಿಗೂ ತನ್ನ ಕರುವಿಗೂ ಏನು ಸಂಬಂಧ? "
      },
      "options": [
        {
          "id": "3ef0ae75-9f9e-4d9a-8bfa-ac67297d15b0",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_1/1/1.0.mp3",
          "text": " ತಾಯಿ"
        },
        {
          "id": "614c161e-a0b0-426f-913c-fc8adba39f8b",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_1/2/1.0.mp3",
          "text": " ಒಡಹುಟ್ಟಿದವರು"
        },
        {
          "id": "f34e920d-78a5-4230-9bd0-d603acc941d5",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_1/3/1.0.mp3",
          "text": " ಸ್ನೇಹಿತ"
        },
        {
          "id": "e8ba0f72-bf17-47b2-ba4b-6bd53615604a",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_1/4/1.0.mp3",
          "text": " ಚಿಕ್ಕಮ್ಮ "
        }
      ],
      "correct_option_id": "3ef0ae75-9f9e-4d9a-8bfa-ac67297d15b0"
    },
    {
      "question": {
        "id": "973d0720-ada8-4193-9708-6c0191c47971",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_2/1.0.mp3",
        "text": "ಪುಣ್ಯಕೋಟಿ ತನ್ನ ಸ್ನೇಹಿತರೊಂದಿಗೆ ಹುಲ್ಲು ತಿನ್ನುತ್ತಿರುವಾಗ ಯಾವ ಪ್ರಾಣಿ ತಡೆಯುತ್ತದೆ? "
      },
      "options": [
        {
          "id": "a66288f0-f09d-4510-b747-26660b6dac8d",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_2/1/1.0.mp3",
          "text": " ಹುಲಿ "
        },
        {
          "id": "d79f2970-e8a6-4b19-95a4-15c49db89886",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_2/2/1.0.mp3",
          "text": " ಸಿಂಹ"
        },
        {
          "id": "1e4756b0-684b-48d0-9168-b544b3ad2396",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_2/3/1.0.mp3",
          "text": " ಬೇಟೆಗಾರ"
        },
        {
          "id": "2f1a696e-7857-43fe-973f-50b599224946",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_2/4/1.0.mp3",
          "text": " ತೋಳ"
        }
      ],
      "correct_option_id": "a66288f0-f09d-4510-b747-26660b6dac8d"
    },
   
  ]
}

def getKeyPressUrl(key, language, speechRate):
    replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    replaced_url = re.sub(r'\{key\}', key, replaced_url)
    return pullMenuMainUrl + replaced_url


async def get_content_by_ids(content_ids: List[str]):
    api_url = os.environ.get("SEEDS_SERVER_BASE_URL", "") + "content"
    headers = {
        'authToken': 'postman'
    }
    params = [('ids[]', content_id) for content_id in content_ids]
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(api_url, headers=headers, params=params) as response:
                response.raise_for_status()  # Raise an exception for HTTP errors
                response_data = await response.text()  # get response text
                contents = json.loads(response_data)  # parse text to JSON
                contents.append(quiz_new)
                return contents
    except aiohttp.ClientError as e:
        print(f"Client error: {e}")
        return {"error": "Client error occurred while fetching content by IDs."}
    except json.JSONDecodeError as e:
        print(f"JSON decode error: {e}")
        return {"error": "Failed to decode JSON response."}
    except Exception as e:
        print(f"Unexpected error: {e}")
        return {"error": "An unexpected error occurred."}
    
    
async def instantiate_from_content_ids(content_ids: List[str]):
    content = await get_content_by_ids(content_ids)
    if "error" in content:
        return content
    elif not content:
        return {"error": "No valid content sent"}
    else:
        fsm = FSM(fsm_id=str(uuid.uuid4()))
        menu = Menu(description="Bye Bye (call cuts)", options=[], level=3)
        fsm.set_end_state(State(state_id="END", actions=[TalkAction(text="You didn't choose a valid option. Bye bye.", bargeIn=False)], menu=menu))
        
        init_state_id = "START"
        actions = []
        actions.append(StreamAction('https://seedsblob.blob.core.windows.net/pull-model-menus/welcomeDialog/kannada/welcome%20to%20SEEDS/1.0.mp3'))
        key_to_value_mapping = dict()
        
        for i in range(len(content)):
            key = str(i+1)
            current_content = content[i]
            titleAudio = current_content['titleAudio']
            if current_content['type'] != 'quiz':
                titleAudio = titleAudio + f'/{speechRate}.mp3'
            actions.append(StreamAction(titleAudio))
            actions.append(StreamAction(getKeyPressUrl(key, language, speechRate)))
            key_to_value_mapping[int(key)] = current_content['title']
            
        actions.append(InputAction(type_=["dtmf"], eventApi='/input'))
        options = [Option(key=key, value=value) for key, value in key_to_value_mapping.items()]
        description = "Welcome State"
        menu = Menu(description=description, options=options if options else None, level=0)
    
        fsm.add_state(State(state_id=init_state_id, actions=actions, menu=menu))
        fsm.init_state_id = init_state_id
            
        
        state_id_final_state = f"Audio Finished"
        actions_final = []
        audioFinishedUrl = audioFinishedMessageUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        actions_final.append(StreamAction(pullMenuMainUrl + audioFinishedUrl))
        actions_final.append(InputAction(type_=["dtmf"], eventApi='/input'))
        options = []
        exit_option = Option(key=9, value='start')
        next_state = Option(key=0, value='exit')
        options = [exit_option, next_state]
        menu = Menu(description= "Audio Finished", options = options, level=2)
        
        fsm.add_state(State(state_id=state_id_final_state, actions=actions_final, menu=menu))
        
        fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=fsm.end_state.id, input="empty", actions=[]))
        fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=init_state_id, input=previous_category_level_key, actions=[]))
        
        for i in range(len(content)):
            key_for_option_chosen = i+1
            current_content = content[i]
            if current_content['type'] != 'quiz':
                content_actions = []
                audioGoingTobePlayedUrl = audioGoingTobePlayedDialogUrl.replace('{language}', language).replace('{speechRate}', speechRate)
                content_actions.append(StreamAction(pullMenuMainUrl + audioGoingTobePlayedUrl))
                music_url = content_url + current_content['id'] + '/1.0.wav'
                content_actions.append(StreamAction(music_url, record_playback_time=True))
                audioFinishedUrl = audioFinishedMessageUrl.replace('{language}', language).replace('{speechRate}', speechRate)
                content_actions.append(StreamAction(pullMenuMainUrl + audioFinishedUrl))
                content_actions.append(InputAction(type_=["dtmf"], eventApi='/input', timeOut=3))

                state_id = current_content['title'] + " Audio Playing" # to remove '-' at the end
                # print("STATE ID", state_id)
                        # print("STATE ID", state_id)
                options = []
                repeat_option = Option(key=8, value='repeat')
                exit_option = Option(key=9, value='exit')
                next_state = Option(key=0, value='next (instructions to exit)')
                options = [repeat_option, exit_option, next_state]
                menu = Menu(description= current_content['title'] + " Audio Playing", options = options, level=1)

                fsm.add_state(State(state_id=state_id, actions=content_actions, menu=menu))
                
                fsm.add_transition(Transition(source_state_id=init_state_id, dest_state_id=state_id, input=str(key_for_option_chosen), actions=[]))
                fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=init_state_id, input=previous_category_level_key, actions=[]))
                fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=state_id, input=repeat_current_categories_key, actions=[]))
                fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=state_id_final_state, input="empty", actions=[]))
                # fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=state_id, input=repeat_current_categories_key, actions=[]))
            else:
                quiz_data = QuizData(**current_content)
                quiz = Quiz(quiz_data)
                quiz.generate_states(fsm=fsm, prefix_state_id=quiz_data.title, parent_block_state_id=init_state_id, key_chosen=key_for_option_chosen, level = 1) 
        return fsm


# async def main():
#     content_ids = ["45", "20"]
#     fsm = await instantiate_from_latest_content(content_ids)
    
#     # if fsm.get("error"):
#     #     print(f"Error: {fsm['error']}")
#     # else:
#     print("FSM instantiated successfully!")
#     # You can add more logic here to interact with the FSM or further process it
#     print(fsm.visualize_fsm())
#     # print(f"FSM ID: {fsm.id}")

# # Run the main function
# if __name__ == "__main__":
#     asyncio.run(main())