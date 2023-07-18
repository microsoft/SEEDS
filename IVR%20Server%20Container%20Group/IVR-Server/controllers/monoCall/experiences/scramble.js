const { getTalkAction, DTMFInputAction, getStreamAction } = require("../../../nccoActions")
const { storeLog, prepareNext4MenuContent, checkCommonDTMF, handleCommonDTMF, setCurrentExperience } = require("../utils")

class ScrambleGame{
    constructor(phoneNumber,lang){
      this.userPhoneNumber = phoneNumber
      this.language = lang
      this.audioTitle = ""
      this.audioId = undefined
      this.audioBlobUrl = ""
      this.numberOfLines = 0
      this.randomLineSequence = []
      this.randomLineNumberToAudioId = {}
    }

    // we are using it multiple times. That's why separating out from *initiate* method(In *initiate* method, there are some prior Actions. Look at this method.)
    prepareContentForMainMenu(currentUserObj){
      currentUserObj.nextItemStartingIndex = 0
      prepareNext4MenuContent(currentUserObj,global.poems,global.poemTitleToAudioMessageUrl,'poetry')
      
      const endpoint = global.communicationApi.getEndPointForScrambleMainMenu()
      currentUserObj.currentEndPoint = endpoint
    }

    initiate(params){
      const {userPhoneNumber} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      const priorMessageActions = [
        getStreamAction(global.pullMenuMainUrl + global.scrambleWelcomeMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
        getStreamAction(global.pullMenuMainUrl + global.scrambleChoosePoemsMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate))
      ]

      this.prepareContentForMainMenu(currentUserObj)

      const actions = [
        ...priorMessageActions,
        ...currentUserObj.currentContentActions,
        DTMFInputAction(currentUserObj.currentEndPoint)
      ]

      return actions
    }

    getBlobUrlFromAudioId(id){
        return global.poemIdToBlobUrl[id]
    }

    // It will be triggered when they choose a paritcular poem title to start the game or want to repeat the game with same poem
    startOrRepeatScrambleGame(){
      const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]
      const endpoint = global.communicationApi.getEndPointForScrambleExploringPoemLineOrdering()
      global.monoCallInfo[this.userPhoneNumber]['currentEndPoint'] = endpoint
      this.numberOfLines = global.poemIdToNumberOfLines[this.audioId]
      const temp = []
      for(let i=1;i<=this.numberOfLines;i++){
          temp.push(i.toString())
      }
      this.randomLineSequence = temp.sort( ()=>Math.random()-0.5 )
      // console.log(this.randomLineSequence)
      for(let i=0;i<this.numberOfLines;i++){
          this.randomLineNumberToAudioId[this.randomLineSequence[i]] = this.audioId+"_"+(i+1).toString()
      }

      const announcementActions = [
        getStreamAction(global.pullMenuMainUrl + global.toListenToMappedLinesMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
        getStreamAction(global.pullMenuMainUrl + global.scramblePressAnyKeyBetweenMessageUrl.replace('{language}',this.language).replace('upperLimit',this.numberOfLines).replace('{speechRate}',speechRate))
      ]

      const actions = [
        getStreamAction(this.audioBlobUrl,1,false),
        ...announcementActions,
        DTMFInputAction(endpoint)
      ]

      return {
        isExploringLineOrder:true,
        nextActions:actions
      }
    }

    // It will simply prepare the menu to announce the audio messages(audio url populated in *global.guessSequenceMessageUrl*)
    startOrRepeatHandleSequenceMenu(){
      const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]

