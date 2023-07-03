// for monoCall.
global.maxTimeLimitForUserInMilliseconds = 46 * 60 * 1000
global.timeInMillisecondsToUpdateUserTimeInDB = 2 * 60 * 1000
global.inactiveTimeLimitForUserInPullModel = 3 * 60 * 1000
global.phoneNumberToLogCollectionId = {}
global.monoCallInfo = {}
global.fsmIdToPhoneNumber = {}
global.pullMenuMainUrl = "https://seedsblob.blob.core.windows.net/pull-model-menus/"
global.silenceStreamUrl = "https://seedsblob.blob.core.windows.net/pull-model-menus/silence/2-sec/1.0.mp3"

global.IVRExperienceNameToSEEDSServerExperienceName = {
  'story':'Story',
  'poetry':'Poem',
  'music':'Song',
  'snippet': 'Snippet',
  'riddle': 'Riddle'
}

global.verbalLanguageToIVRTTSLanguageCode = {
  'english':'en-IN',
  'kannada':'kn-IN',
  'bengali':'bn-IN'
}

global.initialLanguage = 'english'
global.initialSpeechRateIndex = 2
global.initialTTSLanguage = verbalLanguageToIVRTTSLanguageCode[initialLanguage]

global.languageDialogUrls = {
  'english':'languageDialog/english/For%20English/{speechRate}.mp3',
  'kannada':'languageDialog/kannada/For%20Kannada/{speechRate}.mp3',
  'bengali':'languageDialog/bengali/For%20Bengali/{speechRate}.mp3'
}
global.availableLanguages = ["english","kannada","bengali"]

global.incrementSpeechRateDialogUrl = 'speechRateDialog/{language}/To%20increase%20Speech%20Rate/{speechRate}.mp3'
global.decrementspeechRateDialogUrl = 'speechRateDialog/{language}/To%20decrease%20Speech%20Rate/{speechRate}.mp3'

global.repeatCurrentMenuUrl = 'repeatMenuDialog/{language}/To%20repeat%20Current%20Menu/{speechRate}.mp3'

global.welcomeMessageUrl = 'welcomeDialog/{language}/welcome%20to%20SEEDS/{speechRate}.mp3'

global.speechRateSettingDialogUrl = 'speechRateSettingDialog/{language}/{speechRate}/{speechRate}.mp3'

