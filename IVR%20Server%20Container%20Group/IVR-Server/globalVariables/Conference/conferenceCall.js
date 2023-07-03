// for Conference.
global.milliSecondsToWaitToReleaseTheLockOfAPhoneNumberFromAConference = 60000
global.phoneNumberToConfId = {}
global.confIdToTeacherNumber = {}
global.confIdToTeacherName = {}
global.confIdToLeaderPhoneNumber = {}
global.confIdToIntendedLeaderPhoneNumber = {}
global.confIdToEndConferenceTimeoutId = {}
global.confIdToLeaderName = {}
global.confIdToMusicState = {}
global.digitToAction = {
  "1":"backwardStream",
  "2":["pause","resume"],
  "3":"forwardStream",
  "4":"muteAll",
  "6":"unMuteAll",
  "8":"play",
  "#":"incrementSpeechRate",
  "*":"decrementSpeechRate",
}
global.audioControlDigits = ["1","2","3","8","*","#"]
global.nonAudioControls = new Set([
  "end","muteAll","unMuteAll"
])
global.audioControls = new Set([
  "play","pause","resume",
  "forwardStream","backwardStream",
  "incrementSpeechRate","decrementSpeechRate"
])