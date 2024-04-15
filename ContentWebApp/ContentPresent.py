import csv
import requests

# Function to check if a file exists at a given URL
def file_exists(url):
    response = requests.head(url)
    return response.status_code == 200

# Path to your CSV file
def fileExistence():
    # Read the CSV file
    file_exists_count = 0


    with open('mongoDbContent.csv', newline='', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            story_id = row['id']
            story_type = row['type']


            # Construct the URLs
            src = f'https://seedscontent.blob.core.windows.net/output-original/{story_id}.mp3'
            answer_src = f'https://seedscontent.blob.core.windows.net/output-original/{story_id}/answer.mp3'
            if story_type == 'Riddle':
                src = f'https://seedscontent.blob.core.windows.net/output-original/{story_id}/question.mp3'

            # Check if files exist
            
            if file_exists(src):
                # print(f'File exists at {src}')
                pass
            else:
                print(f'File does not exist at {src}')

            if story_type == 'Riddle':
                if file_exists(answer_src):
                    pass
                    # print(f'File exists at {answer_src}')
                else:
                    print(f'File does not exist at {answer_src}')



def read_titles_from_csv(file_path, title_column_name):
    titles = set()
    with open(file_path, newline='', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            titles.add(row[title_column_name].strip().lower())
    return titles

# Paths to your CSV files
mongo_db_contents_file = 'mongoDbContent.csv'  # replace with your actual file path
all_contents_file = 'allContents.csv'          # replace with your actual file path

# Column name for titles in both CSVs
title_column_name = 'title'  # Replace with the actual column name for titles

# Read titles from both files
mongo_db_titles = read_titles_from_csv(mongo_db_contents_file, title_column_name)
all_contents_titles = read_titles_from_csv(all_contents_file, title_column_name)

# Find titles in allContents.csv not present in mongoDbContents.csv
missing_titles = all_contents_titles - mongo_db_titles

missing_titles_from_logs = ['Chicken sound', 'Champa and her stage fear', 'Ba Ba Maleraya', 'Tanu learns about her body special', 'Sense things with other Organs ', 'Vehicle sound- Aeroplane', 'Achchu belladachchu', 'Tandanano', 'Animalsound - Labrador', 'Vehicle sound - Firetruck', 'Jokhu mathu jokini', 'Riddles - food 2', 'How are you song.', 'Riddles - food 3', 'Riddles - food 4', 'Papluvina prashnegLu', 'Yako Gotilla', 'Know your body parts', 'Fear of the dark - ಮನೆಯಲ್ಲಿ ಯಾರೂ ಇಲ್ಲ', 'About SEEDS', 'Cat Meow', "Chinu's friends save the day", 'Animal Sound - Mouse', 'Beralugala Kutumba Ridd', 'I am ', 'Why burping?', 'Uma mathe uppittu', 'Crow Sound', 'Yaradu', 'Street sound - unknown', 'Jogada Siri Belakinalli', 'Family Riddle 3', 'Beke beke tarkari', 'COW - Moo']

# strip and lowercase the titles
missing_titles_from_logs = set([title.strip().lower() for title in missing_titles_from_logs])

# Print or process the missing titles
for title in missing_titles:
    print(title)

print('---------------------')

miss = missing_titles_from_logs - missing_titles

common = missing_titles_from_logs.intersection(missing_titles)
print('Common')
for title in common:
    print(title)

print('---------------------')

print('Missing from logs')
for title in miss:
    print(title)