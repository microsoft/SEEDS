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
import uuid

from utils.sas_gen import SASGen

load_dotenv()

class Quiz:
    def __init__(self, language, theme, title, quiz_id, positive_marks, negative_marks, questions=[]):
        """
        Initializes a new Quiz instance.

        :param title: str - the title of the quiz
        :param quiz_id: str - a unique identifier for the quiz
        :param positive_marks: int - the score for a correct answer in the quiz
        :param negative_marks: int - the penalty for a wrong answer in the quiz
        :param questions: list - a list of dictionaries, each representing a question with its text, a URL,
                                 and its options.
        """
        self.title = title
        self.id = quiz_id
        self.language = language
        self.theme = theme
        self.positiveMarks = positive_marks
        self.negativeMarks = negative_marks
        self.questions = questions
        self.input_action = InputAction(type_=["dtmf"], eventUrl=os.getenv('NGROK_URL') + '/input')
        self.move_forward_key = "1"
        self.type = "quiz"
    
    def add_question(self, question):
        """
        Adds a new question to the quiz.

        :param question: dict - a dictionary representing the new question to add
        """
        self.questions.append(question)
    
    def remove_question(self, question_title):
        """
        Removes a question from the quiz based on its title.

        :param question_title: str - the title of the question to remove
        """
        self.questions = [q for q in self.questions if q['question']['text'] != question_title]
    
    def display_quiz(self):
        """
        Prints the quiz details, including questions and their options.
        """
        print(f"Quiz: {self.title} (ID: {self.id})")
        print(f"Language: {self.language}")
        print(f"Theme: {self.theme}")
        print(f"Positive Marks per correct answer: {self.positiveMarks}")
        print(f"Negative Marks per incorrect answer: {self.negativeMarks}")
        for question in self.questions:
            print(f"\nQuestion: {question['question']['text']} (URL: {question['question']['url']})")
            print("Options:")
            for option in question['options']:
                print(option['text'])
                
            correct_option = [opt for opt in question['options'] if opt['id'] == question['correct_option']][0]
            print(f"Correct Option: {correct_option['text']}\n")
            
    def to_dict(self):
        """
        Converts the Quiz object into a dictionary representation.
        """
        # Convert each question into a dictionary representation
        questions_dict = [{
            'question': {
                'text': question['question']['text'],
                'url': question['question']['url'],
            },
            'options': [
                {'id': opt['id'], 'text': opt['text']} for opt in question['options']
            ],
            'correct_option': question['correct_option'],
        } for question in self.questions]

        # Create the main dictionary for the Quiz object
        quiz_dict = {
            'title': self.title,
            'id': self.id,
            'language': self.language,
            'theme': self.theme,
            'type': self.type,  
            'positive_marks': self.positiveMarks,
            'negative_marks': self.negativeMarks,
            'questions': questions_dict,
        }

        return quiz_dict
    
    @classmethod
    def from_dict(cls, quiz_dict):
        title = quiz_dict['title']
        quiz_id = quiz_dict['id']
        language = quiz_dict['language']
        theme = quiz_dict['theme']
        positive_marks = quiz_dict['positive_marks']
        negative_marks = quiz_dict['negative_marks']

        questions = [{
            'question': {
                'text': question['question']['text'],
                'url': question['question']['url'],
            },
            'options': question['options'],
            'correct_option': question['correct_option'],
        } for question in quiz_dict['questions']]

        return cls(language, theme, title, quiz_id, positive_marks, negative_marks, questions)
                
    
    def generate_states(self, fsm, prefix_state_id, parent_block_state_id, key_chosen):
        initial_quiz_state_id = f"{prefix_state_id}_QuizStart"
        # fsm = FSM(fsm_id = "quiz")
        
        initial_state = self.get_initial_state(initial_quiz_state_id)
        fsm.add_state(initial_state)
        # print("PREFIX", prefix_state_id)
        # print("PARENT BLOCK", parent_block_state_id)
        parent_to_initial_transition = Transition(input=str(key_chosen), source_state_id=parent_block_state_id, dest_state_id=initial_quiz_state_id)
        print("PARENT_TO_INITIAL", parent_to_initial_transition)
        fsm.add_transition(parent_to_initial_transition)
        # fsm.set_init_state_id(initial_quiz_state_id)
        
        mapping_questionids_common_ids = []
        
        prev_state_question_id = None
        
        for index, question in enumerate(self.questions):
            question_state_id = f"{prefix_state_id}_Question{index+1}"
            actions = []
            correct_option_id = question['correct_option']
            correct_option_text = [opt['text'] for opt in question['options'] if opt['id'] == correct_option_id][0]
            actions.append(TalkAction(text=question['question']['text']))
            transitions = []
            mapping = dict()
            mapping['question_id'] = question_state_id
            cor_or_wrong_state_ids = []
            for index_option, option in enumerate(question['options']):
                actions.append(TalkAction(text=option['text']))
                if option['id'] == correct_option_id:
                    correct_state = self.get_correct_option_state(prefix_state_id, "Q" + str(index+1)+ "-O" + str(index_option+1))
                    fsm.add_state(correct_state)
                    cor_or_wrong_state_ids.append(correct_state.id)
                    # mapping_questionids_common_ids.append(mapping)
                    transition_from_question_to_correct = Transition(input=str(index_option+1), source_state_id=question_state_id, dest_state_id=correct_state.id)
                    transitions.append(transition_from_question_to_correct)
                
                else:
                    incorrect_state = self.get_incorrect_option_state(prefix_state_id, "Q" + str(index+1)+ "-O" + str(index_option+1), correct_option_text)
                    fsm.add_state(incorrect_state)
                    cor_or_wrong_state_ids.append(incorrect_state.id)
                    # mapping_questionids_common_ids.append(mapping)
                    transition_from_question_to_incorrect = Transition(input=str(index_option+1), source_state_id=question_state_id, dest_state_id=incorrect_state.id) 
                    transitions.append(transition_from_question_to_incorrect)
                    
            actions.append(TalkAction(text="Please select an option."))
            actions.append(TalkAction(text="To replay the question, press 8."))
            actions.append(self.input_action)
            mapping['cor_or_wrong_state_ids'] = cor_or_wrong_state_ids
            mapping_questionids_common_ids.append(mapping)
            
            question_state = State(state_id=question_state_id, actions=actions)
            
            fsm.add_state(question_state)
            repeat_question_transition = Transition(input="8", source_state_id=question_state_id, dest_state_id=question_state_id)
            for transition in transitions:
                fsm.add_transition(transition)
            fsm.add_transition(repeat_question_transition)
           
        
        final_state = self.get_final_state(prefix_state_id)
        fsm.add_state(final_state)
       
        for index, mapping in enumerate(mapping_questionids_common_ids):
            if index == len(mapping_questionids_common_ids) - 1:
                for state_id in mapping['cor_or_wrong_state_ids']:
                    last_to_end = Transition(input=self.move_forward_key, source_state_id=state_id, dest_state_id=final_state.id)
                    fsm.add_transition(last_to_end)
                    
            else:
                if index == 0:
                    start_id_to_first_question = Transition(input=self.move_forward_key, source_state_id=initial_quiz_state_id, dest_state_id=mapping['question_id'])
                    fsm.add_transition(start_id_to_first_question)
                for state_id in mapping['cor_or_wrong_state_ids']:
                    next_question = Transition(input=self.move_forward_key, source_state_id=state_id, dest_state_id=mapping_questionids_common_ids[index+1]['question_id'])
                    fsm.add_transition(next_question)
               
        end_to_parent = Transition(input=self.move_forward_key, source_state_id=final_state.id, dest_state_id=parent_block_state_id)
        fsm.add_transition(end_to_parent)   
        # with open('fsm-quiz-class.txt', 'w', encoding='utf-8') as f:
        #     f.write(fsm.visualize_fsm())
        return fsm   
    
    
    def get_initial_state(self, initial_quiz_state_id):
        """
        Returns the initial state of the quiz.
        """
        actions = []
        actions.append(TalkAction(text=f"Welcome to the {self.title} quiz!"))
        actions.append(TalkAction(text=f"Let's get started!"))
        actions.append(TalkAction(text=f"Press 1 to start the quiz."))
        actions.append(self.input_action)
        post_operation = QuizInitStateOperation()
        initial_state = State(state_id=initial_quiz_state_id, actions=actions, post_operation=post_operation)
        
        return initial_state
    
    def get_final_state(self, prefix_state_id):
        actions = []
        actions.append(TalkAction(text="Congratulations! You have completed the quiz."))
        actions.append(TalkAction(text="Your final score is something points."))
        actions.append(TalkAction(text="Press 1 to exit the quiz."))
        actions.append(self.input_action)
        state_id = f"{prefix_state_id}_{self.id}_final_state"
        return State(state_id=state_id, actions=actions)
    
    def get_correct_option_state(self, prefix_state_id, state_id_append):
        """
        Returns the state for the correct option.
        """
        actions = []
        actions.append(TalkAction(text="Congratulations! You have selected the correct option."))
        actions.append(TalkAction(text="You have earned 5 points."))
        actions.append(TalkAction(text="Press 1 to continue to the next question."))
        actions.append(self.input_action)
        state_id = f"{prefix_state_id}_correct_state_{state_id_append}" 
        post_operation = QuizPostStateOperation(score=self.positiveMarks)
        return State(state_id=state_id, actions=actions, post_operation=post_operation)
    
    def get_incorrect_option_state(self, prefix_state_id, state_id_append, correct_option_text):
        """
        Returns the state for the incorrect option.
        """
        actions = []
        actions.append(TalkAction(text="Sorry, that is incorrect."))
        actions.append(TalkAction(text=f"The correct option is {correct_option_text}."))
        actions.append(TalkAction(text="Press 1 to continue to the next question."))
        actions.append(self.input_action)
        state_id = f"{prefix_state_id}_incorrect_state_{state_id_append}" 
        post_operation = QuizPostStateOperation(score=-self.negativeMarks)
        return State(state_id=state_id, actions=actions, post_operation=post_operation)
        
    

# Example usage



# quiz.display_quiz()