global.next4MessageUrls = {
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

global.prev4MessageUrls = {
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

global.goToPreviousMenuMessageUrl = 'previousMenuDialog/{language}/To%20go%20to%20Previous%20Menu/{speechRate}.mp3'

global.experienceNames = {
  'english':[
    'story',
    'poetry',
    'music',
    // 'keyLearning',
    // 'scramble',
    // 'quiz',
    'snippet',
    'riddle'
  ]
}

global.experienceDialogAudioUrls = {
  'story':global.pullMenuMainUrl + 'experiencesDialog/{language}/story/For%20Stories/{speechRate}.mp3',
  'poetry':global.pullMenuMainUrl + 'experiencesDialog/{language}/poetry/For%20Rhymes/{speechRate}.mp3',
  'music':global.pullMenuMainUrl + 'experiencesDialog/{language}/music/For%20Songs/{speechRate}.mp3',
  'keyLearning':global.pullMenuMainUrl + 'experiencesDialog/{language}/keyLearning/to%20learn%20phone%20keys/{speechRate}.mp3',
  'scramble':global.pullMenuMainUrl + 'experiencesDialog/{language}/scramble/to%20play%20Scramble%20Game/{speechRate}.mp3',
  'quiz':global.pullMenuMainUrl + 'experiencesDialog/{language}/quiz/to%20play%20quiz/{speechRate}.mp3',
  'snippet': global.pullMenuMainUrl + 'experiencesDialog/{language}/snippet/For%20Snippets/{speechRate}.mp3',
  'riddle': global.pullMenuMainUrl + 'experiencesDialog/{language}/riddle/For%20Riddles/{speechRate}.mp3'
}

global.pressKeyMessageUrl = 'pressKeysDialog/{language}/{key}/{speechRate}.mp3'

global.readingExperienceMainMenuUrl = {
  'story': 'readingExperienceMainMenuDialog/{language}/story/{speechRate}.mp3',
  'poetry':'readingExperienceMainMenuDialog/{language}/poetry/{speechRate}.mp3',
  'music':'readingExperienceMainMenuDialog/{language}/music/{speechRate}.mp3',
  'snippet':'readingExperienceMainMenuDialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'readingExperienceMainMenuDialog/{language}/riddle/{speechRate}.mp3'
}

global.audioExperienceNames = ["story","poetry","music",'snippet']

global.contentListDialogUrl = {
  'story':'contentListDialog/{language}/story/To%20List%20Stories/{speechRate}.mp3',
  'poetry':'contentListDialog/{language}/poetry/To%20List%20Poems/{speechRate}.mp3',
  'music':'contentListDialog/{language}/music/To%20List%20Songs/{speechRate}.mp3',
  'snippet':'contentListDialog/{language}/snippet/To%20List%20Snippets/{speechRate}.mp3',
  'riddle':'contentListDialog/{language}/riddle/To%20List%20Riddles/{speechRate}.mp3'
}

global.readingContentTitlesDialogUrl = {
  'story':'readingContentTitlesDialog/{language}/story/{speechRate}.mp3',
  'poetry':'readingContentTitlesDialog/{language}/poetry/{speechRate}.mp3',
  'music':'readingContentTitlesDialog/{language}/music/{speechRate}.mp3',
  'snippet':'readingContentTitlesDialog/{language}/snippet/{speechRate}.mp3',
  'riddle':'readingContentTitlesDialog/{language}/riddle/{speechRate}.mp3',
  'quiz':'readingContentTitlesDialog/{language}/quiz/{speechRate}.mp3',
  'scramble':'readingContentTitlesDialog/{language}/scramble/{speechRate}.mp3',
  'theme':'readingContentTitlesDialog/{language}/theme/{speechRate}.mp3'
}

global.audioGoingTobePlayedDialogUrl = 'audioDialogs/{language}/audioGoingToBePlayedDialog/{speechRate}.mp3'
global.helpDialogWhileAudioStreamingUrl = 'audioDialogs/{language}/helpDialog/{speechRate}.mp3'
global.audioControlDialogUrl = 'audioDialogs/{language}/audioControlDialog/{speechRate}.mp3'
global.audioPausedMessageUrl = 'audioDialogs/{language}/audioPausedDialog/{speechRate}.mp3'
global.audioFinishedMessageUrl = 'audioDialogs/{language}/audioFinishedDialog/{speechRate}.mp3'

// global.experience = {
//   'kannada':{
//     'story':`ಕಥೆಯ`,
//     'poetry':`ಪದ್ಯದ`,
//     'music':`ಹಾಡಿನ`
//   }
// }

// global.digitTranslation = {
//   'kannada':{
//     '1':'ಒಂದನ್ನು',
//     '2':'ಎರಡನ್ನು',
//     '3':'ಮೂರನ್ನು',
//     '4':'ನಾಲ್ಕನ್ನು'
//   }
// }

global.internalErrorMessageUrl = 'internalErrorMessage/{language}/some%20internal%20error%20occurred%2C%20please%20try%20again%20later/{speechRate}.mp3'

global.press1Url = "pressKeysDialog/{language}/1/{speechRate}.mp3"
global.press2Url = "pressKeysDialog/{language}/2/{speechRate}.mp3"
global.press3Url = "pressKeysDialog/{language}/3/{speechRate}.mp3"
global.press4Url = "pressKeysDialog/{language}/4/{speechRate}.mp3"
global.press8Url = "pressKeysDialog/{language}/8/{speechRate}.mp3"
global.press9Url = "pressKeysDialog/{language}/9/{speechRate}.mp3"
global.pressHashUrl = "pressKeysDialog/{language}/%23/{speechRate}.mp3"
global.pressStarUrl = "pressKeysDialog/{language}/*/{speechRate}.mp3"

global.chosenNoOptionUrl = "chosenNoOptionDialog/{language}/Sorry%2C%20you%20have%20not%20chosen%20any%20option/{speechRate}.mp3"
global.chosenWrongOptionUrl = "chosenWrongOptionDialog/{language}/Sorry%2C%20you%20have%20chosen%20the%20wrong%20option/{speechRate}.mp3"
