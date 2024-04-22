from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction

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

from utils.sas_gen import SASGen

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
    {'category': 'language', 'level': 0, 'id': 'LA'},
    {'category': 'theme', 'level': 1, 'id': 'TH'},
    {'category': 'type', 'level': 2, 'id': 'EX'},
    {'category': 'title', 'level': 3, 'id': 'TI'}
]

# sas_test = 'https://seedsblob.blob.core.windows.net/output-container/1dfe33fd-7fb7-4adb-9d60-2d9ae3c44910/1.0.wav'
# sas_gen_obj = SASGen(os.getenv("BLOB_STORE_CONN_STR"))
# sas_url = sas_gen_obj.get_url_with_sas(sas_test)
# print("SAS", sas_url)


def getStreamActions(items_list, level, state, parent_selections = {}):
    
    category = content_attributes[level]['category']
    
    number_of_states_in_same_level = len(items_list) // number_of_categories_listed_in_one_state
    if len(items_list) % number_of_categories_listed_in_one_state != 0:
        number_of_states_in_same_level += 1
        
    actions = []
    
    if level == 0 and state == 0:
        actions.append(StreamAction('https://seedsblob.blob.core.windows.net/pull-model-menus/welcomeDialog/kannada/welcome%20to%20SEEDS/1.0.mp3'))
    
    if category == 'language':
        for key, language in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
            language = language.lower()
            actions.append(StreamAction(pullMenuMainUrl + languageDialogUrls[language].replace('{speechRate}', str(speechRate))))
            replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
            replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
            actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    if category == "theme":
        language = parent_selections['language']
        if state == 0: 
            actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl["theme"].replace('{language}',language).replace('{speechRate}',speechRate)))
        for key, theme_url in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
            actions.append(StreamAction(theme_url))
            replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
            replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
            actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    if category == "type":
        for key, experience_url in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
            language = parent_selections['language']
            experience_url = experience_url.replace('{language}',language).replace('{speechRate}',speechRate)
            # print("DOES EXPERIENCE URL NEEDS FIXING", experience_url)
            actions.append(StreamAction(experience_url))
            replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
            replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
            actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    if category == "title":
        if state == 0:
            language = parent_selections['language']
            experience = parent_selections['type'].lower()
            actions.append(StreamAction(pullMenuMainUrl + readingContentTitlesDialogUrl[experience].replace('{language}',language).replace('{speechRate}',speechRate)))
            
        for key, titleUrl in enumerate(items_list[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(items_list))]):
            # title = content.title
            # audioUrl = content.titleAudio + + '/{speechRate}.mp3'
            language = parent_selections['language']
            actions.append(StreamAction(titleUrl))
            replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
            replaced_url = re.sub(r'\{key\}', str(key+1), replaced_url)  # Using regex for global replacement
            actions.append(StreamAction(pullMenuMainUrl + replaced_url))
            
    if category == "title":
        category = parent_selections['type'].lower()
        
    if state != number_of_states_in_same_level-1 and number_of_states_in_same_level > 1:
        next4MessageUrl = next4MessageUrls[category].replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + next4MessageUrl))
        replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        replaced_url = re.sub(r'\{key\}', '5', replaced_url)  # Using regex for global replacement
        actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    elif state != 0 and number_of_states_in_same_level > 1:
        prev4MessageUrl = prev4MessageUrls[category].replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + prev4MessageUrl))
        replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        replaced_url = re.sub(r'\{key\}', '7', replaced_url)  # Using regex for global replacement
        actions.append(StreamAction(pullMenuMainUrl + replaced_url))
        
    repeatMenuUrl = repeatCurrentMenuUrl.replace('{language}',language).replace('{speechRate}',speechRate)
    actions.append(StreamAction(pullMenuMainUrl + repeatMenuUrl))
    replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
    replaced_url = re.sub(r'\{key\}', '8', replaced_url)  # Using regex for global replacement
    actions.append(StreamAction(pullMenuMainUrl + replaced_url))
    
    
    if level != 0 and len(content_attributes) > 1:
        previousMenuMessageUrl = goToPreviousMenuMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)
        actions.append(StreamAction(pullMenuMainUrl + previousMenuMessageUrl))
        replaced_url = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        replaced_url = re.sub(r'\{key\}', '9', replaced_url)  # Using regex for global replacement
        actions.append(StreamAction(pullMenuMainUrl + replaced_url))

    return actions
    
    

