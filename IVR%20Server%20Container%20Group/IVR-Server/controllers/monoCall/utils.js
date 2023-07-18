const axios = require('axios');
const { getTalkAction, DTMFInputAction, getStreamAction } = require("../../nccoActions")

const monoCallLog = require("../../models/monoCallLog")
const pullModelUserSettings = require("../../models/pullModelUserSettings")

// It will create a document to use further to store all the pullCall Logs
// And saving that document's id in a global variable to refer it for later use
async function createMonoCallLog(userPhoneNumber){
  const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  try{
    const monoCallLogObject = await monoCallLog.create({
      userPhoneNumber:userPhoneNumber,
      createdDate:indianTime,
      interactions:[]
    })
    global.phoneNumberToLogCollectionId[userPhoneNumber] = monoCallLogObject._id
  }
  catch(error){
    throw error
  }
}

// It will initiate a MonoCall Object for a user in a global variable called *global.monoCallInfo*
function createMonoCallObjectForUser(userPhoneNumber){
  global.monoCallInfo[userPhoneNumber] = {speechRateIndex:{},language:global.initialLanguage}
}

function deleteMonoCallObject(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    delete global.monoCallInfo[userPhoneNumber]
  }
}

function getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    return global.monoCallInfo[userPhoneNumber]['audioExperienceObj']
  }
}

// It will be triggered everytime, to store KeyPress Event or any other necessary event in Pull Model
async function storeLog(userPhoneNumber,pressedKey,means){
  const collectionId = global.phoneNumberToLogCollectionId[userPhoneNumber]
  const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  const interactionObject = {
    pressedKey:pressedKey,
    means:means,
    timeStamp:indianTime
  }
  try{
    await monoCallLog.findById(collectionId).update({
        $push: { interactions : interactionObject },
    });
  }
  catch(error){
    throw error
  }
}

// It will simply populate the field *endDate* when the call is over
async function setCallEndedDateInMonoCallLog(userPhoneNumber){
    const collectionId = global.phoneNumberToLogCollectionId[userPhoneNumber]
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    
    await monoCallLog.findById(collectionId).update({
        endDate:indianTime
    });

    delete global.phoneNumberToLogCollectionId[userPhoneNumber]
}

// It will simply slices out 4 items max from current position in the list
function getTheNext4Items(currPos,items){
    var nextItemExist = false
    var prevItemExist = false
    if(items.length > currPos + 4){
      nextItemExist = true
    }
    if(currPos - 4 >= 0){
      prevItemExist = true
    }
    const currentSectionOfItems = items.slice(currPos,currPos+4)
    return [currentSectionOfItems,prevItemExist,nextItemExist]
  }
  
  // It will map the key to item like 1 to 1st item, 2 to 2nd item etc.
  function mapTheKeys(keys){
    const length = keys.length
    const mappings = {}
    for(let i=0;i<length;i++){
      mappings[(i+1).toString()] = keys[i]
    }
    return mappings
  }
  
  // It will prepare the Audio Message Actions for all items in the list(audioUrls), to be played in Call as Menu
  function prepareMappingMessageActions(audioUrls,language,speechRate){
    const messageActions = []
    const length = audioUrls.length
    for(let i=0;i<length;i++){
      const audioUrl = audioUrls[i]
      messageActions.push(getStreamAction(audioUrl)),
      messageActions.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,(i+1)).replace('{speechRate}',speechRate)))
    }
    return messageActions
  }

  // This the main function which prepares all the audio messages required to be played as Menu in Call, upon user selecting the Next4/Prev4 items
