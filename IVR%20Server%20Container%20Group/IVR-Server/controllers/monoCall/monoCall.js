const axios = require('axios');
// const { checkSlotAndQueue } = require("../../handlingRequests");
const { getTalkAction, DTMFInputAction, getStreamAction } = require("../../nccoActions")
const { prepareNext4MenuContent, checkCommonDTMF, handleCommonDTMF, storeLog,isValidRequest, getCurrentAudioExperienceObjectInMonoCall, createUserSettingsDocInDB, updateUserSettingsDocInDB, setCurrentSpeechRateIndex } = require("./utils");
const { Story } = require("./experiences/story")
const { Poetry } = require("./experiences/poetry")
const { Music } = require("./experiences/music")
const { Snippet } = require("./experiences/snippet")
const Riddle = require("./experiences/riddle");
const { Quiz } = require("./experiences/quiz");
const { KeyLearning } = require('./experiences/keyLearning');
const { ScrambleGame } = require('./experiences/scramble');
const userSpentTimeForPullModel = require("../../models/userSpentTimeForPullModel")
const monoCallLog = require("../../models/monoCallLog")
const pullModelInsights = require("../../models/pullModelInsights")
const pullModelUserSettings = require("../../models/pullModelUserSettings")

// It prepares the content for language menu
function prepareContentForLanguageMenu(userPhoneNumber){
  global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'language'
  const endpoint = global.communicationApi.getEndPointForLanguageInput()
  global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint

  const currentSpeechRate = global.speechRates[global.initialSpeechRateIndex]
  const priorMessageUrl = global.welcomeMessageUrl.replace('{language}',global.initialLanguage).replace('{speechRate}',currentSpeechRate)
  const priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)

  const languageMenus = []
  for(let i=0;i<global.availableLanguages.length;i+=1){
    const language = global.availableLanguages[i]
    languageMenus.push(getStreamAction(global.pullMenuMainUrl + global.languageDialogUrls[language].replace('{speechRate}',currentSpeechRate)))
    languageMenus.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,(i+1).toString()).replace('{speechRate}',currentSpeechRate)))
  }

  global.monoCallInfo[userPhoneNumber]['currentContentActions'] = [
    ...languageMenus,
    getStreamAction(global.pullMenuMainUrl + global.repeatCurrentMenuUrl.replace('{language}',global.initialLanguage).replace('{speechRate}',currentSpeechRate)),
    getStreamAction(global.pullMenuMainUrl + global.press8Url.replace('{language}',global.initialLanguage).replace('{speechRate}',currentSpeechRate))
  ]
  

  const actions = [
      priorMessageAction,
      ...global.monoCallInfo[userPhoneNumber]['currentContentActions'],
      DTMFInputAction(endpoint)
      // {
      //     action: "record",
      //     eventUrl:[global.remoteUrl + "mono_call/recording"]
      // },
    ];
  
  return actions
}