      const announcementAction = getStreamAction(global.pullMenuMainUrl + global.guessSequenceMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate))

      const endpoint = global.communicationApi.getEndPointForScramblePoemLineSequenceInput()
      global.monoCallInfo[this.userPhoneNumber]['currentEndPoint'] = endpoint
      
      const actions = [
        announcementAction,
        DTMFInputAction(endpoint,this.numberOfLines,10)
      ]

      return {
        isExploringLineOrder:false,
        nextActions:actions
      }

    }

    // It will handle input of user when they choose a particular poem from the list of poems OR to choose next 4 poems
    async handleMainMenu(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      
      if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
         const actions = await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
         return  {
          isExploringLineOrder:false,
          nextActions:actions
        }
      }
      else{
        if(currentUserObj.keyMappings.hasOwnProperty(digits)){
            if(digits === "5"){
              await storeLog(userPhoneNumber,digits,"chosen next 4 poems")
              prepareNext4MenuContent(currentUserObj,global.poems,global.poemTitleToAudioMessageUrl,'poetry')

              const actions = [
                ...currentUserObj.currentContentActions,
                DTMFInputAction(currentUserObj.currentEndPoint)
              ]
              
              return  {
                isExploringLineOrder:false,
                nextActions:actions
              }
            }
            else{
                this.audioTitle = currentUserObj.keyMappings[digits]
                await storeLog(userPhoneNumber,digits,`chosen ${this.audioTitle} Poem`)
                this.audioId = global.poemToId[this.audioTitle]
                this.audioBlobUrl = this.getBlobUrlFromAudioId(this.audioId)
                
                return this.startOrRepeatScrambleGame()
            }
          }
          else if(digits === "9"){
            setCurrentExperience(userPhoneNumber,undefined)
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

            return {
              isExploringLineOrder:false,
              nextActions:actions
            }
          }
      }
    }

    // It will be triggered while user is pressing keys to know the mapped Poem Line. Basically it will announce that poem line upon user pressing the valid key.
    // User will use this option to explore the keys and mapped Poem Lines. so that user can guess and enter the correct sequence by pressing 0 further
    async handleExplorePoemLineOrdering(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      if(this.randomLineNumberToAudioId.hasOwnProperty(digits)){
        await storeLog(userPhoneNumber,digits,`Pressed ${digits} to guess the corresponding mapped line`)
        const audioId = this.randomLineNumberToAudioId[digits]
        const blobUrl = this.getBlobUrlFromAudioId(audioId)

        const actions = [
          getStreamAction(blobUrl,1),
          DTMFInputAction(currentUserObj.currentEndPoint)
        ]

        return {
          isExploringLineOrder:true,
          nextActions:actions
        }

      }
      else if(digits === "0"){
        await storeLog(userPhoneNumber,digits,`chosen ${digits} to enter the Guessed Sequence`)
        return this.startOrRepeatHandleSequenceMenu()
      }
      else{
        await storeLog(userPhoneNumber,digits,`Pressed invalid digit ${digits} while guessing mapped line`)

        const actions = [
          DTMFInputAction(currentUserObj.currentEndPoint)
        ]

        return {
          isExploringLineOrder:true,
          nextActions:actions
        }

      }
    }

    // It will receive the user entered Sequence and validate whether it is correct or not
    async handleEnteredPoemLineSequence(params){
      const {userPhoneNumber,digits} = params
      const ttsLanguage = global.verbalLanguageToIVRTTSLanguageCode[this.language]
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      if(digits === this.randomLineSequence.join("")){
        await storeLog(userPhoneNumber,digits,`chosen the correct sequence as ${digits}. So, Playing the Poem once again and then taking the user back to Scramble Main Menu.`)

        const priorMessage1Action = getStreamAction(global.pullMenuMainUrl + global.messageForCorrectSequenceUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false)
        const priorMessage2Action = getStreamAction(global.pullMenuMainUrl + global.takeToScrambleMainMenuMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false)

        this.prepareContentForMainMenu(currentUserObj)
        
        const actions = [
          priorMessage1Action,
          getStreamAction(this.audioBlobUrl,1,false),
          priorMessage2Action,
          ...currentUserObj.currentContentActions,
          DTMFInputAction(currentUserObj.currentEndPoint)
        ]

        return actions
      }
      else{
        await storeLog(userPhoneNumber,digits,`chosen the wrong sequence as ${digits}`)
        await storeLog(userPhoneNumber,"enterSequenceAgainOrRepeatContent",`Asking the User if they want an other chance to enter the sequence or Repeat the Poem from starting.`)

        const priorMessageActions = [
          getStreamAction(global.pullMenuMainUrl + global.enteredSequenceMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
          getTalkAction(digits.split("").join(' '),ttsLanguage,false),
          getStreamAction(global.pullMenuMainUrl + global.wrongMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
        ]

        currentUserObj.currentContentActions = [
          getStreamAction(global.pullMenuMainUrl + global.enterSequenceAgainMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
          getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'0').replace('{speechRate}',speechRate)),
          getStreamAction(global.pullMenuMainUrl + global.replayContentMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
          getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'8').replace('{speechRate}',speechRate))
        ]

        const endpoint = global.communicationApi.getEndPointForScrambleEnterSequenceOrRepeatContentInput()
        global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint

        const actions = [
          ...priorMessageActions,
          ...currentUserObj.currentContentActions,
          DTMFInputAction(endpoint)
        ]

        return actions
      }
    }

    // It will be triggered when the user chooses an option for the prompt of either entering the sequence again or repeating the whole game again with same poem
    async handleEnterSequenceAgainOrRepeatContentInput(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const speechRate = global.speechRates[currentUserObj.speechRateIndex]

      if(digits === "0"){
        await storeLog(userPhoneNumber,digits,"chosen to enter the sequence Again")
        return this.startOrRepeatHandleSequenceMenu()
      }
      else if(digits === "8"){
        await storeLog(userPhoneNumber,digits,"chosen to repeat the Poem Again")
        return this.startOrRepeatScrambleGame()
      }
      else{
        await storeLog(userPhoneNumber,digits,"chosen Invalid Option")
        var priorMessageAction = undefined
        if(digits === ""){
          const priorMessageUrl = global.chosenNoOptionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)
          priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
        }
        else{
          const priorMessageUrl = global.chosenWrongOptionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)
          priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
        }

        const actions = [
          priorMessageAction,
          ...currentUserObj.currentContentActions,
          DTMFInputAction(currentUserObj.currentEndPoint)
        ]

        return {
          isExploringLineOrder:false,
          nextActions:actions
        }
      }
    }
  }

  module.exports = {ScrambleGame}