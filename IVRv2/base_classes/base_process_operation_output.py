from abc import ABC, abstractmethod
from base_classes.action import Action
from fsm.state import State
from utils.model_classes import IVRCallStateMongoDoc

class ProcessOperationOutput(ABC):
    @abstractmethod
    def execute(self, state: State, op_output, fsm_state_doc: IVRCallStateMongoDoc = None) -> [Action]:
        pass
    
    def __repr__(self):
        return self.__str__()

    def to_json(self):
        """Serializes the object to a JSON string."""
        # Convert object properties to a dictionary
        return {
            '__class__': self.__class__.__name__,
            '__module__': self.__class__.__module__,
            'attributes': vars(self)
        }

    @staticmethod
    def from_json(data: dict):
        """Deserializes JSON Object to an `Action` object."""
        # Fetch the class dynamically from the class name and module
        module = __import__(data['__module__'], fromlist=[data['__class__']])
        cls = getattr(module, data['__class__'])
        # Create an instance of the class using the stored attributes
        obj = cls.__new__(cls)  # Create an empty instance without calling __init__
        obj.__dict__.update(data['attributes'])
        return obj
    