// It handles the input from user during Language Menu
async function handleLanguageInput(params){
  const {userPhoneNumber,digits} = params
  //handling if the abrupts the call.
  if(!isValidRequest(userPhoneNumber)){
    return []
  }
  
  const digitToLanguage = {}
  for(let i=0;i<global.availableLanguages.length;i+=1){
    digitToLanguage[(i+1).toString()] = global.availableLanguages[i]
  }

  console.log(`digits received for language input, = ${digits}`)
  if(checkCommonDTMF({...params,validDigits:digitToLanguage})){
    return await handleCommonDTMF({...params,validDigits:digitToLanguage})
  }
  else if(digits === "9"){
    return await handleCommonDTMF({...params,validDigits:digitToLanguage})
  }
  else{
    const language = digitToLanguage[digits]
    var speechRate = global.speechRates[global.initialSpeechRateIndex]
    if(global.monoCallInfo[userPhoneNumber]['speechRateIndex'].hasOwnProperty(language)){
      speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
    }
    else{
      const user = await pullModelUserSettings.findOne(
        {
          phoneNumber: userPhoneNumber
        }
      )
      if(user === null){
        const newUserSettings = {
          phoneNumber: userPhoneNumber,
          languageLevelSettings: [
            {
              language: language,
              speechRateIndex: global.initialSpeechRateIndex
            }
          ]
        }
        await createUserSettingsDocInDB(newUserSettings)
        global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = global.initialSpeechRateIndex
      }
      else{
        const languageLevelSettings = user.languageLevelSettings
        var currentLanguageSetting = languageLevelSettings.find((languageSetting) => languageSetting.language === language)
        if(currentLanguageSetting === undefined){
          currentLanguageSetting = {
            language: language,
            speechRateIndex: global.initialSpeechRateIndex
          }
          languageLevelSettings.push(currentLanguageSetting)
          await updateUserSettingsDocInDB(userPhoneNumber,{languageLevelSettings})
          global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = global.initialSpeechRateIndex
        }
        else{
          global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = currentLanguageSetting.speechRateIndex
          speechRate = global.speechRates[currentLanguageSetting.speechRateIndex]
        }
      }
    }

    await storeLog(userPhoneNumber,digits,`chosen ${language} Language`)
    const currentUserObj = global.monoCallInfo[userPhoneNumber]
    currentUserObj.language = language
    currentUserObj.themes = []
    currentUserObj.themeTitleAudioUrl = {}
    const resp = await axios.get(global.contentUrl+`/themes?language=${language}`,{headers:{ authToken:'postman'}})
    const themes = resp.data

    for(const theme of themes){
      currentUserObj.themes.push(theme.name)
      currentUserObj.themeTitleAudioUrl[theme.name] = theme.audioUrl + '/{speechRate}.mp3'
    }

    currentUserObj.currentItemStartingIndex = 0
    prepareNext4MenuContent(currentUserObj,currentUserObj.themes,currentUserObj.themeTitleAudioUrl,"theme")
    currentUserObj.currentContentActions.push(
      getStreamAction(global.pullMenuMainUrl + global.incrementSpeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
      getStreamAction(global.pullMenuMainUrl + global.pressHashUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
      getStreamAction(global.pullMenuMainUrl + global.decrementspeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
      getStreamAction(global.pullMenuMainUrl + global.pressStarUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
    )
    const priorMessageAction = getStreamAction(global.pullMenuMainUrl + global.readingContentTitlesDialogUrl["theme"].replace('{language}',language).replace('{speechRate}',speechRate))
    
    const endpoint = global.communicationApi.getEndPointForAudioContentListInput("theme")
    global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
    const actions = []
    actions.push(priorMessageAction)
    actions.push(...currentUserObj.currentContentActions)
    actions.push(DTMFInputAction(endpoint))

    return actions
      
  }
}

// It handles the input from user during Themes Menu
async function handleThemeTypeInput(params){
    const {userPhoneNumber,digits } = params
    //handling if the user abrupts the call.
    if(!isValidRequest(userPhoneNumber)){
      return []
    }
    
    const currentUserObj = global.monoCallInfo[userPhoneNumber]
    // below digits are valid in handing theme type input as we are giving incre/decre speechRate option in that menu.
    currentUserObj.keyMappings["*"] = ""
    currentUserObj.keyMappings["#"] = ""
    const language = currentUserObj.language
    const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
    
    if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
      return await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
    }
    else if(["#","*"].includes(digits)){
      if(digits === "#"){
        global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = Math.min(global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] + 1, 4)
      }
      else{
        global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = Math.max(global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] - 1, 0)
      }

      await setCurrentSpeechRateIndex(userPhoneNumber,global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language])
      const newSpeechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
      const priorMessageUrl = global.speechRateSettingDialogUrl.replace('{language}',language).replace(/{speechRate}/g,newSpeechRate)
      const priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)

      prepareNext4MenuContent(currentUserObj,currentUserObj.themes,currentUserObj.themeTitleAudioUrl,"theme")
      currentUserObj.currentContentActions.push(
        getStreamAction(global.pullMenuMainUrl + global.incrementSpeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',newSpeechRate)),
        getStreamAction(global.pullMenuMainUrl + global.pressHashUrl.replace('{language}',language).replace('{speechRate}',newSpeechRate)),
        getStreamAction(global.pullMenuMainUrl + global.decrementspeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',newSpeechRate)),
        getStreamAction(global.pullMenuMainUrl + global.pressStarUrl.replace('{language}',language).replace('{speechRate}',newSpeechRate)),
      )

      const actions = [priorMessageAction]
      actions.push(...currentUserObj.currentContentActions)
      actions.push(DTMFInputAction(currentUserObj.currentEndPoint))
      return actions
    }
    else{
      var actions = []
      switch(digits){
        case "1":
        case "2":
        case "3":
        case "4":
          const theme = currentUserObj.keyMappings[digits]
          await storeLog(userPhoneNumber,digits,`chosen ${theme} theme`)
          global.monoCallInfo[userPhoneNumber]['theme'] = theme

          currentUserObj.currentItemStartingIndex = 0
          prepareNext4MenuContent(global.monoCallInfo[userPhoneNumber],global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
          global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'experience'
          const endpoint = global.communicationApi.getEndPointForExperienceTypeInput()
          global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
          actions = [
            ...global.monoCallInfo[userPhoneNumber]['currentContentActions'],
            DTMFInputAction(endpoint)
          ]
          return actions
        case "5":
        case "7":
          if(digits === "5"){
            await storeLog(userPhoneNumber,digits,`chosen next 4 themes`)
            currentUserObj.currentItemStartingIndex += 4
          }
          else if(digits === "7"){
            await storeLog(userPhoneNumber,digits,`chosen previous 4 themes`)
            currentUserObj.currentItemStartingIndex -= 4
          }
          prepareNext4MenuContent(currentUserObj,currentUserObj.themes,currentUserObj.themeTitleAudioUrl,"theme")
          currentUserObj.currentContentActions.push(
            getStreamAction(global.pullMenuMainUrl + global.incrementSpeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
            getStreamAction(global.pullMenuMainUrl + global.pressHashUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
            getStreamAction(global.pullMenuMainUrl + global.decrementspeechRateDialogUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
            getStreamAction(global.pullMenuMainUrl + global.pressStarUrl.replace('{language}',language).replace('{speechRate}',speechRate)),
          )
          return [
            ...global.monoCallInfo[userPhoneNumber]['currentContentActions'],
            DTMFInputAction(global.monoCallInfo[userPhoneNumber]['currentEndPoint'])
          ]
        case "9":
          await storeLog(userPhoneNumber,digits,`chosen previous menu(language menu)`)
          global.monoCallInfo[userPhoneNumber]['language'] = global.initialLanguage
          actions = prepareContentForLanguageMenu(userPhoneNumber)
          return actions.slice(1)
      }
    }
}

// It handles the input from user during Experiences(Story/Poem/Song) Menu
async function handleExperienceTypeInput(params){
  const {userPhoneNumber,digits } = params
  //handling if the abrupts the call.
  if(!isValidRequest(userPhoneNumber)){
    return []
  }
  
  const currentUserObj = global.monoCallInfo[userPhoneNumber]
  const language = currentUserObj.language
  const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]

  if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
    return await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
  }
  else{
    var actions = []
    switch(digits){
        case "1":
        case "2":
        case "3":
        case "4":
          const experience = currentUserObj.keyMappings[digits]
          await storeLog(userPhoneNumber,digits,`chosen ${experience} experience`)
          global.monoCallInfo[userPhoneNumber]['experience'] = experience
          if(global.audioExperienceNames.includes(experience)){
            if(experience === "story"){
              global.monoCallInfo[userPhoneNumber]['audioExperienceObj'] = new Story(userPhoneNumber,experience,language)
            }
            else if(experience === "poetry"){
              global.monoCallInfo[userPhoneNumber]['audioExperienceObj'] = new Poetry(userPhoneNumber,experience,language)
            }
            else if(experience === "music"){
              global.monoCallInfo[userPhoneNumber]['audioExperienceObj'] = new Music(userPhoneNumber,experience,language)
            }
            else if(experience === "snippet"){
              global.monoCallInfo[userPhoneNumber]['audioExperienceObj'] = new Snippet(userPhoneNumber,experience,language)
            }
            return await global.monoCallInfo[userPhoneNumber]['audioExperienceObj'].initiate(params)
          }
          else if(experience === "riddle"){
            global.monoCallInfo[userPhoneNumber]['riddleExperienceObj'] = new Riddle(userPhoneNumber,experience,language)
            return await global.monoCallInfo[userPhoneNumber]['riddleExperienceObj'].initiate(params)
          }
          else if(experience === "keyLearning"){
            global.monoCallInfo[userPhoneNumber]['keyLearningGameObj'] = new KeyLearning(userPhoneNumber,language)
            return global.monoCallInfo[userPhoneNumber]['keyLearningGameObj'].initiate(params)
          }
          else if(experience === "scramble"){
            const game = new ScrambleGame(userPhoneNumber,language)
            global.monoCallInfo[userPhoneNumber]['scrambleGameObj'] = game
            return game.initiate(params)
          }
          else if(experience === "quiz"){
            const quiz = new Quiz(userPhoneNumber,language)
            global.monoCallInfo[userPhoneNumber]['quizObj'] = quiz
            return await quiz.initiate(params)
          }
          break
        case "5":
        case "7":
          if(digits === "5"){
            await storeLog(userPhoneNumber,digits,`chosen next 4 experiences`)
            currentUserObj.currentItemStartingIndex += 4
          }
          else if(digits === "7"){
            await storeLog(userPhoneNumber,digits,`chosen previous 4 experiences`)
            currentUserObj.currentItemStartingIndex -= 4
          }
          prepareNext4MenuContent(currentUserObj,global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
          return [
            ...global.monoCallInfo[userPhoneNumber]['currentContentActions'],
            DTMFInputAction(global.monoCallInfo[userPhoneNumber]['currentEndPoint'])
          ]
        case "9":
          await storeLog(userPhoneNumber,digits,`chosen previous menu(theme menu)`)
          currentUserObj.currentItemStartingIndex = 0
          prepareNext4MenuContent(currentUserObj,currentUserObj.themes,currentUserObj.themeTitleAudioUrl,"theme")

          const priorMessageAction = getStreamAction(global.pullMenuMainUrl + global.readingContentTitlesDialogUrl["theme"].replace('{language}',language).replace('{speechRate}',speechRate))
          
          const endpoint = global.communicationApi.getEndPointForAudioContentListInput("theme")
          global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
          actions.push(priorMessageAction)
          actions.push(...currentUserObj.currentContentActions)
          actions.push(DTMFInputAction(endpoint))

          return actions
      }
  }
}

async function handleMainMenuOfAudioExperience(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
      if(obj){
        return await obj.handleMainMenu(params)
      }
  }
  return []
}

async function handleContentListMenuOfAudioExperience(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
      if(obj){
        return await obj.handleContentList(params)
      }
  }
  return {
    nextActions:[],
    audioSelected:false
  }
}

// It will be triggered when the audio streaming is finished in Pull Call, to take further actions
async function handleAudioStreamFinished(userPhoneNumber){
  const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
  if(obj){
    return await obj.handleStreamFinished(userPhoneNumber)
  }
  else{
    return ""
  }
}

async function handleGoingBackToPreviousMenuFromAudioStreaming(params){
  const { userPhoneNumber } = params
  const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
  if(obj){
    return await obj.handleGoingBackToPreviousMenuFromStreaming(params)
  }
  else{
    return []
  }
}

async function handleUserInputOfKeryLearning(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['keyLearningGameObj'].handleUserInput(params)
  }
  else{
      return []
  }
}

async function handleScrambleMainMenu(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['scrambleGameObj'].handleMainMenu(params)
  }
  else{
      return []
  }
}