def generate_states(fsm, content_list, content_attributes, level, parent_state_id='', parent_selections={}):
    
    if level == len(content_attributes):
        filtered_content = []
        for item in content_list:
            if all(item[k].lower() == v.lower() for k, v in parent_selections.items()):
                filtered_content.append(item)
            
        state_id = parent_state_id
        actions = []
        language = parent_selections['language']
        
        replacedRepeatContentUrl = repeatContentUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        repeatContentPressKey = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        repeatContentPressKey = re.sub(r'\{key\}', str(8), repeatContentPressKey)
        
        actions.append(StreamAction(pullMenuMainUrl + replacedRepeatContentUrl))
        actions.append(StreamAction(pullMenuMainUrl + repeatContentPressKey))
        
        replacedExitContentUrl = exitContentUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        exitContentPressKey = pressKeyMessageUrl.replace('{language}', language).replace('{speechRate}', str(speechRate))
        exitContentPressKey = re.sub(r'\{key\}', str(9), exitContentPressKey)
        
        actions.append(StreamAction(pullMenuMainUrl + replacedExitContentUrl))
        actions.append(StreamAction(pullMenuMainUrl + exitContentPressKey))
        
        audioGoingTobePlayedUrl = audioGoingTobePlayedDialogUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        # actions.append(TalkAction(text = "To exit the content. Press 9"))
        actions.append(StreamAction(pullMenuMainUrl + audioGoingTobePlayedUrl))
        # https://seedsblob.blob.core.windows.net/output-container/23_1/1.0.wav
        music_url = content_url + filtered_content[0]['id'] + '/1.0.wav'
        
        actions.append(StreamAction(music_url))
        audioFinishedUrl = audioFinishedMessageUrl.replace('{language}', language).replace('{speechRate}', speechRate)
        
        actions.append(StreamAction(pullMenuMainUrl + audioFinishedUrl))
        actions.append(InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input', timeOut=1))

        state_id = state_id[:-1] # to remove '-' at the end
        print("STATE ID", state_id)
        fsm.add_state(State(state_id=state_id, actions=actions))
        
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
        actions_final.append(InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input'))
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
        
    input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')
    
    category = content_attributes[level]['category']
    category_id_prefix = content_attributes[level]['id']

    
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
        
    elif category == "theme":
        language = parent_selections['language']
        themes = sorted(set([item['theme'] for item in filtered_content]))
        theme_to_audio_url = {}
        for theme in themes:
            theme_content = [x for x in filtered_content if x['theme'] == theme]
            theme_to_audio_url[theme] = theme_content[0]['themeAudio'] + f'/{speechRate}.mp3'
        sorted_categories = list(theme_to_audio_url.values())
          
    elif category == "type":
        experiences_list = sorted(set(item['type'].lower() for item in filtered_content))
        sorted_categories = [experienceDialogAudioUrls[experience] for experience in experiences_list]
        
    elif category == "title":
        titles = sorted(set(item['title'] for item in filtered_content))
        sorted_categories = []
        for title in titles:
            titleAudio = [x for x in filtered_content if x['title'] == title][0]['titleAudio'] + f'/{speechRate}.mp3'
            sorted_categories.append(titleAudio)


    number_of_states_in_same_level = len(sorted_categories) // number_of_categories_listed_in_one_state
    if len(sorted_categories) % number_of_categories_listed_in_one_state != 0:
        number_of_states_in_same_level += 1
    
    # print("NUMBER OF CATEGORIES FOR", category, ":", len(sorted_categories), "NUMBER OF STATES", number_of_states_in_same_level)
    for state in range(number_of_states_in_same_level):
        state_id = f"{parent_state_id}{category_id_prefix}{state}"
        print("STATE ID", state_id)
        actions = []
        if level == 0 and state == 0:
            fsm.init_state_id = state_id
            # actions.append(StreamAction(url = 'https://seedsblob.blob.core.windows.net/pull-model-menus/welcomeDialog/kannada/welcome%20to%20SEEDS/1.0.mp3'))
        
        actions += getStreamActions(sorted_categories, level, state, parent_selections)

        if level < len(content_attributes):
            actions.append(input_action)
         
        fsm.add_state(State(state_id=state_id, actions=actions))
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


async def instantiate_from_latest_content():
    api_url = os.environ.get("SEEDS_SERVER_BASE_URL", "") + "content"
    headers = {
        'authToken': 'postman'
    }
    async with aiohttp.ClientSession() as session:
        async with session.get(api_url, headers=headers) as response:
            response_data = await response.text()  # get response text
            # Parse text to JSON
            contents = json.loads(response_data)
            
    # FILTER ISPULLMODEL AND ISDELETED CONTENT
    content = [
        x for x in contents
        if all(key in x for key in ["isPullModel", "isDeleted", "isProcessed"]) and
        x["isPullModel"] and x["isProcessed"] and not x["isDeleted"]
    ]
    print('FECTHED LATEST CONTENT: ', \
                json.dumps(contents, indent=2, ensure_ascii=False))
    
    fsm = FSM(fsm_id="SEEDS-IVR")
    fsm.set_end_state(State(state_id="END", actions=[TalkAction("Bye bye")]))
    generate_states(fsm, content, content_attributes, 0)
    # with open('/home/kavyansh/SEEDS/IVRv2/fsm.json', 'w', encoding='utf-8') as file:
    #     json.dump(fsm.serialize(), file, indent=4)
    return fsm

def instantitate_from_json():
    file_path = 'fsm.json'
    with open(file_path, 'r', encoding='utf-8') as file:
        data = json.load(file)
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


