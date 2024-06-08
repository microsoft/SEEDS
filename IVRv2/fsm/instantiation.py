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

languageDialogUrls = {
  'english':'languageDialog/english/For%20English/{speechRate}.mp3',
  'kannada':'languageDialog/kannada/For%20Kannada/{speechRate}.mp3',
  'bengali':'languageDialog/bengali/For%20Bengali/{speechRate}.mp3'
}

speechRate = "1.0"

readingContentTitlesDialogUrl = {
  'story':'readingContentTitlesDialog/{language}/story/{speechRate}.mp3',
  'poem':'readingContentTitlesDialog/{language}/poetry/{speechRate}.mp3',
  'song':'readingContentTitlesDialog/{language}/music/{speechRate}.mp3',
  'snippet':'readingContentTitlesDialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'readingContentTitlesDialog/{language}/riddle/{speechRate}.mp3',
  'quiz':'readingContentTitlesDialog/{language}/quiz/{speechRate}.mp3',
  'scramble':'readingContentTitlesDialog/{language}/scramble/{speechRate}.mp3',
  'theme':'readingContentTitlesDialog/{language}/theme/{speechRate}.mp3'
}

next4MessageUrls = {
  'story':'next4Dialog/{language}/story/{speechRate}.mp3',
  'poem':'next4Dialog/{language}/poetry/{speechRate}.mp3',
  'song':'next4Dialog/{language}/music/{speechRate}.mp3',
  'scramble':'next4Dialog/{language}/scramble/{speechRate}.mp3',
  'quiz':'next4Dialog/{language}/quiz/{speechRate}.mp3',
  'snippet':'next4Dialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'next4Dialog/{language}/riddle/{speechRate}.mp3',
  'experience':'next4Dialog/{language}/experience/{speechRate}.mp3',
  'theme':'next4Dialog/{language}/theme/{speechRate}.mp3'
}

prev4MessageUrls = {
  'story':'prev4Dialog/{language}/story/{speechRate}.mp3',
  'poem':'prev4Dialog/{language}/poetry/{speechRate}.mp3',
  'song':'prev4Dialog/{language}/music/{speechRate}.mp3',
  'scramble':'prev4Dialog/{language}/scramble/{speechRate}.mp3',
  'quiz':'prev4Dialog/{language}/quiz/{speechRate}.mp3',
  'snippet':'prev4Dialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'prev4Dialog/{language}/riddle/{speechRate}.mp3',
  'experience':'prev4Dialog/{language}/experience/{speechRate}.mp3',
  'theme':'prev4Dialog/{language}/theme/{speechRate}.mp3'
}

experienceNames = {
  'english':[
    'story',
    'poem',
    'song',
    'snippet',
    'riddle'
  ]
}

experienceDialogAudioUrls = {
  'story':pullMenuMainUrl + 'experiencesDialog/{language}/story/For%20Stories/{speechRate}.mp3',
  'poem':pullMenuMainUrl + 'experiencesDialog/{language}/poetry/For%20Rhymes/{speechRate}.mp3',
  'song':pullMenuMainUrl + 'experiencesDialog/{language}/music/For%20Songs/{speechRate}.mp3',
  'keyLearning':pullMenuMainUrl + 'experiencesDialog/{language}/keyLearning/to%20learn%20phone%20keys/{speechRate}.mp3',
  'scramble':pullMenuMainUrl + 'experiencesDialog/{language}/scramble/to%20play%20Scramble%20Game/{speechRate}.mp3',
  'quiz':pullMenuMainUrl + 'experiencesDialog/{language}/quiz/to%20play%20quiz/{speechRate}.mp3',
  'snippet': pullMenuMainUrl + 'experiencesDialog/{language}/snippet/For%20Snippets/{speechRate}.mp3',
  'riddle': pullMenuMainUrl + 'experiencesDialog/{language}/riddle/For%20Riddles/{speechRate}.mp3'
}

repeatCurrentMenuUrl = 'repeatMenuDialog/{language}/To%20repeat%20Current%20Menu/{speechRate}.mp3'
repeatContentUrl = 'contentPlayingDialogs/{language}/toRepeatContent/{speechRate}.mp3'
exitContentUrl = 'contentPlayingDialogs/{language}/toExitContent/{speechRate}.mp3'
goToPreviousMenuMessageUrl = 'previousMenuDialog/{language}/To%20go%20to%20Previous%20Menu/{speechRate}.mp3'


pressKeyMessageUrl = 'pressKeysDialog/{language}/{key}/{speechRate}.mp3'

audioGoingTobePlayedDialogUrl = 'audioDialogs/{language}/audioGoingToBePlayedDialog/{speechRate}.mp3'
audioFinishedMessageUrl = 'audioDialogs/{language}/audioFinishedDialog/{speechRate}.mp3' #includes 'to repeat, press 8 and to go back press 9'