function prepareNext4MenuContent(obj,items,itemTitleToAudioUrl,exp){
    const language = obj.language
    const speechRate = global.speechRates[obj['speechRateIndex'][language]]
    const [keys,prevItemExist,nextItemExist] = getTheNext4Items(obj.currentItemStartingIndex,items)
    obj.keyMappings = mapTheKeys(keys)
    const audioUrls = []
    for(const key of keys){
      audioUrls.push(itemTitleToAudioUrl[key].replace('{language}',language).replace('{speechRate}',speechRate))
    }
    var actions = []

    actions.push(...prepareMappingMessageActions(audioUrls,language,speechRate))
    
    if(nextItemExist){
      const next4MessageUrl = global.next4MessageUrls[exp].replace('{language}',language).replace('{speechRate}',speechRate)
      actions.push(getStreamAction(global.pullMenuMainUrl + next4MessageUrl))
      actions.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,'5').replace('{speechRate}',speechRate)))
      obj.keyMappings["5"] = ""
    }
    if(prevItemExist){
      const prev4MessageUrl = global.prev4MessageUrls[exp].replace('{language}',language).replace('{speechRate}',speechRate)
      actions.push(getStreamAction(global.pullMenuMainUrl + prev4MessageUrl))
      actions.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,'7').replace('{speechRate}',speechRate)))
      obj.keyMappings["7"] = ""
    }

    const repeatMenuUrl = global.repeatCurrentMenuUrl.replace('{language}',language).replace('{speechRate}',speechRate)
    actions.push(getStreamAction(global.pullMenuMainUrl + repeatMenuUrl))
    actions.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,'8').replace('{speechRate}',speechRate)))

    const previousMenuMessageUrl = global.goToPreviousMenuMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)

    actions.push(getStreamAction(global.pullMenuMainUrl + previousMenuMessageUrl))
    actions.push(getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',language).replace(/{key}/g,'9').replace('{speechRate}',speechRate)))

    obj.currentContentActions = actions

  }

  // here isValidRequest? means is that call still alive or not(ended)
  function isValidRequest(userPhoneNumber){
    if(!userPhoneNumber || !global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
        return false
    }
    else{
      return true
    }
  }

  /* 
  this function checks if the given input is Common DTMF or not
  common DTMF inputs are  8,9,"",invalidDigit Entered.
  8 to repeat, 9 to go to previous menu, remaining are invalid Digits
  Because actions to these keys are same across all the menus, i called it as common DTMF and handling it separetly to reduce redundancy
  */
  function checkCommonDTMF(params){
    const {
      digits,
      validDigits
    } = params
    if(digits === "9"){
      return false
    }
    else if(digits === ""){
      return true
    }
    else if(digits.length > 1 || !validDigits.hasOwnProperty(digits)){
      return true
    }
    else if(digits === "8"){
      return true
    }
    else{
      return false
    }
  }

//this function handles the common DTMF input like 8,9,"",invalidDigit Entered. It prepares the Action Menus accordingly
async function handleCommonDTMF(params){
  const {
    validDigits,
    userPhoneNumber,
    digits
  } = params
  const language = global.monoCallInfo[userPhoneNumber]['language']
  const currentEndPoint = global.monoCallInfo[userPhoneNumber]['currentEndPoint']
  var currentSpeechRate = global.speechRates[global.initialSpeechRateIndex]
  if(global.monoCallInfo[userPhoneNumber]['speechRateIndex'].hasOwnProperty(language)){
    currentSpeechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
  }
  var priorMessageAction = undefined
  const currentMenuContentActions = global.monoCallInfo[userPhoneNumber]['currentContentActions']
  if(digits === ""){
     const priorMessageUrl = global.chosenNoOptionUrl.replace('{language}',language).replace('{speechRate}',currentSpeechRate)
     priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
  }
  else if(digits === "8"){
    // Do Nothing
  }
  else if(digits.length > 1 || !validDigits.hasOwnProperty(digits)){
    const priorMessageUrl = global.chosenWrongOptionUrl.replace('{language}',language).replace('{speechRate}',currentSpeechRate)
    priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
  }
  if(digits === "8"){
    await storeLog(userPhoneNumber,digits,"Repeating Current Menu")
  }
  else{
    await storeLog(userPhoneNumber,digits,"chosen Invalid Option")
  }
  const actions = []
  if(priorMessageAction){
    actions.push(priorMessageAction)
  }
  actions.push(...currentMenuContentActions)
  actions.push(DTMFInputAction(currentEndPoint))
  
  return actions
}

