import datetime
from enum import Enum
from json import JSONEncoder


def format_data_html(data, level=0):
    if isinstance(data, dict):
        role = 'group' if level > 0 else 'tree'  # Use 'tree' role for the top level and 'group' for nested lists
        items = f'<ul role="{role}">'
        for key, value in data.items():
            # Using 'treeitem' role for items and specifying 'aria-level' for better depth understanding
            items += f'<li role="treeitem" aria-level="{level + 1}"><strong>{key}</strong>{format_data_html(value, level + 1)}</li>'
        items += '</ul>'
        return items
    elif isinstance(data, set):
        # Leaf nodes are simple list items without a specific role needed as they do not expand further
        items = '<ul>'
        for item in data:
            items += f'<li>{item}</li>'
        items += '</ul>'
        return items
    return ''

class CustomJSONEncoder(JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Enum):
            return obj.value
        elif isinstance(obj, datetime.datetime):
            utc_datetime = obj.astimezone(datetime.timezone.utc)
            return utc_datetime.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
        return JSONEncoder.default(self, obj)