number_of_categories_listed_in_one_state = 4
next_n_categories_key = "5"
previous_n_categories_key = "7"
repeat_current_categories_key = "8"
previous_category_level_key = "9"

content_attributes = [
    # {'category': 'language', 'level': 0, 'id': 'LA'},
    # {'category': 'theme', 'level': 1, 'id': 'TH'},
    {'category': 'type', 'level': 0, 'id': 'EX'},
    {'category': 'title', 'level': 1, 'id': 'TI'}
]


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
    {
      "question": {
        "id": "fee0d467-7360-4b67-b022-2f5f2a857527",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_3/1.0.mp3",
        "text": "ಪುಣ್ಯಕೋಟಿಯು ಹುಲಿಯನ್ನು ಭೇಟಿಯಾದಾಗ ಏನು ಮಾಡಬೇಕೆಂದು ಕೇಳಿದಳು? "
      },
      "options": [
        {
          "id": "f2762f67-8ae1-4829-a802-94dc05ede9c7",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_3/1/1.0.mp3",
          "text": " ಅವಳು ಮನೆಗೆ ಹೋಗಲಿ"
        },
        {
          "id": "8e14b74e-856e-4f1c-b420-ab2cb6a64414",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_3/2/1.0.mp3",
          "text": " ಅವಳ ಜೀವವನ್ನು ಉಳಿಸಿ"
        },
        {
          "id": "35ab28b3-6af7-41ac-945b-1c0c2976b001",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_3/3/1.0.mp3",
          "text": " ಅವಳನ್ನು ಬೇಗನೆ ತಿನ್ನಿರಿ"
        },
        {
          "id": "e7a8047e-22ce-4593-be4e-acd698a59854",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_3/4/1.0.mp3",
          "text": " ಅವಳನ್ನು ಬಿಟ್ಟುಬಿಡಿ "
        }
      ],
      "correct_option_id": "f2762f67-8ae1-4829-a802-94dc05ede9c7"
    },
    {
      "question": {
        "id": "c791fe18-fb7b-4818-a067-bbd7fb176b08",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_4/1.0.mp3",
        "text": "ಪುಣ್ಯಕೋಟಿ ತನ್ನ ಕರುವನ್ನು ಏನು ಮಾಡಬೇಕೆಂದು ತನ್ನ ಸ್ನೇಹಿತರನ್ನು ಕೇಳಿದಳು? "
      },
      "options": [
        {
          "id": "642c6e83-116f-45a4-b4b4-872d623bb7e4",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_4/1/1.0.mp3",
          "text": " ಅವಳನ್ನು ನೋಡಿಕೊಳ್ಳಿ"
        },
        {
          "id": "42fee7a7-c05d-43cb-bb87-9d63fb0a7bef",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_4/2/1.0.mp3",
          "text": " ಅವಳೊಂದಿಗೆ ಆಟವಾಡಿ"
        },
        {
          "id": "90c089e9-79cd-43cd-92c5-207adcf414fc",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_4/3/1.0.mp3",
          "text": " ಅವಳಿಗೆ ಆಹಾರ ನೀಡಿ"
        },
        {
          "id": "6db0e624-0eb7-4750-99d3-5839a1b7a496",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_4/4/1.0.mp3",
          "text": " ಅವಳನ್ನು ನಿರ್ಲಕ್ಷಿಸಿ "
        }
      ],
      "correct_option_id": "642c6e83-116f-45a4-b4b4-872d623bb7e4"
    },
    {
      "question": {
        "id": "a6311ab0-0f8f-400f-bf5d-b015ba0a401c",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_5/1.0.mp3",
        "text": "ಸತ್ಯದ ಬಗ್ಗೆ ಪುಣ್ಯಕೋಟಿ ಹೇಳಿದ್ದೇನು? "
      },
      "options": [
        {
          "id": "5a72b87a-f7fe-4e19-9099-dabe763e6fc8",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_5/1/1.0.mp3",
          "text": " ಇದು ನಮ್ಮ ತಾಯಿ ಮತ್ತು ತಂದೆ"
        },
        {
          "id": "79df7b0e-6ec2-4882-913f-33f1cac8f9d9",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_5/2/1.0.mp3",
          "text": " ಇದು ಮುಖ್ಯವಲ್ಲ"
        },
        {
          "id": "0600e6e2-1055-4702-8396-8a331db133f3",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_5/3/1.0.mp3",
          "text": " ಇದು ಮೂರ್ಖರಿಗೆ"
        },
        {
          "id": "3b27625b-9b25-442a-9adf-1727dd77165d",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_5/4/1.0.mp3",
          "text": " ಇದು ಶ್ರೀಮಂತರಿಗೆ "
        }
      ],
      "correct_option_id": "5a72b87a-f7fe-4e19-9099-dabe763e6fc8"
    },
    {
      "question": {
        "id": "92e20030-83c9-43ad-9b4a-ba8561104e14",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_6/1.0.mp3",
        "text": "ಹುಲಿಯು ಪುಣ್ಯಕೋಟಿಯನ್ನು ತಿನ್ನುವ ಆಲೋಚನೆಯನ್ನು ಏಕೆ ಬದಲಾಯಿಸಿತು? "
      },
      "options": [
        {
          "id": "56c874b1-ae12-4aac-8ad5-f73db7920145",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_6/1/1.0.mp3",
          "text": " ಅವನು ಅವಳ ಪ್ರಾಮಾಣಿಕತೆಯಿಂದ ಪ್ರಭಾವಿತನಾದನು"
        },
        {
          "id": "3dd7cf7c-4968-4c9a-8d97-598be6e10e76",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_6/2/1.0.mp3",
          "text": " ಅವನು ಅವಳಿಗೆ ಹೆದರುತ್ತಿದ್ದನು"
        },
        {
          "id": "e7ca0e4e-c4f8-4276-84df-0ddea5dd6e98",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_6/3/1.0.mp3",
          "text": " ಅವನು ಇತರ ಆಹಾರವನ್ನು ಕಂಡುಕೊಂಡನು"
        },
        {
          "id": "1b775566-ffdc-4d52-8e80-6bc534985a6c",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_6/4/1.0.mp3",
          "text": " ಅವನ ಹೊಟ್ಟೆ ತುಂಬಿತ್ತು "
        }
      ],
      "correct_option_id": "56c874b1-ae12-4aac-8ad5-f73db7920145"
    },
    {
      "question": {
        "id": "fba9ecc2-23e2-42e0-ab84-b05bd6c03fa4",
        "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/question_7/1.0.mp3",
        "text": "ಪುಣ್ಯಕೋಟಿ ಮನೆಗೆ ಹಿಂದಿರುಗಿದಾಗ ಏನಾಯಿತು? "
      },
      "options": [
        {
          "id": "d94ce54d-09b5-432e-842d-e3a84ad62caa",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_7/1/1.0.mp3",
          "text": " ಅವಳು ತನ್ನ ಕರುವನ್ನು ತಬ್ಬಿಕೊಂಡಳು"
        },
        {
          "id": "aa0993af-ef17-4024-ba50-3eee8eaa0c79",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_7/2/1.0.mp3",
          "text": " ಹುಲಿ ಅವಳನ್ನು ತಿನ್ನಿತು"
        },
        {
          "id": "59a485ab-5d91-4a17-93c9-0a62a32acaab",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_7/3/1.0.mp3",
          "text": " ಅವಳು ಓಡಿಹೋದಳು"
        },
        {
          "id": "0dd73d31-9cd2-4c08-b33b-1134c0c86860",
          "url": "https://seedsblob.blob.core.windows.net/output-container/Quiz/Punyakoti/option_7/4/1.0.mp3",
          "text": " ಅವಳು ಆಕಾಶದಲ್ಲಿ ನಕ್ಷತ್ರವಾದಳು "
        }
      ],
      "correct_option_id": "d94ce54d-09b5-432e-842d-e3a84ad62caa"
    }
  ]
}

