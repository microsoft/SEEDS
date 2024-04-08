pullMenuMainUrl = "https://seedsblob.blob.core.windows.net/pull-model-menus/"
seeds_content_url = 'https://seeds-teacherapp.azurewebsites.net/content'
seeds_content_headers = {
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
  'music':'readingContentTitlesDialog/{language}/music/{speechRate}.mp3',
  'snippet':'readingContentTitlesDialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'readingContentTitlesDialog/{language}/riddle/{speechRate}.mp3',
  'quiz':'readingContentTitlesDialog/{language}/quiz/{speechRate}.mp3',
  'scramble':'readingContentTitlesDialog/{language}/scramble/{speechRate}.mp3',
  'theme':'readingContentTitlesDialog/{language}/theme/{speechRate}.mp3'
}


next4MessageUrls = {
  'story':'next4Dialog/{language}/story/{speechRate}.mp3',
  'poetry':'next4Dialog/{language}/poetry/{speechRate}.mp3',
  'music':'next4Dialog/{language}/music/{speechRate}.mp3',
  'scramble':'next4Dialog/{language}/scramble/{speechRate}.mp3',
  'quiz':'next4Dialog/{language}/quiz/{speechRate}.mp3',
  'snippet':'next4Dialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'next4Dialog/{language}/riddle/{speechRate}.mp3',
  'experience':'next4Dialog/{language}/experience/{speechRate}.mp3',
  'theme':'next4Dialog/{language}/theme/{speechRate}.mp3'
}

prev4MessageUrls = {
  'story':'prev4Dialog/{language}/story/{speechRate}.mp3',
  'poetry':'prev4Dialog/{language}/poetry/{speechRate}.mp3',
  'music':'prev4Dialog/{language}/music/{speechRate}.mp3',
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
    'poetry',
    'music',
    'snippet',
    'riddle'
  ]
}

experienceDialogAudioUrls = {
  'story':pullMenuMainUrl + 'experiencesDialog/{language}/story/For%20Stories/{speechRate}.mp3',
  'poetry':pullMenuMainUrl + 'experiencesDialog/{language}/poetry/For%20Rhymes/{speechRate}.mp3',
  'music':pullMenuMainUrl + 'experiencesDialog/{language}/music/For%20Songs/{speechRate}.mp3',
  'keyLearning':pullMenuMainUrl + 'experiencesDialog/{language}/keyLearning/to%20learn%20phone%20keys/{speechRate}.mp3',
  'scramble':pullMenuMainUrl + 'experiencesDialog/{language}/scramble/to%20play%20Scramble%20Game/{speechRate}.mp3',
  'quiz':pullMenuMainUrl + 'experiencesDialog/{language}/quiz/to%20play%20quiz/{speechRate}.mp3',
  'snippet': pullMenuMainUrl + 'experiencesDialog/{language}/snippet/For%20Snippets/{speechRate}.mp3',
  'riddle': pullMenuMainUrl + 'experiencesDialog/{language}/riddle/For%20Riddles/{speechRate}.mp3'
}

repeatCurrentMenuUrl = 'repeatMenuDialog/{language}/To%20repeat%20Current%20Menu/{speechRate}.mp3'
goToPreviousMenuMessageUrl = 'previousMenuDialog/{language}/To%20go%20to%20Previous%20Menu/{speechRate}.mp3'


pressKeyMessageUrl = 'pressKeysDialog/{language}/{key}/{speechRate}.mp3'

audioGoingTobePlayedDialogUrl = 'audioDialogs/{language}/audioGoingToBePlayedDialog/{speechRate}.mp3'
audioFinishedMessageUrl = 'audioDialogs/{language}/audioFinishedDialog/{speechRate}.mp3'