/*
This is a Base Class for all the story kind of experiences like story/poem/song/Snippet.
Apologies for the Bad Naming. Please feel free to change it.
All the experiences mentioned above will inherit this base class.
*/
  class AudioExperience{
    constructor(number,experienceName,lang){
      this.userPhoneNumber = number
      this.experienceName = experienceName
      this.language = lang
      this.audioTitle = undefined
      this.audioId = undefined
      this.audioState = undefined
      this.contents = []
      this.audioTitleToId = {}
      this.audioTitleToAudioMessageUrl = {}
    }
    setAudioState(state){
      this.audioState = state
    }
    getAudioState(){
      return this.audioState
    }
    getAudioId(){
      return this.audioId
    }

    // prepareMainMenu(){
    //   const speechRate = global.speechRates[global.monoCallInfo[this.userPhoneNumber]['speechRateIndex']]
    //   const priorMessageUrl = global.readingExperienceMainMenuUrl[this.experienceName].replace('{language}',this.language).replace('{speechRate}',speechRate)
    //   const priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)

    //   global.monoCallInfo[this.userPhoneNumber]['currentContentActions'] = [
    //     getStreamAction(global.pullMenuMainUrl + global.contentListDialogUrl[this.experienceName].replace('{language}',this.language).replace('{speechRate}',speechRate)),
    //     getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'1').replace('{speechRate}',speechRate)),
    //     getStreamAction(global.pullMenuMainUrl + global.repeatCurrentMenuUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
    //     getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'8').replace('{speechRate}',speechRate)),
    //     getStreamAction(global.pullMenuMainUrl + global.goToPreviousMenuMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
    //     getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'9').replace('{speechRate}',speechRate))
    //   ]

    //   const endpoint = global.communicationApi.getEndPointForAudioExperienceMainMenu(this.experienceName)
    //   global.monoCallInfo[this.userPhoneNumber]['currentEndPoint'] = endpoint

    //   const actions = [
    //     priorMessageAction,
    //     ...global.monoCallInfo[this.userPhoneNumber]['currentContentActions'],
    //     DTMFInputAction(endpoint)
    //   ];
    
    //   return actions

    // }

    
    // It will inititate the Experience by preparing the Main Menu of the corresponding experience and returning to caller
    async initiate(params){
        const {userPhoneNumber,digits} = params
        const currentUserObj = global.monoCallInfo[userPhoneNumber]

        const language = this.language
        const experience = this.experienceName
        const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
        const theme = currentUserObj.theme
        const actions = []
        
        await storeLog(userPhoneNumber,digits,`Chosen ${experience} Content List`)

        const expName = global.IVRExperienceNameToSEEDSServerExperienceName[experience]

        const resp = await axios.get(global.contentUrl+`?language=${language}&theme=${theme}&expName=${expName}`,{headers:{ authToken:'postman'}})
        const contents = resp.data

        this.contents = []
        this.audioTitleToAudioMessageUrl = {}
        this.audioTitleToId = {}
        for(const contentInfo of contents){
          this.contents.push(contentInfo.title)
          // tag: Map contentNamesInAudioUrls Note: don't replace the language variable here.
          this.audioTitleToAudioMessageUrl[contentInfo.title] = contentInfo.titleAudio + '/{speechRate}.mp3'
          this.audioTitleToId[contentInfo.title] = contentInfo.id
        }

        // console.log(`Total contentLength = ${this.contents.length}`)

        currentUserObj.currentItemStartingIndex = 0
        prepareNext4MenuContent(currentUserObj,this.contents,this.audioTitleToAudioMessageUrl,experience)

        const priorMessageAction = getStreamAction(global.pullMenuMainUrl + global.readingContentTitlesDialogUrl[experience].replace('{language}',language).replace('{speechRate}',speechRate))
        
        const endpoint = global.communicationApi.getEndPointForAudioContentListInput(experience)
        global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
        actions.push(priorMessageAction)
        actions.push(...currentUserObj.currentContentActions)
        actions.push(DTMFInputAction(endpoint))

        return actions
    }

    // async handleMainMenu(params){
    //   const {userPhoneNumber,digits} = params
    //   const currentUserObj = global.monoCallInfo[userPhoneNumber]

    //   const validDigits = {"1":""}
    //   if(checkCommonDTMF({...params,validDigits:validDigits})){
    //     return await handleCommonDTMF({...params,validDigits:validDigits})
    //   }
    //   else{
    //     const language = this.language
    //     const experience = this.experienceName
    //     const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex']]
    //     var priorMessageAction = undefined
    //     const actions = []
    //     var endpoint = ""
    //     if(digits === "1"){
    //       await storeLog(userPhoneNumber,digits,`Chosen ${experience} Content List`)

    //       const expName = global.IVRExperienceNameToSEEDSServerExperienceName[experience]

    //       const resp = await axios.get(global.contentUrl,{headers:{ authToken:'postman'}})
    //       const contents = resp.data

    //       this.contents = []
    //       this.audioTitleToAudioMessageUrl = {}
    //       this.audioTitleToId = {}
    //       for(const contentInfo of contents){
    //         if(contentInfo.isPullModel && contentInfo.isProcessed && contentInfo.type === expName && contentInfo.language.toLowerCase() === language && contentInfo.titleAudio != ""){
    //           this.contents.push(contentInfo.title)

    //           // tag: Map contentNamesInAudioUrls Note: don't replace the language variable here.
    //           this.audioTitleToAudioMessageUrl[contentInfo.title] = contentInfo.titleAudio + '/{speechRate}.mp3'
    //           this.audioTitleToId[contentInfo.title] = contentInfo.id
    //         }
    //       }

    //       // console.log(`Total contentLength = ${this.contents.length}`)

    //       currentUserObj.nextItemStartingIndex = 0
    //       prepareNext4MenuContent(currentUserObj,this.contents,this.audioTitleToAudioMessageUrl,experience)

    //       priorMessageAction = getStreamAction(global.pullMenuMainUrl + global.readingContentTitlesDialogUrl[experience].replace('{language}',language).replace('{speechRate}',speechRate))
          
    //       endpoint = global.communicationApi.getEndPointForAudioContentListInput(experience)
    //       global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
    //     }
    //     else if(digits === "9"){
    //       setCurrentExperience(userPhoneNumber,undefined)
    //       await storeLog(userPhoneNumber,digits,`chosen previous menu(experiences menu)`)
    //       currentUserObj.nextItemStartingIndex = 0
    //       prepareNext4MenuContent(currentUserObj,global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
    //       global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'experience'
    //       endpoint = global.communicationApi.getEndPointForExperienceTypeInput()
    //       global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
    //     }

    //     if(priorMessageAction){
    //       actions.push(priorMessageAction)
    //     }
    //     actions.push(...currentUserObj.currentContentActions)
    //     actions.push(DTMFInputAction(endpoint))

    //     return actions
    //   }
    // }


    // It will handle user input to the content List Menu of that experience like the menu which list out all the audio titles
    async handleContentList(params){
      const {userPhoneNumber,digits} = params
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const language = currentUserObj.language
      const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]

      if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
        const actions = await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
        return {
          nextActions:actions,
          audioSelected:false
        }
      }
      else{
        const experience = this.experienceName
  
        if(currentUserObj.keyMappings.hasOwnProperty(digits)){
          if(digits === "5" || digits === "7"){
            if(digits === "5"){
              await storeLog(userPhoneNumber,digits,`chosen next 4 ${experience}`)
              currentUserObj.currentItemStartingIndex += 4
            }
            else if(digits === "7"){
              await storeLog(userPhoneNumber,digits,`chosen previous 4 ${experience}`)
              currentUserObj.currentItemStartingIndex -= 4
            }
            prepareNext4MenuContent(currentUserObj,this.contents,this.audioTitleToAudioMessageUrl,experience)
            const actions = [
              ...currentUserObj.currentContentActions,
              DTMFInputAction(currentUserObj.currentEndPoint)
            ]
            return {
              nextActions:actions,
              audioSelected:false
            }
          }
          else{
            this.audioTitle = currentUserObj.keyMappings[digits]
            this.audioId = this.audioTitleToId[this.audioTitle]

            await storeLog(userPhoneNumber,digits,`chosen ${this.audioTitle} ${experience}`)

            const announcementActions = [
              getStreamAction(global.pullMenuMainUrl + global.audioGoingTobePlayedDialogUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
              getStreamAction(global.pullMenuMainUrl + global.helpDialogWhileAudioStreamingUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
              getStreamAction(global.pullMenuMainUrl + global.pressKeyMessageUrl.replace('{language}',this.language).replace(/{key}/g,'0').replace('{speechRate}',speechRate),1,false)
            ]
            
            const endpoint = global.communicationApi.getEndPointForAudioContentPlayingInput(experience)
            global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
            
            const actions = [
              announcementActions,
              DTMFInputAction(endpoint)
            ]
            
            return {
              nextActions:actions,
              audioSelected:true,
              audioMetaData:{
                audioId: this.audioId,
                audioTitle: this.audioTitle
              }
            }

          }
        }
        else if(digits === "9"){
          setCurrentExperience(userPhoneNumber,undefined)
          await storeLog(userPhoneNumber,digits,`chosen previous menu(experiences menu)`)
          currentUserObj.currentItemStartingIndex = 0
          prepareNext4MenuContent(currentUserObj,global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
          global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'experience'
          const endpoint = global.communicationApi.getEndPointForExperienceTypeInput()
          global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
          const actions = []
          actions.push(...currentUserObj.currentContentActions)
          actions.push(DTMFInputAction(endpoint))

          return {
            nextActions:actions,
            audioSelected:false
          }

          // await storeLog(userPhoneNumber,digits,`chosen previous menu(${experience} main menu)`)
          // const actions = this.prepareMainMenu()
          // return {
          //   nextActions:actions,
          //   audioSelected:false
          // }
        }
      }
    }

    // It will be triggered when the user opts to go back from audio streaming section
    // And prepares the actions for previous menu
    async handleGoingBackToPreviousMenuFromStreaming(params){
      const { userPhoneNumber,digits } = params
      const experience = this.experienceName
      const currentUserObj = global.monoCallInfo[userPhoneNumber]
      const language = currentUserObj.language
      const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]

      await storeLog(userPhoneNumber,digits,`chosen previous menu(${experience} Content List)`)
      currentUserObj.currentItemStartingIndex = 0
      prepareNext4MenuContent(global.monoCallInfo[userPhoneNumber],this.contents,this.audioTitleToAudioMessageUrl,experience)
      const priorMessageAction = getStreamAction(global.pullMenuMainUrl + global.readingContentTitlesDialogUrl[experience].replace('{language}',this.language).replace('{speechRate}',speechRate))
      
      const endpoint = global.communicationApi.getEndPointForAudioContentListInput(experience)
      global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint

      const actions = [
        priorMessageAction,
        ...global.monoCallInfo[userPhoneNumber]['currentContentActions'],
        DTMFInputAction(endpoint)
      ]

      return actions
    }

    // It will be triggered when the audio streaming is over
    // And it does all the required processing before sending *audioFinishedMessage* back to caller
    async handleStreamFinished(userPhoneNumber){
      this.setAudioState("finished")
      const title = this.audioTitle
      const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][this.language]]

      await storeLog(userPhoneNumber,"audioFinished",`audio with Title ${title} is finished playing.`)

      const audioFinishedMessage = [
        global.pullMenuMainUrl + global.audioFinishedMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)
      ]

      return audioFinishedMessage
    }
  }

  // It makes calls to FSM Executor
  async function callFSM(endPoint,clientEp,name,contextId,type,fsmType='onDemand'){
    try{
      console.log(`endPoint = ${endPoint}`)
      // console.log(`clientEp = ${clientEp}`)
      // console.log(`name = ${name}`)
      // console.log(`contextId = ${contextId}`)
      // console.log(`type = ${type}`)
      // console.log(`version = ${version}`)
      // console.log(`Type of version ${typeof(version)}`)
      const payload = {
        clientEp:clientEp,
        fsmName:name,
        fsmContextId:contextId,
        type:type,
        fsmType:fsmType
      }
      console.log(`payload = `)
      console.log(payload)
      return await axios.post(endPoint,payload)
    }
    catch(error){
      console.log('error in call FSM')
      console.log(error)
      throw error
    }
  }

  // It will be helpful to publish new experiences in PLACE Server
  async function publishFSM(id,fsmType,type){
    try{
      console.log(`Id = ${id}, fsmType = ${fsmType}, type = ${type}`)
      const resp = await axios.post(global.fsmPublishEndPoint,{
        id:id,
        fsmType:fsmType,
        type:type
      })
      return resp.data
    }
    catch(error){
      console.log('error in Publish FSM')
      console.log(error)
      throw error
    }
  }

  // it is for simple HTTP req-res model (Vonage's (Communication API) interactions are excluded as we are handling these interactions separately using an other tryCatchWrapper) in Pull Model
  // If any error occurs anywhere in the code which is wrapped by this function, then it will simply return error message with 500 code
  const tryCatchWrapper1 = f => { 
    return async function() { 
       const [req,res] = arguments 
        try {   
           return await f.apply(this, arguments)  
       } catch (error) {  
           // console.log(JSON.stringify(error, ["message", "arguments", "type", "name"]))  
           console.log(error.message)  
           return res.status(500).json({ message: error.message, stack: error.stack })
        }   
       }
   }

  // it is an error handling wrapper for all the interactions with Communication API (Vonage in this case) in Pull Model
  const tryCatchWrapper2 = f => { 
    return async function() { 
       const [req,res] = arguments 
        try {   
           return await f.apply(this, arguments)  
       } catch (error) {  
           console.log("from wrapper2")
           console.log(error)
           try{
             const params = global.communicationApi.getRequiredParamsFromCommunicationApiBody(req)
             const {userPhoneNumber} = params
             const language = global.monoCallInfo[userPhoneNumber]['language']
             const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
             const internalErrorMessageFullUrl = global.pullMenuMainUrl + global.internalErrorMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)
            
            res.json([getStreamAction(internalErrorMessageFullUrl,1,false)])
  
            setTimeout(() => {
              global.communicationApi.hangUpTheMonoCall(userPhoneNumber)
            },10000)
           }
           catch(error2){}
        }   
       }
   }

   // It will handle all the events from FSM Executor
   // Sends the required params to caller, like keyToEventMappings, eventActions. These params will be useful from caller side further
   function handleEventsFromFSM(events,language='kannada',speechRate){
    const length = events.length
    const data = {
      "keyToEventMappings":{

      },
      "eventActions":[]
    }

    const pressMessagePaths = [
      global.press1Url.replace('{language}',language).replace('{speechRate}',speechRate),
      global.press2Url.replace('{language}',language).replace('{speechRate}',speechRate),
      global.press3Url.replace('{language}',language).replace('{speechRate}',speechRate),
      global.press4Url.replace('{language}',language).replace('{speechRate}',speechRate)
    ]

    for(let i=0;i<length;i++){
      const event = events[i]
      data["keyToEventMappings"][(i+1).toString()] = event.name
      data["eventActions"].push(
        getStreamAction(event.audioData[0][speechRate]["filePath"],1),
        getStreamAction(global.pullMenuMainUrl + pressMessagePaths[i],1)
      )
    }
    return data
   }