# questions = [
#     {
#         'question': {
#             'text': 'What is the capital of France?',
#             'url': 'https://example.com/question1'
#         },
#         'options': [
#             {'id': 'opt1', 'text': 'Paris', 'url': 'https://example.com/paris'},
#             {'id': 'opt2', 'text': 'Rome', 'url': 'https://example.com/rome'},
#             {'id': 'opt3', 'text': 'Berlin', 'url': 'https://example.com/berlin'},
#             {'id': 'opt4', 'text': 'Madrid', 'url': 'https://example.com/madrid'}
#         ],
#         'correct_option': 'opt1'
#     },
#     {
#         'question': {
#             'text': 'What is the capital of India?',
#             'url': 'https://example.com/question2'
#         },
#         'options': [
#             {'id': 'opt5', 'text': 'Bangalore', 'url': 'https://example.com/paris'},
#             {'id': 'opt6', 'text': 'Delhi', 'url': 'https://example.com/rome'},
#             {'id': 'opt7', 'text': 'Mumbai', 'url': 'https://example.com/berlin'},
#             {'id': 'opt8', 'text': 'Ahmedabad', 'url': 'https://example.com/madrid'}
#         ],
#         'correct_option': 'opt6'
#     }
# ]

# sas_test = 'https://seedsblob.blob.core.windows.net/output-container/1dfe33fd-7fb7-4adb-9d60-2d9ae3c44910/1.0.wav'
# sas_gen_obj = SASGen(os.getenv("BLOB_STORE_CONN_STR"))
# sas_url = sas_gen_obj.get_url_with_sas(sas_test)
# print("SAS", sas_url)

