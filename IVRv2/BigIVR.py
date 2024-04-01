import json
from fastapi import FastAPI, Request, Response
import vonage
from pydantic import BaseModel
from typing import Optional
from fastapi.responses import JSONResponse

from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction
from actions.vonage_actions.vonage_action_factory import VonageActionFactory

from fsm.state import State
from fsm.transition import Transition
from fsm.fsm import FSM
import os
from dotenv import load_dotenv

load_dotenv()

fsm = FSM(fsm_id="fsm1")
input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')

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

content_list = [
    {"language": "English", "theme": "Nature", "type": "Story", "title": "The Forest Adventure"},
    {"language": "English", "theme": "Nature", "type": "Poem", "title": "The Mighty Oak"},
    {"language": "English", "theme": "Adventure", "type": "Story", "title": "Pirate's Treasure"},
    {"language": "English", "theme": "Adventure", "type": "Game", "title": "Escape Island"},
    {"language": "English", "theme": "Family", "type": "Story", "title": "Grandma's Tales"},
    {"language": "English", "theme": "Family", "type": "Song", "title": "Together Forever"},
    {"language": "English", "theme": "Space", "type": "Story", "title": "Moon Landing"},
    {"language": "English", "theme": "Space", "type": "Quiz", "title": "Astronomy Quiz"},
    {"language": "Spanish", "theme": "Nature", "type": "Story", "title": "La Aventura del Bosque"},
    {"language": "Spanish", "theme": "Nature", "type": "Poem", "title": "El Roble Poderoso"},
    {"language": "Spanish", "theme": "Adventure", "type": "Story", "title": "El Tesoro del Pirata"},
    {"language": "Spanish", "theme": "Adventure", "type": "Game", "title": "Isla de Escape"},
    {"language": "Spanish", "theme": "Family", "type": "Story", "title": "Cuentos de la Abuela"},
    {"language": "Spanish", "theme": "Family", "type": "Song", "title": "Juntos Para Siempre"},
    {"language": "Spanish", "theme": "Space", "type": "Story", "title": "Aterrizaje en la Luna"},
    {"language": "Spanish", "theme": "Space", "type": "Quiz", "title": "Cuestionario de Astronomía"},
    {"language": "French", "theme": "Nature", "type": "Story", "title": "L'Aventure de la Forêt"},
    {"language": "French", "theme": "Nature", "type": "Poem", "title": "Le Chêne Puissant"},
    {"language": "French", "theme": "Adventure", "type": "Story", "title": "Le Trésor du Pirate"},
    {"language": "French", "theme": "Adventure", "type": "Game", "title": "Île Évasion"},
    {"language": "French", "theme": "Family", "type": "Story", "title": "Contes de Grand-mère"},
    {"language": "French", "theme": "Family", "type": "Song", "title": "Ensemble Pour Toujours"},
    {"language": "French", "theme": "Space", "type": "Story", "title": "Alunissage"},
    {"language": "French", "theme": "Space", "type": "Quiz", "title": "Quiz d'Astronomie"},
    {"language": "German", "theme": "Nature", "type": "Story", "title": "Das Waldabenteuer"},
    {"language": "German", "theme": "Nature", "type": "Poem", "title": "Die Mächtige Eiche"},
    {"language": "German", "theme": "Adventure", "type": "Story", "title": "Der Piratenschatz"},
    {"language": "German", "theme": "Adventure", "type": "Game", "title": "Insel Flucht"},
    {"language": "German", "theme": "Family", "type": "Story", "title": "Großmutters Geschichten"},
    {"language": "German", "theme": "Family", "type": "Song", "title": "Für Immer Zusammen"},
    {"language": "German", "theme": "Space", "type": "Story", "title": "Mondlandung"},
    {"language": "German", "theme": "Space", "type": "Quiz", "title": "Astronomie Quiz"},
    {"language": "English", "theme": "Science", "type": "Documentary", "title": "The Wonders of Photosynthesis"},
    {"language": "Spanish", "theme": "History", "type": "Documentary", "title": "The Ancient Civilizations"},
    {"language": "French", "theme": "Technology", "type": "Documentary", "title": "The Future of AI"},
    {"language": "German", "theme": "Art", "type": "Documentary", "title": "The Evolution of European Art"},
    {"language": "Hindi", "theme": "Art", "type": "Documentary", "title": "Munna Bhai MBBS"},
    {"language": "Kannada", "theme": "Art", "type": "Documentary", "title": "Yella waste"},
    {"language": "Tamil", "theme": "Art", "type": "Documentary", "title": "Yella waste"},
    {"language": "Odiya", "theme": "Art", "type": "Documentary", "title": "Yella waste"},
    
    {"language": "Bengali", "theme": "Art", "type": "Documentary", "title": "Yella waste 1"},
    {"language": "Bengali", "theme": "Art", "type": "Documentary", "title": "Yella waste 2"},
    {"language": "Bengali", "theme": "Art", "type": "Documentary", "title": "Yella waste 3"},
    {"language": "Bengali", "theme": "Art", "type": "Documentary", "title": "Yella waste 4"},
    {"language": "Bengali", "theme": "Art", "type": "Documentary", "title": "Yella waste 5"},
    
    {"language": "Bengali", "theme": "Dance", "type": "Documentary", "title": "Yella waste"},
    {"language": "Bengali", "theme": "Fun", "type": "Documentary", "title": "Yella waste"},
    {"language": "German", "theme": "Art", "type": "Documentary", "title": "Yella waste"},
    {"language": "Telugu", "theme": "Art", "type": "Documentary", "title": "Yella waste"}
]



