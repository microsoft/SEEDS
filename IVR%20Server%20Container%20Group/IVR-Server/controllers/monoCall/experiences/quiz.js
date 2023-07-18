const axios = require('axios');
const { prepareNext4MenuContent, checkCommonDTMF, handleCommonDTMF, callFSM, storeLog, handleEventsFromFSM } = require('../utils');
const { v4: uuidv4 } = require("uuid");
const { getTalkAction, DTMFInputAction, getStreamAction } = require('../../../nccoActions');


  class Quiz{
    constructor(phoneNumber,lang){
      this.quizTitles = []
      this.quizTitleToId = {}
      this.quizTitleToAudioMessageUrl = {}
      this.userPhoneNumber = phoneNumber
      this.language = lang
      this.fsmContextId = undefined
      this.fsmName = undefined
      this.nextStateUrl = `${remoteUrl}mono_call/quiz/getNextState`
      this.URLToSendUserInput = undefined
      this.timeInSecondsForUserToSelectAnswer = 5
      this.currentQuestionActions = []
      this.currentQuestionMultiChoiceMappings = {}
      this.answerMappingMessage = undefined
      this.prevScoreActions = []
      this.prevScoreText = ""
      this.isEnded = undefined
    }

    // we are using it multiple times. That's why separating out from *initiate* method(In *initiate* method, there are some prior Actions. Look at this method.)
    async prepareContentForMainMenu(currentUserObj){
      const resp = await axios.get(`https://place-seeds.azurewebsites.net/getAllQuizzesByLanguage?language=${this.language}`)
      for(const quizInfo of resp.data['quizzes']){
        if(quizInfo.hasOwnProperty("titleAudio")){
          this.quizTitles.push(quizInfo.title)
          this.quizTitleToId[quizInfo.title] = quizInfo.id
          this.quizTitleToAudioMessageUrl[quizInfo.title] = quizInfo.titleAudio["1.0"].filePath.replace("1.0.mp3","{speechRate}.mp3")
        }
      }
      currentUserObj.nextItemStartingIndex = 0
      prepareNext4MenuContent(currentUserObj,this.quizTitles,this.quizTitleToAudioMessageUrl,'quiz') // currentUserObj.currentContentActions will get filled out here
      const endpoint = global.communicationApi.getEndPointForQuizMainMenuInMonoCall()
      currentUserObj.currentEndPoint = endpoint
    }

    async initiate(params){
      const {userPhoneNumber} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      const priorMessageActions = [
        getStreamAction(global.pullMenuMainUrl + global.quizWelcomeMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
        getStreamAction(global.pullMenuMainUrl + global.chooseQuizzesMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate))
      ]

      await this.prepareContentForMainMenu(currentUserObj)

      const actions = [
        ...priorMessageActions,
        ...currentUserObj.currentContentActions, // currentUserObj.currentContentActions will get filled out after the call of *this.prepareContentForMainMenu(currentUserObj)* above
        DTMFInputAction(currentUserObj.currentEndPoint)
      ]

      return actions
    }

    // It will start the Quiz FSM with FSM Executor
    async initiateFSM(){
      this.isEnded = false
      this.fsmContextId = uuidv4()
      global.fsmIdToPhoneNumber[this.fsmContextId] = this.userPhoneNumber
      // const startFSMEndPoint = `http://${process.env.FSM_EXEC_CONTAINER_NAME}:${process.env.FSM_EXEC_PORT}/startFSM`
      const startFSMEndPoint = `http://localhost:${process.env.FSM_EXEC_PORT}/startFSM`
      const fsmType = "onDemand"
      return await callFSM(startFSMEndPoint,this.nextStateUrl,this.fsmName,this.fsmContextId,"quiz",fsmType)
    }

    async handleMainMenu(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
          return await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
      }
      else{
          if(currentUserObj.keyMappings.hasOwnProperty(digits)){
              if(digits === "5"){
                await storeLog(userPhoneNumber,digits,"chosen next 4 Quizes")
                prepareNext4MenuContent(currentUserObj,this.quizTitles,this.quizTitleToAudioMessageUrl,'quiz')

                const actions = [
                  ...currentUserObj.currentContentActions,
                  DTMFInputAction(currentUserObj.currentEndPoint)
                ]
                
                return actions
              }
              else{
                  const quizTitle = currentUserObj.keyMappings[digits]
                  this.fsmName = this.quizTitleToId[quizTitle]
                  await storeLog(userPhoneNumber,digits,`chosen ${quizTitle} Trivia`)
                  const body = await this.initiateFSM()
                  console.log("response from initiate FSM")
                  // console.log(body.data)
                  console.log(JSON.stringify(body.data,null,2))
                  return await this.handleStatesFromFSMExecutor(body.data)
              }
            }
            else if(digits === "9"){
              await storeLog(userPhoneNumber,digits,"chosen previous menu(experiences menu)")
              currentUserObj.nextItemStartingIndex = 0
              prepareNext4MenuContent(currentUserObj,global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
              global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'experience'
              const endpoint = global.communicationApi.getEndPointForExperienceTypeInput()
              global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint

              const actions = [
                ...currentUserObj.currentContentActions,
                DTMFInputAction(endpoint)
              ]

              return actions
            }
      }
    }

    // It will be triggered when the last question in quiz is just completed i.e. Quiz is Over
    // It will prepare the Actions that announce the Final Score to user and take the user back to Quiz Main Menu
    async handleQuizEndState(currentUserObj,body){
      const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]
      const ttsLanguage = global.verbalLanguageToIVRTTSLanguageCode[this.language]
      this.isEnded = true
      const fsmId = body.fsmContextId

      // sending Mapping of fsmContextId to PhoneNumbers in the Call(only one phoneNumber mostly as it is pull model), to SEEDS server
      axios.post(global.fsmContentIdToPhoneNumberMappingUrl,{
        fsmContextId:fsmId,
        phoneNumbers:[this.userPhoneNumber]
      },{headers:{ authToken:'postman'}})
      .then(resp => console.log(resp.data))
      .catch((err) => console.log(err.message))

      
      delete global.fsmIdToPhoneNumber[fsmId]

      const actions = [...this.prevScoreActions]

      if(body.data.hasOwnProperty('text')){
        actions.push(getTalkAction(body.data.text,ttsLanguage,false))
      }
      else if(body.data.hasOwnProperty('audioData')){
        for(const audioInfo of body.data.audioData){
          actions.push(getStreamAction(audioInfo[speechRate]["filePath"],1,false))
        }
      }

      const priorMessageActions = [
        getStreamAction(global.pullMenuMainUrl + global.takingToQuizMainMenuMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
        getStreamAction(global.pullMenuMainUrl + global.chooseQuizzesMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate))
      ]

      await this.prepareContentForMainMenu(currentUserObj)

      actions.push(
        ...priorMessageActions,
        ...currentUserObj.currentContentActions,
        DTMFInputAction(currentUserObj.currentEndPoint)
      )

      if(this.prevScoreText !== ""){
        await storeLog(this.userPhoneNumber,"prevQuestionResult",this.prevScoreText)
      }
      await storeLog(this.userPhoneNumber,"quizEnd",`${body.data.text} && Taking the User to Quiz Main Menu`)

      // clearing the previous as it might affect next quiz.
      this.prevScoreActions = []
      this.prevScoreText = ""

      return actions
    }

    // It will prepare the actions that announce the Quiz Questions along with answer choices and waits for user input
    async playQuestionAlongWithMultipleChoices(currentUserObj,body){
      const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]
      const ttsLanguage = global.verbalLanguageToIVRTTSLanguageCode[this.language]
      const actions = [...this.prevScoreActions]

      this.currentQuestionActions = []

      this.currentQuestionText = ""
      if(body.data.hasOwnProperty('text')){
        this.currentQuestionActions.push(getTalkAction(body.data.text,ttsLanguage,false))
      }
      else if(body.data.hasOwnProperty('audioData')){
        
        this.currentQuestionText = body.data.audioData[0][speechRate]["text"] || ""
        for(const audioInfo of body.data.audioData){
          this.currentQuestionActions.push(getStreamAction(audioInfo[speechRate]["filePath"],1,false))
        }
      }
      const response = handleEventsFromFSM(body.events,this.language,speechRate)
      this.currentQuestionMultiChoiceMappings = response["keyToEventMappings"]
      this.answerMappingActions = response["eventActions"] 
      /*
      note: here actions means control flow. 
      Every communication API has it's own control flow.
      Ex: vonage has NCCO as it's call flow.
      That's why i am calling it as "Actions" as general name.
      */
      this.URLToSendUserInput = body.userInputEp

      console.log('url to send input.')
      console.log(this.URLToSendUserInput)

      const endpoint = global.communicationApi.getEndPointForQuizMultipleChoiceSelectionInput()
      currentUserObj.currentEndPoint = endpoint

      actions.push(
        ...this.currentQuestionActions,
        ...this.answerMappingActions
      )
      if(this.currentQuestionActions.length > 0){
        actions.push(
          getStreamAction(global.pullMenuMainUrl + global.repeatQuizQuestionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1),
          getStreamAction(global.pullMenuMainUrl + global.press8Url.replace('{language}',this.language).replace('{speechRate}',speechRate),1)
        )
      }
      
      actions.push(
        DTMFInputAction(endpoint,1,this.timeInSecondsForUserToSelectAnswer)
      )
      
      if(this.prevScoreText !== ""){
        await storeLog(this.userPhoneNumber,"prevQuestionResult",this.prevScoreText)
      }
      await storeLog(this.userPhoneNumber,"quizQuestion",this.currentQuestionText)

      this.prevScoreActions = []
      this.prevScoreText = ""

      return actions
    }

    // It will receive the next states from FSMExecutor like Previous Quiz Question Answer, Next Quiz Question
    async handleStatesFromFSMExecutor(body){
      const states = body.states;
      const userPhoneNumber = this.userPhoneNumber
      const ttsLanguage = global.verbalLanguageToIVRTTSLanguageCode[this.language]
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
  
      for(const state of states){
        if(state.isEndState){
          return await this.handleQuizEndState(currentUserObj,state)
        }
        else if(state.expectedInputType === "NIL"){
          this.prevScoreActions = []
          this.prevScoreText = ""
          if(state.data.hasOwnProperty('text')){
            this.prevScoreActions.push(getTalkAction(state.data.text,ttsLanguage,false))
            this.prevScoreText = state.data.text
          }
          else if(state.data.hasOwnProperty('audioData')){
            for(const audioInfo of state.data.audioData){
              const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]
              this.prevScoreActions.push(getStreamAction(audioInfo[speechRate]["filePath"],1,false))
            }
          }
        }
        else{
          return await this.playQuestionAlongWithMultipleChoices(currentUserObj,state)
        }
      }
    }

    // It submits the user selected answer to FSMExecutor to validate and gets the response of score, next question
    async submitAnswerToFSMForValidation(answeredEvent){
      return await axios.post(this.URLToSendUserInput,{
        fsmContextId:this.fsmContextId,
        event:answeredEvent
      })
    }

    // This is the main function which handles the user selected input for a quiz question
    async handleMultipleChoiceSelection(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      if(this.currentQuestionMultiChoiceMappings.hasOwnProperty(digits)){
        console.log("valid option.")
        const answeredEvent = this.currentQuestionMultiChoiceMappings[digits]
        await storeLog(userPhoneNumber,digits,`chosen Answer as ${answeredEvent}`)
        const body = await this.submitAnswerToFSMForValidation(answeredEvent)
        console.log("response from answer submition")
        console.log(JSON.stringify(body.data,null,2))
        return await this.handleStatesFromFSMExecutor(body.data)
      }
      else{
        var actions = []

        if(digits === ""){
          actions.push(getStreamAction(global.pullMenuMainUrl + global.chosenNoOptionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false))
          await storeLog(userPhoneNumber,digits,"chosen Invalid Option")
        }
        else if(digits !== "8"){
          actions.push(getStreamAction(global.pullMenuMainUrl + global.chosenWrongOptionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false))
          await storeLog(userPhoneNumber,digits,"chosen Invalid Option")
        }
        else if(digits === "8"){
          await storeLog(userPhoneNumber,digits,"chosen to repeat Question")
        }

        actions.push(
          ...this.currentQuestionActions,
          ...this.answerMappingActions
        )
        if(this.currentQuestionActions.length > 0){
          actions.push(
            getStreamAction(global.pullMenuMainUrl + global.repeatQuizQuestionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1),
            getStreamAction(global.pullMenuMainUrl + global.press8Url.replace('{language}',this.language).replace('{speechRate}',speechRate),1)
          )
        }
        
        actions.push(
          DTMFInputAction(currentUserObj.currentEndPoint,1,this.timeInSecondsForUserToSelectAnswer)
        )

        return actions
      }
    }
  }

  module.exports = {
    Quiz
  }