async function handleScrambleExplorePoemLineOrdering(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['scrambleGameObj'].handleExplorePoemLineOrdering(params)
  }
  else{
      return []
  }
}

async function handleScrambleEnteredPoemLineSequence(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['scrambleGameObj'].handleEnteredPoemLineSequence(params)
  }
  else{
      return []
  }
}

async function handleScrambleEnterSequenceAgainOrRepeatContentInput(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['scrambleGameObj'].handleEnterSequenceAgainOrRepeatContentInput(params)
  }
  else{
      console.log("invalid request")
      return []
  }
}

async function handleQuizMainMenu(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['quizObj'].handleMainMenu(params)
  }
  else{
      return []
  }
}

async function handleQuizMultipleChoiceSelection(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['quizObj'].handleMultipleChoiceSelection(params)
  }
  else{
      return []
  }
}

async function handleContentListMenuOfRiddleExperience(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['riddleExperienceObj'].handleContentListMenu(params)
  }
  else{
    return []
  }
}

async function handleRiddleContentPlaying(params){
  const {userPhoneNumber} = params
  if(isValidRequest(userPhoneNumber)){
      return await global.monoCallInfo[userPhoneNumber]['riddleExperienceObj'].handleContentPlaying(params)
  }
  else{
    return []
  }
}