def generate_states(fsm, content_list, content_attributes, level, parent_state_id='', parent_selections={}):
    # Define the input action
    
    if level == len(content_attributes):
        filtered_content = []
        for item in content_list:
            if all(item[k] == v for k, v in parent_selections.items()):
                filtered_content.append(item)
    
        state_id = parent_state_id
        actions = []
        actions.append(TalkAction("After choosing" + str(parent_selections) + " you got title: " + filtered_content[0]['title']))
        state_id = state_id[:-1]
        fsm.add_state(State(state_id=state_id, actions=actions))
        indexOfLastOp = parent_state_id.rfind('Op')
        parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
        option_chosen = parent_state_id[indexOfLastOp+2:][:-1]
        key_for_option_chosen = int(option_chosen) + 1
        fsm.add_transition(Transition(source_state_id=parent_block_state_id, dest_state_id=state_id, input=key_for_option_chosen, actions=[]))
        return
        
    input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')

    # Extract category at the current level
    
    category = content_attributes[level]['category']
    category_id_prefix = content_attributes[level]['id']

    # Filter content based on selections made in previous levels
    filtered_content = []
    for item in content_list:
        if all(item[k] == v for k, v in parent_selections.items()):
            filtered_content.append(item)
    

    # Group content by category
    grouped_content = {}
    for item in filtered_content:
        key = item[category]
        if key not in grouped_content:
            grouped_content[key] = []
        grouped_content[key].append(item)
        
    sorted_categories = sorted(grouped_content.keys())  # ['English', 'French', 'German', 'Spanish']

    number_of_states_in_same_level = len(sorted_categories) // number_of_categories_listed_in_one_state
    if len(sorted_categories) % number_of_categories_listed_in_one_state != 0:
        number_of_states_in_same_level += 1
    
    for state in range(number_of_states_in_same_level):
        state_id = f"{parent_state_id}{category_id_prefix}{state}"
        actions = []
        if level == 0 and state == 0:
            fsm.init_state_id = state_id
            actions.append(StreamAction(url = 'https://contentmenu.blob.core.windows.net/menu/WelcomeToSeedsNinad.mp3'))
        for keys, category in enumerate(sorted_categories[state*number_of_categories_listed_in_one_state: min((state+1)*number_of_categories_listed_in_one_state, len(sorted_categories))]):
            actions.append(TalkAction("For " + category + " press " + str(keys+1)))
            # dest_state_id = str(category_level) + str(state) + str(keys+1)    
        if state == 0 and number_of_states_in_same_level > 1:
            actions.append(TalkAction("For next " + str(number_of_categories_listed_in_one_state) + " categories press " + next_n_categories_key))
        elif state == number_of_states_in_same_level-1 and number_of_states_in_same_level > 1:
            actions.append(TalkAction("For previous " + str(number_of_categories_listed_in_one_state) + " categories press " + previous_n_categories_key))
        elif number_of_states_in_same_level > 1:
            actions.append(TalkAction("For next " + str(number_of_categories_listed_in_one_state) + " categories press " + next_n_categories_key))
            actions.append(TalkAction("For previous " + str(number_of_categories_listed_in_one_state) + " categories press " + previous_n_categories_key))
        
        actions.append(TalkAction("To repeat the current categories press " + repeat_current_categories_key))
        if level != 0 and len(content_attributes) > 1:
            actions.append(TalkAction("To go back to previous level press " + previous_category_level_key))
        if level < len(content_attributes) - 1:
            actions.append(input_action)
         
        fsm.add_state(State(state_id=state_id, actions=actions))
        if level > 0 and state == 0:
            indexOfLastOp = parent_state_id.rfind('Op')
            parent_block_state_id = parent_state_id[:(indexOfLastOp-1)]
            option_chosen = parent_state_id[indexOfLastOp+2:][:-1]
            key_for_option_chosen = int(option_chosen) + 1
            fsm.add_transition(Transition(source_state_id=parent_block_state_id, dest_state_id=state_id, input=key_for_option_chosen, actions=[]))
        
    
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
            new_state_id = f"{state_id}-Op{index_of_category}-"
            new_selections = parent_selections.copy()
            new_selections[content_attributes[level]['category']] = sorted_categories[index_of_category]
            if level + 1 <= len(content_attributes):
                generate_states(fsm, content_list, content_attributes, level + 1, new_state_id, new_selections)
        
fsm = FSM(fsm_id="fsm1")
generate_states(fsm, content_list, content_attributes, 0)
fsm.print_transitions()
fsm.print_states()
fsm.visualize_fsm()