def getKeyPressUrl(key, language, speechRate):
    replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    replaced_url = re.sub(r'\{key\}', key, replaced_url)
    return pullMenuMainUrl + replaced_url
  

def getStreamActions(items_list, values_to_urls, level, state, parent_selections = {}):
    
    # print("ITEM LIST", items_list)
    
    key_to_value_mapping = dict()
    current_value = ''
    description = ''
    
    category = content_attributes[level]['category']
        
    actions = []
    
    if level == 0 and state == 0:
        actions.append(StreamAction('https://seedsblob.blob.core.windows.net/pull-model-menus/welcomeDialog/kannada/welcome%20to%20SEEDS/1.0.mp3'))
        
    complete_url = ''
    language = ''
    if 'language' in parent_selections:
        language = parent_selections['language']
    else:
        language = 'kannada'
    
    print("LANGAUGE CHECK", language)
    
    # Handle initial actions for specific states and categories
    if state == 0:
        if category == 'theme':
          print("THEME URL", readingContentTitlesDialogUrl["theme"])
          print("THEME URL replaced", readingContentTitlesDialogUrl["theme"].replace('{language}', language).replace('{speechRate}', speechRate))
          print("LANGAUGE", language)
          
          actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl["theme"].replace('{language}', language).replace('{speechRate}', speechRate)))
        elif category == 'title':
          actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl[parent_selections['type'].lower()].replace('{language}', language).replace('{speechRate}', speechRate)))

    # Loop over the relevant items based on the current state and category
    for index, value_used_to_create_url in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
        if category == 'language':
            language_value = value_used_to_create_url.lower()
            complete_url = pullMenuMainUrl + languageDialogUrls[language_value].replace('{speechRate}', str(speechRate))
            language = language_value
            current_value = value_used_to_create_url.lower()
            description = 'Language menu'
        elif category == 'theme':
            complete_url = value_used_to_create_url
            theme = list(values_to_urls.keys())[list(values_to_urls.values()).index(value_used_to_create_url)]
            current_value = theme
            description = 'Theme menu'
            # print("THEME URL", complete_url)
        elif category == 'type':
            complete_url = value_used_to_create_url.replace('{language}', language).replace('{speechRate}', speechRate)
            print("IN TYPE")
            print("VALUE USED TO CREATE URL", value_used_to_create_url)
            print("COMPLETE URL", complete_url)
            current_value = list(values_to_urls.keys())[list(values_to_urls.values()).index(value_used_to_create_url)]
            description = f"Experience menu"
        elif category == 'title':
            complete_url = value_used_to_create_url
            current_value = list(values_to_urls.keys())[list(values_to_urls.values()).index(value_used_to_create_url)]
            description = 'Title menu'

        # Append the stream action and key press action for the current item
        actions.append(StreamAction(complete_url))
        key = str(index + 1)
        key_to_value_mapping[int(key)] = current_value
        actions.append(StreamAction(getKeyPressUrl(key, language, speechRate)))
    
    # if category == 'language':
    #     for key, language in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
    #         language = language.lower()
    #         actions.append(StreamAction(pullMenuMainUrl + languageDialogUrls[language].replace('{speechRate}', str(speechRate))))
    #         replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    #         replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
    #         actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    # if category == "theme":
    #     language = parent_selections['language']
    #     if state == 0: 
    #         actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl["theme"].replace('{language}',language).replace('{speechRate}',speechRate)))
    #     for key, theme_url in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
    #         actions.append(StreamAction(theme_url))
    #         replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    #         replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
    #         actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    # if category == "type":
    #     for key, experience_url in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
    #         language = parent_selections['language']
    #         experience_url = experience_url.replace('{language}',language).replace('{speechRate}',speechRate)
    #         # print("DOES EXPERIENCE URL NEEDS FIXING", experience_url)
    #         actions.append(StreamAction(experience_url))
    #         replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    #         replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
    #         actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    # if category == "title":
    #     if state == 0:
    #         language = parent_selections['language']
    #         experience = parent_selections['type'].lower()
    #         actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl[experience].replace('{language}',language).replace('{speechRate}',speechRate)))
            
    #     for key, titleUrl in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
    #         # title = content.title
    #         # audioUrl = content.titleAudio + + '/{speechRate}.mp3'
    #         language = parent_selections['language']
    #         actions.append(StreamAction(titleUrl))
    #         replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    #         replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
    #         actions.append(StreamAction(pullMenuMainUrl + replaced_url))
      
    number_of_states_in_same_level = len(items_list) // number_of_categories_listed_in_one_state
    if len(items_list) % number_of_categories_listed_in_one_state != 0:
        number_of_states_in_same_level += 1  
            
    if category == "title":
        category = parent_selections['type'].lower()
        
    if state != number_of_states_in_same_level-1 and number_of_states_in_same_level > 1:
        next4MessageUrl = next4MessageUrls[category].replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + next4MessageUrl))
        actions.append(StreamAction(getKeyPressUrl(next_n_categories_key, language, speechRate)))
        key_to_value_mapping[int(next_n_categories_key)] = 'next ' + str(number_of_categories_listed_in_one_state) + ' items'
        # replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        # replaced_url = re.sub(r'\{key\}', '5', replaced_url)  # Using regex for global replacement
        # actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    elif state != 0 and number_of_states_in_same_level > 1:
        prev4MessageUrl = prev4MessageUrls[category].replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + prev4MessageUrl))
        actions.append(StreamAction(getKeyPressUrl(previous_n_categories_key, language, speechRate)))  
        key_to_value_mapping[int(previous_n_categories_key)] = 'previous ' + str(number_of_categories_listed_in_one_state) + ' items'

        # replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        # replaced_url = re.sub(r'\{key\}', '7', replaced_url)  # Using regex for global replacement
        # actions.append(StreamAction(pullMenuMainUrl + replaced_url))
        
    repeatMenuUrl = repeatCurrentMenuUrl.replace('{language}',language).replace('{speechRate}',speechRate)
    actions.append(StreamAction(pullMenuMainUrl + repeatMenuUrl))
    actions.append(StreamAction(getKeyPressUrl(repeat_current_categories_key, language, speechRate)))
    key_to_value_mapping[int(repeat_current_categories_key)] = 'repeatCurrentMenu'
    # replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    # replaced_url = re.sub(r'\{key\}', '8', replaced_url)  # Using regex for global replacement
    # actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    
    if level != 0 and len(content_attributes) > 1:
        previousMenuMessageUrl = goToPreviousMenuMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + previousMenuMessageUrl))
        actions.append(StreamAction(getKeyPressUrl(previous_category_level_key, language, speechRate)))
        key_to_value_mapping[int(previous_category_level_key)] = 'previous category level'
        
    # print("KEY TO VALUE", key_to_value_mapping)
    # print("DESCRIPTION", description)
    # print("LEVEL", level)
    options = [Option(key=key, value=value) for key, value in key_to_value_mapping.items()]
    description = description if description else "Default Menu Description"
    menu = Menu(description=description, options=options if options else None, level=level)
    
    # Return both the actions and the menu
    # print("MENU", menu)
    # return menu, actions
    
    print("ACTIONS", actions)
    
    
    return {
        'actions': actions,
        'menu': menu
    }
    