// After every global.timeInMillisecondsToUpdateUserTimeInDB milliseconds, it will update the DB with user spent time in pull call
// And after updating, it will re-trigger the same function to do the same
async function setTimerToSaveUserTimeInDB(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    const timeToWait = global.timeInMillisecondsToUpdateUserTimeInDB
    global.monoCallInfo[userPhoneNumber]["timeoutIdForUpdateUserTimeInDB"] = setTimeout(async () => {
      const totalTime = global.monoCallInfo[userPhoneNumber]['timeSpentInMilliSeconds'] + timeToWait
      global.monoCallInfo[userPhoneNumber]['timeSpentInMilliSeconds'] = totalTime
      await userSpentTimeForPullModel.findOne({phoneNumber:userPhoneNumber}).update({timeInMilliSeconds:totalTime})
      if(totalTime >= global.maxTimeLimitForUserInMilliseconds){
        // play message and cut the call.
        global.communicationApi.playTimeOverMessageAndCutTheCall(userPhoneNumber)
      }
      else{
        setTimerToSaveUserTimeInDB(userPhoneNumber)
      }
    },timeToWait)
  }
}

// It parses the document to find all the required insights like callDuration, audioContentDurations for a single session/call for a user(based on phoneNumber)
function parseDocument(doc){
  const res = {"callDuration":0, "audioContentDurations":{}}
  const interactionsLength = doc.interactions.length;
  var i = 0
  var contentName = undefined
  var startTimeForContent = undefined
  var endTimeForContent = undefined
  var interaction = undefined
  while(i<interactionsLength){
    while(i<interactionsLength){
      interaction = doc.interactions[i]
      const means = interaction.means
      if(means.startsWith("chosen ") && !means.includes(" next 4 ") && !means.includes(" previous 4") && ( means.endsWith(" story") || means.endsWith(" poetry") || means.endsWith(" music") || means.endsWith(" snippet"))){
        const words = means.split(" ")
        contentName = words.slice(1,words.length-1).join(" ")
        startTimeForContent = interaction.timeStamp
        break;
      }
      else if(means.startsWith("chosen ") && means.endsWith(" resume")){
        startTimeForContent = interaction.timeStamp
        break;
      }
      i += 1
    }
    while(i<interactionsLength){
      interaction = doc.interactions[i]
      const means = interaction.means
      if(means.startsWith("chosen ") && ( means.endsWith(" pause") || means.endsWith(" exit"))){
        endTimeForContent = interaction.timeStamp
        break;
      }
      else if(interaction.pressedKey === "audioFinished"){
        endTimeForContent = interaction.timeStamp
        break;
      }
      i += 1
    }
    if(startTimeForContent && endTimeForContent){
      if(!res["audioContentDurations"].hasOwnProperty(contentName)){
        res["audioContentDurations"][contentName] = 0
      }
      res["audioContentDurations"][contentName] += new Date(endTimeForContent) - new Date(startTimeForContent)
      // console.log(`content Duration = ${res["audioContentDurations"][contentName]}`)
      startTimeForContent = undefined
      endTimeForContent = undefined
    }
    i += 1
  }
  if(!doc.hasOwnProperty("endDate")){
    doc["endDate"] = interaction?interaction.timeStamp:doc.createdDate
  }
  res["callDuration"] = new Date(doc.endDate) - new Date(doc.createdDate)
  if(startTimeForContent){
    if(!res["audioContentDurations"].hasOwnProperty(contentName)){
      res["audioContentDurations"][contentName] = 0
    }
    res["audioContentDurations"][contentName] += new Date(doc.endDate) - new Date(startTimeForContent)
  }
  return res
}