function getCurrentExperience(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
      return global.monoCallInfo[userPhoneNumber]['experience']
  }
}

function setCurrentExperience(userPhoneNumber,experience){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    global.monoCallInfo[userPhoneNumber]['experience'] = experience
  }
}

function getCurrentLanguage(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    return global.monoCallInfo[userPhoneNumber]['language']
  }
}

function getCurrentSpeechRateIndex(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    const language = global.monoCallInfo[userPhoneNumber]['language']
    return global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]
  }
}

async function setCurrentSpeechRateIndex(userPhoneNumber,speechRateIndex){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    const language = global.monoCallInfo[userPhoneNumber]['language']
    global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language] = speechRateIndex
    const result = await pullModelUserSettings.findOneAndUpdate(
      {
        phoneNumber: userPhoneNumber,
        "languageLevelSettings.language": language
      },
      {
        $set: {
          "languageLevelSettings.$.speechRateIndex": speechRateIndex
        }
      },
      {
        new: true
      }
    )
    return result
  }
}

function getCurrentEndPoint(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    return global.monoCallInfo[userPhoneNumber]['currentEndPoint']
  }
}
function setAudioStreamState(userPhoneNumber,state){
  const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
  if(obj){
    obj.setAudioState(state)
  }
}

function getAudioStreamState(userPhoneNumber){
  const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
  if(obj){
    return obj.getAudioState()
  }
}

