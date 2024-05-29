from typing import List
import uuid
from pydantic import BaseModel, Field

class URLTextEntity(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    url: str
    text: str

class QuizQuestion(BaseModel):
    question: URLTextEntity
    options: List[URLTextEntity]
    correct_option_id: str
    
class QuizData(BaseModel):
    id: str = Field(..., alias="_id")
    language: str
    theme: str
    themeAudio: str
    title: str
    localTitle: str
    titleAudio: str
    localTitle: str
    positiveMarks: int = 1
    negativeMarks: int = 0
    questions: List[QuizQuestion] = []

    def dict(self, **kwargs):
        # Ensure that sub-models also serialize recursively with their custom dict methods
        data_dict = super().dict(by_alias=True, **kwargs)
        # Customize the serialization for nested models if needed
        # data_dict['title'] = self.title.dict(**kwargs)
        data_dict['questions'] = [item.dict(**kwargs) for item in self.questions]
        return data_dict

    class Config:
        # This will allow the model to be instantiated with 'id' instead of '_id'
        populate_by_name = True