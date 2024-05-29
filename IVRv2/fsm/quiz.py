from actions.base_actions.talk_action import TalkAction
from actions.base_actions.stream_action import StreamAction
from actions.base_actions.input_action import InputAction

from fsm.operations.quiz_post_state_operation import QuizPostStateOperation
from fsm.operations.quiz_init_state_operation import QuizInitStateOperation
from fsm.operations.quiz_pre_state_operation import QuizPreStateOperation
from fsm.operations.quiz_process_state_output import QuizProcessFinalStateOutput

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
from utils.model_classes import Menu
from utils.model_classes import Option
from utils.quiz_model_classes import QuizData
from utils.quiz_model_classes import QuizQuestion
from utils.quiz_model_classes import URLTextEntity

load_dotenv()

class Quiz:
    def __init__(self, quiz_data: QuizData):
        """
        Initializes a new Quiz instance.

        :param title: str - the title of the quiz
        :param quiz_id: str - a unique identifier for the quiz
        :param positive_marks: int - the score for a correct answer in the quiz
        :param negative_marks: int - the penalty for a wrong answer in the quiz
        :param questions: list - a list of dictionaries, each representing a question with its text, a URL,
                                 and its options.
        """
        self.quiz_data = quiz_data
        self.input_action = InputAction(type_=["dtmf"], eventApi='/input')
        self.move_forward_key = "1"
        self.type = "quiz"

    
    def display_quiz(self):
        """
        Prints the quiz details, including questions and their options.
        """
        print(f"Quiz: {self.quiz_data.title} (ID: {self.quiz_data.id})")
        print(f"Language: {self.quiz_data.language}")
        print(f"Theme: {self.quiz_data.theme}")
        print(f"Positive Marks per correct answer: {self.quiz_data.positiveMarks}")
        print(f"Negative Marks per incorrect answer: {self.quiz_data.negativeMarks}")
        for question in self.quiz_data.questions:
            print(f"\nQuestion: {question.question.text} (URL: {question.question.url})")
            print("Options:")
            for option in question.options:
                print(option.text)
                
            correct_option = next(opt for opt in question.options if opt.id == question.correct_option_id)
            print(f"Correct Option: {correct_option.text}\n")
            
    def generate_states(self, fsm, prefix_state_id, parent_block_state_id, key_chosen, level):
        initial_quiz_state_id = f"{prefix_state_id}_QuizStart"
        
        initial_state = self.get_initial_state(initial_quiz_state_id, level)
        fsm.add_state(initial_state)
        
        parent_to_initial_transition = Transition(input=str(key_chosen), source_state_id=parent_block_state_id, dest_state_id=initial_quiz_state_id)
        fsm.add_transition(parent_to_initial_transition)
        
        mapping_questionids_common_ids = []
        
        for index, question in enumerate(self.quiz_data.questions):
            print("INSIDE LOOP")
            question_state_id = f"{prefix_state_id}_Question{index+1}"
            actions = [StreamAction(url=question.question.url)]
            transitions = []
            mapping = {'question_id': question_state_id}
            cor_or_wrong_state_ids = []
            # actions = []
            correct_option_text = next(opt.text for opt in question.options if opt.id == question.correct_option_id)
            
            for index_option, option in enumerate(question.options):
                actions.append(StreamAction(url=option.url))
                if option.id == question.correct_option_id:
                    correct_state = self.get_correct_option_state(prefix_state_id, "Q" + str(index+1)+ "-O" + str(index_option+1))
                    # menu = Menu(description=f"Question {index+1} Correct State", options=[Option(key=1, value="Continue")], level=level+2)
                    # correct_state.menu = menu
                    fsm.add_state(correct_state)
                    cor_or_wrong_state_ids.append(correct_state.id)
                    transition_from_question_to_correct = Transition(input=str(index_option+1), source_state_id=question_state_id, dest_state_id=correct_state.id)
                    transitions.append(transition_from_question_to_correct)
                else:
                    incorrect_state = self.get_incorrect_option_state(prefix_state_id, "Q" + str(index+1)+ "-O" + str(index_option+1), correct_option_text)
                    # menu = Menu(description=f"Question {index+1} Incorrect State", options=[Option(key=1, value="Continue")], level=level+2)
                    # incorrect_state.menu = menu
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
            # options_for_menu = [Option(key=0, value=question['question']['text'])]
            # options_for_menu += [Option(key=i+1, value=option['text']) for i, option in enumerate(question['options'])]
            # options_for_menu.append(Option(key=8, value="Repeat Question"))
            
            # menu = Menu(description=f"Question {index+1}", options=options_for_menu, level=level+2)
            question_state = State(state_id=question_state_id, actions=actions)
            # print("QUESTION_STATE", question_state)
            fsm.add_state(question_state)
            # print("YOYO")
            repeat_question_transition = Transition(input="8", source_state_id=question_state_id, dest_state_id=question_state_id)
            for transition in transitions:
                fsm.add_transition(transition)
            fsm.add_transition(repeat_question_transition)
           
        
        final_state = self.get_final_state(prefix_state_id, level)
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
    
    
    def get_initial_state(self, initial_quiz_state_id, level):
        """
        Returns the initial state of the quiz.
        """
        actions = []
        actions.append(TalkAction(text=f"Welcome to the {self.quiz_data.title} quiz!"))
        actions.append(TalkAction(text=f"Let's get started!"))
        actions.append(TalkAction(text=f"Press 1 to start the quiz."))
        actions.append(self.input_action)
        post_operation = QuizInitStateOperation()
        options = [Option(key=1, value="Start Quiz")]
        description = f"{self.quiz_data.title} welcome state"
        menu = Menu(description=description, options=options, level=level)
       
        initial_state = State(state_id=initial_quiz_state_id, actions=actions, post_operation=post_operation, menu=menu)
        
        return initial_state
    
    def get_final_state(self, prefix_state_id, level):
        actions = []
        actions.append(TalkAction(text="Congratulations! You have completed the quiz."))
        actions.append(TalkAction(text="Press 1 to exit the quiz."))
        actions.append(self.input_action)
        state_id = f"{prefix_state_id}_{self.quiz_data.id}_final_state"
        process_operation_output_into_actions = QuizProcessFinalStateOutput()
        # menu = Menu(description=f"{self.title} final state", options=[Option(key=1, value="Exit")], level=level+3)
        return State(state_id=state_id, actions=actions, process_operation_output_into_actions = process_operation_output_into_actions)
    
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
        post_operation = QuizPostStateOperation(score=self.quiz_data.positiveMarks)
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
        post_operation = QuizPostStateOperation(score=-self.quiz_data.negativeMarks)
        return State(state_id=state_id, actions=actions, post_operation=post_operation)
        
    

# Example usage



# quiz.display_quiz()



# quiz_data = QuizData(**quiz)
# quiz = Quiz(quiz_data)
# quiz.display_quiz()
# print(quiz.quiz_data.dict())