// Prepare the Doc accordingly to save in MongoDB based on pullModelInsights Model
async function preparePullModelInsightsDoc(doc,dateToSaveInsightsFor){
  const res = {}
  res["date"] = dateToSaveInsightsFor
  res["userInsights"] = []
  for(const [userPhoneNumber,userInsights] of Object.entries(doc)){
    const userInsight = {phoneNumber:userPhoneNumber,callDuration:userInsights["callDuration"],audioContentDurations:[]}
    for(const [contentName,duration] of Object.entries(userInsights["audioContentDurations"])){
      userInsight.audioContentDurations.push({contentName,duration}) // Note: in push method, i used structuring (oppos to destructuring in JavaScript)
    }
    res["userInsights"].push(userInsight)
  }
  return res;
}

function isValidDateFormat(dateString) {
  const datePattern = /^\d{1,2}\/\d{1,2}\/\d{4}$/;
  return datePattern.test(dateString);
}


// It will prepare the insights for all the pullmodel sessions for all dates specified in the List
async function GetPullModelInsights(req,res){
  const dates = req.body.dates
  const results = {}
  for(const date of dates){
    const isValidDate = isValidDateFormat(date);
    if(isValidDate){
      var currentDate = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'})
      currentDate = currentDate.split(",")[0]
      var milliSecondsDifference = new Date(currentDate) - new Date(date)
      if(milliSecondsDifference > 0){
        const exists = await pullModelInsights.exists({date:date})
        if(!exists){
          const docList = await monoCallLog.find({createdDate:{$regex:`^${date}`}})
          const finalResult = {}
          for(const doc of docList){
            const res = parseDocument(doc)
            if(!finalResult.hasOwnProperty(doc.userPhoneNumber)){
              finalResult[doc.userPhoneNumber] = { "callDuration": 0, "audioContentDurations":{}}
            }
            finalResult[doc.userPhoneNumber]["callDuration"] += res["callDuration"]
            for(const [contentName,duration] of Object.entries(res["audioContentDurations"])){
              if(!finalResult[doc.userPhoneNumber]["audioContentDurations"].hasOwnProperty(contentName)){
                finalResult[doc.userPhoneNumber]["audioContentDurations"][contentName] = 0
              }
              finalResult[doc.userPhoneNumber]["audioContentDurations"][contentName] += duration
            }
          }
          const finalDoc = await preparePullModelInsightsDoc(finalResult,date)
          await pullModelInsights.create(finalDoc)
          console.log("fetched logs, created insights and saved in DB successfully.")
        }
        const result = await pullModelInsights.findOne({date:date}).exec(); // Note: here we dont' necessarirly use .exec() method unless we use a chain of queries.
        results[date] = result
      }
      else if(milliSecondsDifference === 0){
        const docList = await monoCallLog.find({createdDate:{$regex:`^${date}`}})
        const finalResult = {}
        for(const doc of docList){
          const res = parseDocument(doc)
          if(!finalResult.hasOwnProperty(doc.userPhoneNumber)){
            finalResult[doc.userPhoneNumber] = { "callDuration": 0, "audioContentDurations":{}}
          }
          finalResult[doc.userPhoneNumber]["callDuration"] += res["callDuration"]
          for(const [contentName,duration] of Object.entries(res["audioContentDurations"])){
            if(!finalResult[doc.userPhoneNumber]["audioContentDurations"].hasOwnProperty(contentName)){
              finalResult[doc.userPhoneNumber]["audioContentDurations"][contentName] = 0
            }
            finalResult[doc.userPhoneNumber]["audioContentDurations"][contentName] += duration
          }
        }
        const finalDoc = await preparePullModelInsightsDoc(finalResult,date)
        results[date] = finalDoc
      }
      else{
        results[date] = {}
      }
    }
    else{
      results[date] = {}
    }
  }
  return res.send(results)
}

module.exports = {
    handleThemeTypeInput,
    handleRiddleContentPlaying,
    handleContentListMenuOfRiddleExperience,
    GetPullModelInsights,
    setTimerToSaveUserTimeInDB,
    handleQuizMultipleChoiceSelection,
    handleQuizMainMenu,
    handleScrambleEnterSequenceAgainOrRepeatContentInput,
    handleScrambleEnteredPoemLineSequence,
    handleScrambleExplorePoemLineOrdering,
    handleScrambleMainMenu,
    handleUserInputOfKeryLearning,
    handleAudioStreamFinished,
    prepareContentForLanguageMenu,
    handleLanguageInput,
    handleExperienceTypeInput,
    handleMainMenuOfAudioExperience,
    handleContentListMenuOfAudioExperience,
    handleGoingBackToPreviousMenuFromAudioStreaming,

}