function getAudioStreamId(userPhoneNumber){
  const obj = getCurrentAudioExperienceObjectInMonoCall(userPhoneNumber)
  if(obj){
    return obj.getAudioId()
  }
}

function getQuizObject(userPhoneNumber){
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    return global.monoCallInfo[userPhoneNumber]['quizObj']
  }
}

function removeFSMIdToPhoneNumberMappingForUser(fsmId){
  if(global.fsmIdToPhoneNumber.hasOwnProperty(fsmId)){
    delete global.fsmIdToPhoneNumber[fsmId]
  }
}

// It will be triggered when the call is ended
async function handleCallEndedEvent(userPhoneNumber){
  if(getCurrentExperience(userPhoneNumber) === "quiz"){
    const quizObj = getQuizObject(userPhoneNumber)
    if(quizObj.isEnded === false && quizObj.URLToSendUserInput !== ''){
      axios.post(quizObj.URLToSendUserInput,{
        fsmContextId:quizObj.fsmContextId,
        event:"AbortExperience"
      })
      .then(resp => console.log(resp.data))
      .catch((err) => console.log(err.message))

      // sending Mapping of fsmContextId to PhoneNumbers the Call to SEEDS server
      axios.post(global.fsmContentIdToPhoneNumberMappingUrl,{
        fsmContextId:quizObj.fsmContextId,
        phoneNumbers:[userPhoneNumber]
      },{headers:{ authToken:'postman'}})
      .then(resp => console.log(resp.data))
      .catch((err) => console.log(err.message))
    }
    removeFSMIdToPhoneNumberMappingForUser(quizObj.fsmContextId)
  }
  if(global.monoCallInfo.hasOwnProperty(userPhoneNumber)){
    clearTimeout(global.monoCallInfo[userPhoneNumber]["timeoutIdForUpdateUserTimeInDB"])
  }
  deleteMonoCallObject(userPhoneNumber)
  await setCallEndedDateInMonoCallLog(userPhoneNumber)
}

