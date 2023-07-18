const { getStreamAction } = require("../../../nccoActions")
const { DTMFInputAction } = require("../../../nccoActions")
const { checkCommonDTMF, handleCommonDTMF, prepareNext4MenuContent, storeLog, setCurrentExperience } = require("../utils")
const axios = require('axios');

module.exports = class Riddle{
    constructor(phoneNumber,experience,lang){
        this.userPhoneNumber = phoneNumber
        this.experienceName = experience
        this.language = lang
        this.questionBlobUrl = undefined
        this.answerBlobUrl = undefined
        this.contents = []
        this.audioTitleToAudioMessageUrl = {}
        this.audioTitleToId = {}
    }

    // It prepares the Main Menu for Riddle Experience
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

    // It will be triggered when the user selects a particular riddle title
    // And prepares the follow-up menu like playing riddle question, asking to guess about answer etc.
    async handleContentListMenu(params){
        const {userPhoneNumber,digits} = params
        const currentUserObj = global.monoCallInfo[userPhoneNumber]

        const language = this.language
        const experience = this.experienceName
        const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
        
        
        if(checkCommonDTMF({...params,validDigits:currentUserObj.keyMappings})){
            const actions = await handleCommonDTMF({...params,validDigits:currentUserObj.keyMappings})
            return  actions
        }
        else{
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
                    return actions
                }
                else{
                    this.audioTitle = currentUserObj.keyMappings[digits]
                    this.audioId = this.audioTitleToId[this.audioTitle]

                    await storeLog(userPhoneNumber,digits,`chosen ${this.audioTitle} ${experience}`)
                    
                    const endpoint = global.communicationApi.getEndPointForRiddleContentPlayingInput(experience)
                    global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
                    
                    const actions = [
                        getStreamAction(global.pullMenuMainUrl + global.playingRiddleQuestionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
                        getStreamAction(global.riddleQuestionUrl.replace('{audioId}',this.audioId).replace('{speechRate}',speechRate)),
                        getStreamAction(global.pullMenuMainUrl + global.repeatRiddleQuestionUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
                        getStreamAction(global.pullMenuMainUrl + global.knowTheAnswerUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
                        getStreamAction(global.pullMenuMainUrl + global.goToPreviousMenuUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
                        DTMFInputAction(endpoint,1,10)
                    ]
                    currentUserObj.currentContentActions = actions
                    currentUserObj.keyMappings = {"0":"","8":"","9":""}
                    return actions
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

                return actions
            }
        }
    }

    // It will be triggered after playing the Riddle Question and when user presses any key like 8 to repeat same question, 0 to reveal answer etc.
    async handleContentPlaying(params){
        const {userPhoneNumber,digits} = params
        const currentUserObj = global.monoCallInfo[userPhoneNumber]

        const language = this.language
        const experience = this.experienceName
        const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex'][language]]
        
        if(currentUserObj.keyMappings.hasOwnProperty(digits)){
            if(digits === "0"){
                await storeLog(userPhoneNumber,digits,`chosen Answer`)
                const actions = []
                const priorActions = [
                    getStreamAction(global.pullMenuMainUrl + global.playingRiddleAnswerUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
                    getStreamAction(global.riddleAnswerUrl.replace('{audioId}',this.audioId).replace('{speechRate}',speechRate),1,false),
                    getStreamAction(global.pullMenuMainUrl + global.takeToRiddleMainMenuUrl.replace('{language}',this.language).replace('{speechRate}',speechRate),1,false),
                ]
                currentUserObj.currentItemStartingIndex = 0
                prepareNext4MenuContent(currentUserObj,this.contents,this.audioTitleToAudioMessageUrl,experience)
                const endpoint = global.communicationApi.getEndPointForAudioContentListInput(experience)
                global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
                actions.push(...priorActions)
                actions.push(...currentUserObj.currentContentActions)
                actions.push(DTMFInputAction(endpoint))
                return actions
            }
            else if(digits === "8"){
                await storeLog(userPhoneNumber,digits,`chosen Repeat Question`)
                const endpoint = currentUserObj.currentEndPoint
                const actions = [
                    getStreamAction(global.riddleQuestionUrl.replace('{audioId}',this.audioId).replace('{speechRate}',speechRate)),
                    DTMFInputAction(endpoint,1,10)
                ]
                return actions
            }
            else if(digits === "9"){
                await storeLog(userPhoneNumber,digits,`chosen previous menu(${experience} main menu)`)
                currentUserObj.currentItemStartingIndex = 0
                prepareNext4MenuContent(currentUserObj,this.contents,this.audioTitleToAudioMessageUrl,experience)
                const endpoint = global.communicationApi.getEndPointForAudioContentListInput(experience)
                global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
                const actions = []
                actions.push(...currentUserObj.currentContentActions)
                actions.push(DTMFInputAction(endpoint))
                return actions
            }
        }
        else{
            await storeLog(userPhoneNumber,digits,"chosen Invalid Option")
            var priorMessageAction = undefined
            if(digits === ""){
                const priorMessageUrl = global.chosenNoOptionUrl.replace('{language}',language).replace('{speechRate}',speechRate)
                priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
            }
            else{
                const priorMessageUrl = global.chosenWrongOptionUrl.replace('{language}',language).replace('{speechRate}',speechRate)
                priorMessageAction = getStreamAction(global.pullMenuMainUrl + priorMessageUrl)
            }
            
            const currentMenuContentActions = currentUserObj.currentContentActions

            const actions = []
            actions.push(priorMessageAction)
            actions.push(...currentMenuContentActions)
            
            return actions
        }
    }
        
}