def generate_states(fsm, content_list, content_attributes, level, parent_state_id='', parent_selections={}):
    
    if level == len(content_attributes):
        filtered_content = []
        for item in content_list:
            if all(item[k].lower() == v.lower() for k, v in parent_selections.items()):
                filtered_content.append(item)
            
        
        if (parent_selections['type'] == 'quiz'):
            print("PARENT SELECTIONS", parent_selections)
            print("PARENT STATE ID", parent_state_id)
            print("QUIZ", filtered_content[0])
                        
        
        
            quiz_dict = filtered_content[0]
            
            
            quiz_data = QuizData(**quiz_dict)
            quiz = Quiz(quiz_data)
            # quiz.display_quiz()
            # print(quiz.quiz_data.dict())
            # quiz_obj = Quiz.from_dict(filtered_content[0])
            
            indexOfLastOp = parent_state_id.rfind('Op')
            parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
            option_chosen = parent_state_id[indexOfLastOp+2:][:-1]
            indexOfDigit = option_chosen.find('(')
            key_for_option_chosen = int(option_chosen[0:indexOfDigit]) + 1
            # print("QUIZ OBJ QUESTIONS", quiz_obj.questions)
            quiz.generate_states(fsm=fsm, prefix_state_id=parent_state_id[:-1], parent_block_state_id=parent_block_state_id, key_chosen=key_for_option_chosen, level = level)
            # with open('fsm-visual-quiz-in-ini.txt', 'w', encoding='utf-8') as file:
            #     file.write(fsm.visualize_fsm())
            #     file.write(fsm.visualize_fsm())
            
            return
        
        state_id = parent_state_id
        actions = []
        language = parent_selections['language']
        
        # replacedRepeatContentUrl = repeatContentUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        # repeatContentPressKey = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        # repeatContentPressKey = re.sub(r'\{key\}', str(8), repeatContentPressKey)
        
        # actions.append(StreamAction(pullMenuMainUrl + replacedRepeatContentUrl))
        # actions.append(StreamAction(pullMenuMainUrl + repeatContentPressKey))
        
        # replacedExitContentUrl = exitContentUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        # exitContentPressKey = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        # exitContentPressKey = re.sub(r'\{key\}', str(9), exitContentPressKey)
        
        # actions.append(StreamAction(pullMenuMainUrl + replacedExitContentUrl))
        # actions.append(StreamAction(pullMenuMainUrl + exitContentPressKey))
        
        audioGoingTobePlayedUrl = audioGoingTobePlayedDialogUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        # actions.append(TalkAction(text = "To exit the content. Press 9"))
        actions.append(StreamAction(pullMenuMainUrl + audioGoingTobePlayedUrl))
        # https://seedsblob.blob.core.windows.net/output-container/23_1/1.0.wav
        # if (parent_selections['type'] != 'quiz'):
        music_url = content_url + filtered_content[0]['id'] + '/1.0.wav'
        actions.append(StreamAction(music_url, record_playback_time=True))
        
        audioFinishedUrl = audioFinishedMessageUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        
        actions.append(StreamAction(pullMenuMainUrl + audioFinishedUrl))
        actions.append(InputAction(type_=["dtmf"], eventApi='/input', timeOut=3))

        state_id = state_id[:-1] # to remove '-' at the end
        # print("STATE ID", state_id)
                # print("STATE ID", state_id)
        options = []
        repeat_option = Option(key=8, value='repeat')
        exit_option = Option(key=9, value='exit')
        next_state = Option(key=0, value='next (instructions to exit)')
        options = [repeat_option, exit_option, next_state]
        menu = Menu(description= filtered_content[0]['title'] + " Audio Playing", options = options, level=level)


        fsm.add_state(State(state_id=state_id, actions=actions, menu=menu))
        
        indexOfLastOp = parent_state_id.rfind('Op')
        parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
        option_chosen = parent_state_id[indexOfLastOp+2:][:-1]
        indexOfDigit = option_chosen.find('(')
        key_for_option_chosen = int(option_chosen[0:indexOfDigit]) + 1
        
        fsm.add_transition(Transition(source_state_id=parent_block_state_id, dest_state_id=state_id, input=str(key_for_option_chosen), actions=[]))
        
        fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=parent_block_state_id, input=previous_category_level_key, actions=[]))
        fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=state_id, input=str(repeat_current_categories_key), actions=[]))
        
        state_id_final_state = f"{state_id}-LastMenu"
        actions_final = []
        actions_final.append(StreamAction(pullMenuMainUrl + audioFinishedUrl))
        actions_final.append(InputAction(type_=["dtmf"], eventApi='/input'))
        fsm.add_state(State(state_id=state_id_final_state, actions=actions_final))
         
        fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=state_id_final_state, input="empty", actions=[]))
        
        fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=fsm.end_state.id, input="empty", actions=[]))
        fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=parent_block_state_id, input=previous_category_level_key, actions=[]))
        fsm.add_transition(Transition(source_state_id=state_id_final_state, dest_state_id=state_id, input=str(repeat_current_categories_key), actions=[]))
        
        return

    filtered_content = []
    for item in content_list:
        if all(item[k].lower() == v.lower() for k, v in parent_selections.items()):
            filtered_content.append(item)      
        
    input_action = InputAction(type_=["dtmf"], eventApi='/input')
    
    category = content_attributes[level]['category']
    category_id_prefix = content_attributes[level]['id']

    
    values_to_urls = dict()
    sorted_categories = []
    themes = []
    experiences_list = []
    titles = []
    
    if category == "language":
        unique_languages = set([item[category] for item in filtered_content])
        count_languages = dict()
        for lang in unique_languages:
            count_languages[lang] = len([item for item in filtered_content if item[category] == lang])
        sorted_count_languages = sorted(count_languages.items(), key=lambda item: item[1], reverse=True)
        sorted_categories = [x[0] for x in sorted_count_languages]
        values_to_urls = {lang: lang for lang in sorted_categories}
    
    elif category == "theme":
        language = parent_selections['language']
        themes = sorted(set([item['theme'] for item in filtered_content]))
        theme_to_audio_url = {}
        for theme in themes:
            theme_content = [x for x in filtered_content if x['theme'] == theme]
            # print("THEME CONTENT", theme_content[0])
            if 'themeAudio' not in theme_content[0]:
                print("THEME CONTENT", theme_content[0])
            theme_to_audio_url[theme] = theme_content[0]['themeAudio'] + f'/{speechRate}.mp3'
        sorted_categories = list(theme_to_audio_url.values())
        print("THEME TO AUDIO URL", theme_to_audio_url)
        values_to_urls = theme_to_audio_url
          
    elif category == "type":
        experiences_list = sorted(set(item['type'].lower() for item in filtered_content))
        sorted_categories = [experienceDialogAudioUrls[experience] for experience in experiences_list]
        values_to_urls = {experience: experienceDialogAudioUrls[experience] for experience in experiences_list}
        
    elif category == "title":
        titles = []
        sorted_categories = []
        titles = sorted(set(item['title'] for item in filtered_content))
        for title in titles:
            if parent_selections['type'] == 'quiz':
                titleAudio = [x for x in filtered_content if x['title'] == title][0]['titleAudio'] 
                sorted_categories.append(titleAudio)
            else:
                titleAudio = [x for x in filtered_content if x['title'] == title][0]['titleAudio'] + f'/{speechRate}.mp3'
                sorted_categories.append(titleAudio)
        values_to_urls = {title: titleAudio for title, titleAudio in zip(titles, sorted_categories)}

    number_of_states_in_same_level = len(sorted_categories) // number_of_categories_listed_in_one_state
    if len(sorted_categories) % number_of_categories_listed_in_one_state != 0:
        number_of_states_in_same_level += 1
    
    # print("NUMBER OF CATEGORIES FOR", category, ":", len(sorted_categories), "NUMBER OF STATES", number_of_states_in_same_level)
    for state in range(number_of_states_in_same_level):
        state_id = f"{parent_state_id}{category_id_prefix}{state}"
        print("STATE ID", state_id)
        actions = []
        if level == 0 and state == 0:
            print("INITIAL STATE", state_id)
            fsm.init_state_id = state_id
            # actions.append(StreamAction(url = 'https://seedsblob.blob.core.windows.net/pull-model-menus/welcomeDialog/kannada/welcome%20to%20SEEDS/1.0.mp3'))
          
        result_dictionary = getStreamActions(sorted_categories, values_to_urls, level, state, parent_selections)
        # print("RESULT DICTIONARY", result_dictionary)
        stream_actions = result_dictionary['actions']
        menu = result_dictionary['menu']
        # print("RETURN VALUE", return_value)
        # stream_actions = return_value[0]
        # menu = return_value[1]
        actions += stream_actions
    

        
        # actions += getStreamActions(sorted_categories, level, state, parent_selections)

        if level < len(content_attributes):
            actions.append(input_action)
         
        fsm.add_state(State(state_id=state_id, actions=actions, menu=menu))
        if level > 0 and state == 0: # Add transition to the parent state
            indexOfLastOp = parent_state_id.rfind('Op')
            parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
            option_chosen = parent_state_id[indexOfLastOp+2:][:-1]
            indexOfDigit = option_chosen.find('(')
            key_for_option_chosen = int(option_chosen[0:indexOfDigit]) + 1
            fsm.add_transition(Transition(source_state_id=parent_block_state_id, dest_state_id=state_id, input=str(key_for_option_chosen), actions=[]))
        
    
    for state in range(number_of_states_in_same_level):
        state_id = f"{parent_state_id}{category_id_prefix}{state}"
        if state != 0 and number_of_states_in_same_level > 1:
            fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=f"{parent_state_id}{category_id_prefix}{state-1}", input=previous_n_categories_key, actions=[]))
        if state != number_of_states_in_same_level-1 and number_of_states_in_same_level > 1:
            fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=f"{parent_state_id}{category_id_prefix}{state+1}", input=next_n_categories_key, actions=[]))
        fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=state_id, input=repeat_current_categories_key, actions=[]))
        if level != 0 and len(content_attributes) > 1:
            indexOfLastOp = parent_state_id.rfind('Op')
            parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
            fsm.add_transition(Transition(source_state_id=state_id, dest_state_id=parent_block_state_id, input=previous_category_level_key, actions=[]))

        
        indexes_possible = min((state+1)*number_of_categories_listed_in_one_state, len(sorted_categories)) - state*number_of_categories_listed_in_one_state
        
        for index_of_category in range(indexes_possible):
            new_state_id = f"{state_id}-Op{index_of_category}"
            index_of_item = state*number_of_categories_listed_in_one_state + index_of_category
            new_selections = parent_selections.copy()
            if category == "language":
                new_selections[content_attributes[level]['category']] = sorted_categories[index_of_item]
                new_state_id = f"{new_state_id}({sorted_categories[index_of_category]})-"
            elif category == "theme":
                new_selections['theme'] = themes[index_of_item]
                new_state_id = f"{new_state_id}({themes[index_of_item]})-"
            elif category == "type":
                new_selections['type'] = experiences_list[index_of_item]
                new_state_id = f"{new_state_id}({experiences_list[index_of_item]})-"
            elif category == "title":
                new_selections['title'] = titles[index_of_item]
                new_state_id = f"{new_state_id}({titles[index_of_item]})-"
            if level + 1 <= len(content_attributes):
                generate_states(fsm, content_list, content_attributes, level + 1, new_state_id, new_selections)
        