async function createUserSettingsDocInDB(userSettings){
  return await pullModelUserSettings.create(userSettings)
}

async function updateUserSettingsDocInDB(phoneNumber,settings){
  const {languageLevelSettings} = settings
  return await pullModelUserSettings.findOneAndUpdate(
    {
      phoneNumber: phoneNumber
    },
    {
      $set: {
        languageLevelSettings: languageLevelSettings
      }
    },
    {
      new: true
    }
  )
}

// It will start the timer to end the call in case if the user is inactive(not pressing any keys) for a certain period(look at *inactiveTimeLimitForUserInPullModel* for exact time period)
function setTimerForInactivityOfUserInPullModel(userPhoneNumber){
  const currentUserObj = global.monoCallInfo[userPhoneNumber]
  if(currentUserObj){
    clearTimeout(currentUserObj.inactiveTimeoutId) // will be helful in case previous trigger is not destroyed.
    console.log("starting the timer.")
    currentUserObj.inactiveTimeoutId = setTimeout(() => {
      console.log("HangUp after inactive timeout is gonna triggered.")
      global.communicationApi.hangUpTheMonoCall(userPhoneNumber)
    },global.inactiveTimeLimitForUserInPullModel)
  }
}

// It will stop the timer in case if the user interacted by pressing any key
function stopTimerForInactivityOfUserInPullModel(userPhoneNumber){
  console.log("stopping the timer")
  const currentUserObj = global.monoCallInfo[userPhoneNumber]
  if(currentUserObj){
    clearTimeout(currentUserObj.inactiveTimeoutId)
  }
}

