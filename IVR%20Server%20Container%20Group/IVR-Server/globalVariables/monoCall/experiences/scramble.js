global.poems = [
  "Johny Johny Yes Papa"
]

global.poemToId = {
  "Johny Johny Yes Papa":"23"
}

global.poemIdToNumberOfLines = {
  "23":4
}

global.poemIdToBlobUrl = {
  "23":"https://seedsblob.blob.core.windows.net/output-container/23/1.0.wav",
  "23_1":"https://seedsblob.blob.core.windows.net/output-container/23_1/1.0.wav",
  "23_2":"https://seedsblob.blob.core.windows.net/output-container/23_2/1.0.wav",
  "23_3":"https://seedsblob.blob.core.windows.net/output-container/23_3/1.0.wav",
  "23_4":"https://seedsblob.blob.core.windows.net/output-container/23_4/1.0.wav",
  
}

global.poemTitleToAudioMessageUrl = {
  "Johny Johny Yes Papa":'https://seedsblob.blob.core.windows.net/pull-model-menus/DynamicContent/english/forMessage/scramble/For%20Johny%20Johny%20Yes%20Papa/{speechRate}.mp3'
}

// global.repeatingContent = {
//   'english': "We are playing the entire Poem again, try to answer this time correctly.",
//   'kannada':"ನಾವು ಸಂಪೂರ್ಣ ಕವಿತೆಯನ್ನು ಮತ್ತೊಮ್ಮೆ ಆಡುತ್ತಿದ್ದೇವೆ, ಈ ಬಾರಿ ಸರಿಯಾಗಿ ಉತ್ತರಿಸಲು ಪ್ರಯತ್ನಿಸಿ."
// }

global.scrambleWelcomeMessageUrl = 'experienceSpecificDialog/{language}/scramble/welcomeMessage/{speechRate}.mp3'
global.scrambleChoosePoemsMessageUrl = 'experienceSpecificDialog/{language}/scramble/choosePoemsMessage/{speechRate}.mp3'

global.scramblePressAnyKeyBetweenMessageUrl = 'experienceSpecificDialog/{language}/scramble/pressAnyKeyBetweenDialogs/Press%20Any%20Key%20Between%201%20to%20upperLimit/{speechRate}.mp3'
global.toListenToMappedLinesMessageUrl = 'experienceSpecificDialog/{language}/scramble/listenToMappedLines/{speechRate}.mp3'
global.guessSequenceMessageUrl = 'experienceSpecificDialog/{language}/scramble/guessSequence/{speechRate}.mp3'
global.messageForCorrectSequenceUrl = 'experienceSpecificDialog/{language}/scramble/messageForCorrectSequence/{speechRate}.mp3'
global.takeToScrambleMainMenuMessageUrl = 'experienceSpecificDialog/{language}/scramble/takeToMainMenu/{speechRate}.mp3'
global.enteredSequenceMessageUrl = 'experienceSpecificDialog/{language}/scramble/enteredSequence/{speechRate}.mp3'
global.wrongMessageUrl = 'experienceSpecificDialog/{language}/scramble/wrong/{speechRate}.mp3'
global.enterSequenceAgainMessageUrl = 'experienceSpecificDialog/{language}/scramble/enterSequenceAgain/{speechRate}.mp3'
global.replayContentMessageUrl = 'experienceSpecificDialog/{language}/scramble/replayContent/{speechRate}.mp3'