def format_data(data, level=0):
    output = ""
    for key, value in data.items():
        output += "     " * level + "- " + key + ":\n"
        if isinstance(value, dict):
            output += format_data(value, level + 1)
        elif isinstance(value, set):
            for item in value:
                output += "     " * (level + 1) + "- " + item + "\n"
    return output

def count_last_level_content(data):
    if isinstance(data, set):
        return len(data)
    elif isinstance(data, dict):
        return sum(count_last_level_content(value) for value in data.values())
    return 0

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

async def get_all_content():
    api_url = os.environ.get("SEEDS_SERVER_BASE_URL", "") + "content"
    headers = {
        'authToken': 'postman'
    }
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(api_url, headers=headers) as response:
                response.raise_for_status()  # Raise an exception for HTTP errors
                response_data = await response.text()  # get response text
                contents = json.loads(response_data)  # parse text to JSON
                
        # FILTER ISPULLMODEL AND ISDELETED CONTENT
        content = [
            x for x in contents
            if all(key in x for key in ["isPullModel", "isDeleted", "isProcessed"]) and
            x["isPullModel"] and x["isProcessed"] and not x["isDeleted"]
        ]
        content.append(quiz_new)
        return content
    except aiohttp.ClientError as e:
        print(f"Client error: {e}")
        return {"error": "Client error occurred while fetching all content."}
    except json.JSONDecodeError as e:
        print(f"JSON decode error: {e}")
        return {"error": "Failed to decode JSON response."}
    except Exception as e:
        print(f"Unexpected error: {e}")
        return {"error": "An unexpected error occurred."}

            