// It will be triggered when this server gets crashed by unexpected situations
async function endAllPullModelCallsInThisServer(){
  for(const userPhoneNumber of Object.keys(global.monoCallInfo)){
    try{
      await global.communicationApi.hangUpTheMonoCallAsync(userPhoneNumber)
    }
    catch(err){
      console.log(`failed to hangup the call for user PhoneNumber = ${userPhoneNumber}`)
    }
  }
}

  module.exports = {
    endAllPullModelCallsInThisServer,
    setTimerForInactivityOfUserInPullModel,
    stopTimerForInactivityOfUserInPullModel,
    createUserSettingsDocInDB,
    updateUserSettingsDocInDB,
    handleCallEndedEvent,
    getAudioStreamId,
    setCurrentSpeechRateIndex,
    getCurrentEndPoint,
    getCurrentLanguage,
    getAudioStreamState,
    setAudioStreamState,
    getCurrentSpeechRateIndex,
    setCurrentExperience,
    getCurrentExperience,
    getCurrentAudioExperienceObjectInMonoCall,
    createMonoCallObjectForUser,
    prepareNext4MenuContent,
    mapTheKeys,
    checkCommonDTMF,
    handleCommonDTMF,
    AudioExperience,
    isValidRequest,
    createMonoCallLog,
    storeLog,
    callFSM,
    publishFSM,
    tryCatchWrapper1,
    tryCatchWrapper2,
    handleEventsFromFSM,
  }
