import asyncio
import aiohttp
import os
import json

async def get_latest_content():
    api_url = os.environ.get("SEEDS_SERVER_BASE_URL", "") + "content"
    headers = {
        'authToken': 'postman'
    }
    async with aiohttp.ClientSession() as session:
        async with session.get(api_url, headers=headers) as response:
            response_data = await response.text()  # Get response text
            contents = json.loads(response_data)   # Parse text to JSON
    
    # Filter isPullModel and isDeleted content
    filtered_content = [
        x for x in contents
        if all(key in x for key in ["isPullModel", "isDeleted", "isProcessed"]) and
        x["isPullModel"] and x["isProcessed"] and not x["isDeleted"]
    ]
    return filtered_content

def process_content(content):
    unique_languages = set([item['language'] for item in content])
    count_languages = {lang: len([item for item in content if item['language'] == lang]) for lang in unique_languages}
    sorted_languages = sorted(count_languages.items(), key=lambda item: item[1], reverse=True)
    
    language_to_theme = {}
    for language, count in sorted_languages:
        lang_content = [x for x in content if x['language'].lower() == language.lower()]
        themes = set([item['theme'].lower() for item in lang_content])
        theme_to_experience = {}
        for theme in themes:
            theme_content = [x for x in lang_content if x['theme'].lower() == theme]
            experiences = set([x['type'].lower() for x in theme_content])
            experience_to_title = {}
            for experience in experiences:
                experience_content = [x for x in theme_content if x['type'].lower() == experience.lower()]
                titles = set([x['title'] for x in experience_content])
                experience_to_title[experience] = titles
            theme_to_experience[theme] = experience_to_title
        language_to_theme[language.lower()] = theme_to_experience
    return language_to_theme
    
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