async def instantiate_from_latest_content(content_ids: Optional[List[str]] = None):
    content = []
    
    if (content_ids):
        print("CONTENT IDS SENT")
        content = await get_content_by_ids(content_ids)
        print(content)
    else:
        content = await get_all_content()
        
    fsm = FSM(fsm_id=str(uuid.uuid4()))
    fsm.set_end_state(State(state_id="END", actions=[TalkAction(text="You didn't choose a valid option. Bye bye.", bargeIn=False)]))
    parent_selections = {'language': 'kannada'}
    generate_states(fsm, content, content_attributes, 0, parent_selections=parent_selections)
   
    print("NUMBER OF CONTENT", len(content))
    # quiz = Quiz(title='Geography Quiz', quiz_id='GEO123', positive_marks=5, negative_marks=2, questions=questions, language='kannada', theme='My environment')
    # # print(quiz.type)
    # quiz_dict = quiz.to_dict()

    
    # print("NUMBER OF CONTENT", len(content))
    # print('FECTHED LATEST CONTENT: ', \
    #             json.dumps(contents, indent=2, ensure_ascii=False))


    # print('FECTHED LATEST CONTENT: ', \
    #             json.dumps(contents, indent=2, ensure_ascii=False))
    
      # fsm.set_init_state(fsm.init_state_id)
    
    
    # print("FSM IS HERE")
    # with open('fsm-visual.txt', 'w', encoding='utf-8') as file:
    #     file.write(fsm.visualize_fsm())
    # print("FSM IS HERE")
    # fsm.print_transitions()
    # with open('/home/kavyansh/SEEDS/IVRv2/fsm.json', 'w', encoding='utf-8') as file:
    #     json.dump(fsm.serialize().dict(), file, indent=4)
    return fsm

def instantitate_from_doc(data: IVRfsmDoc):
    return FSM.deserialize(data)
        
# if __name__ == "__main__":
#     asyncio.run(instantiate_from_latest_content())

# with open('contents.json', 'r', encoding='utf-8') as file:
# file = open('contents.json', 'r', encoding='utf-8')
# contents = json.load(file)
# content = [x for x in contents if x['language'].lower() == 'kannada']

    
    
        


    # themes = []
    # response = requests.get(f'{url}/themes?language=kannada', headers={'authToken': 'postman'})
    # if response.status_code == 200:
    #     themes_data = response.json()
    #     for theme_obj in themes_data:
    #         if theme_obj['name'].lower() not in themes:
    #             themes.append(theme_obj['name'].lower())

    # sorted_themes = sorted(themes)
    # theme_to_experience = {}
    # for theme in sorted_themes:
    #     theme_content = [x for x in content if x['theme'].lower() == theme]
    #     experiences = set([x['type'].lower() for x in theme_content])
    #     experience_to_title = {}
    #     for experience in experiences:
    #         experience_content = [x for x in theme_content if x['type'].lower() == experience.lower()]
    #         titles = set([x['title'] for x in experience_content])
    #         experience_to_title[experience] = titles
    #     theme_to_experience[theme] = experience_to_title
        
    # total_content = count_last_level_content(theme_to_experience)
    # with open("output.txt", "w") as file:
    #     file.write(f"Total content at the last level: {total_content}\n")
    #     file.write(format_data(theme_to_experience))




# with open("output-fsm-vis.txt", "w", encoding='utf-8') as file:
#     file.write(fsm.visualize_fsm())
    
# file.close()


