const { DTMFInputAction, getStreamAction } = require("../../../nccoActions")
const { prepareNext4MenuContent, storeLog, setCurrentExperience } = require("../utils")

class KeyLearning{
    constructor(number,lang){
        this.userPhoneNumber = number
        this.language = lang
    }
    // It will prepare the main menu of Key Learning Experience
    initiate(params){
        const {userPhoneNumber} = params
        const speechRate = global.speechRates[global.monoCallInfo[userPhoneNumber]['speechRateIndex']]
        const messageBeforePlayingKeyGame = [
            getStreamAction(global.pullMenuMainUrl + global.keyLearningWelcomeMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
            getStreamAction(global.pullMenuMainUrl + global.keyLearningPressAnyKeyOnceMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate)),
            getStreamAction(global.pullMenuMainUrl + global.keyLearningPressAnyKeyTwiceMessageUrl.replace('{language}',this.language).replace('{speechRate}',speechRate))
        ]

        const endpoint = global.communicationApi.getEndPointForKeyLearningUserInputInMonoCall()
        global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint

        const actions = [
            messageBeforePlayingKeyGame,
            DTMFInputAction(endpoint,2,1)
        ]

        return actions
    }
    isItExitRequest(digits){
        // when the user presses two digits immediately and that too same digits
        if(digits.length === 2 && digits[0]===digits[1]){
            return true
        }
        else{
            return false
        }
    }
    async handleUserInput(params){
        const {userPhoneNumber,digits} = params
        const currentUserObj = global.monoCallInfo[userPhoneNumber]
        const currentEndPoint = currentUserObj.currentEndPoint
        const language = currentUserObj.language
        const speechRate = global.speechRates[currentUserObj.speechRateIndex]

        if(digits.length === 1){
            // play entered digit and wait for input again...
            await storeLog(userPhoneNumber,digits,`chosen ${digits}`)

            const pressedMessageActions = [
                getStreamAction(global.pullMenuMainUrl + global.keyLearningPressedMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate))
            ]

            if(["0","1","2","3","4","5","6","7","8","9"].includes(digits)){
                pressedMessageActions.push(getStreamAction(global.pullMenuMainUrl + global.keyMessageUrl.replace('{language}',language).replace(/{key}/g,digits).replace('{speechRate}',speechRate)))
            }
            else if(digits === "#"){
                pressedMessageActions.push(getStreamAction(global.pullMenuMainUrl + global.hashMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)))
            }
            else if(digits === "*"){
                pressedMessageActions.push(getStreamAction(global.pullMenuMainUrl + global.starMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)))
            }

            const actions = [
                ...pressedMessageActions,
                DTMFInputAction(currentEndPoint,2)
            ]

            return {
                isExitRequest:false,
                nextActions:actions
            }
        }
        else if(this.isItExitRequest(digits)){
            setCurrentExperience(userPhoneNumber,undefined)
            await storeLog(userPhoneNumber,digits,`chosen Exit from Game.So after exiting game, taking user back to experiences menu.`)
            currentUserObj.nextItemStartingIndex = 0
            prepareNext4MenuContent(currentUserObj,global.experienceNames['english'],global.experienceDialogAudioUrls,'experience')
            global.monoCallInfo[userPhoneNumber]['currentMenuName'] = 'experience'
            const endpoint = global.communicationApi.getEndPointForExperienceTypeInput()
            global.monoCallInfo[userPhoneNumber]['currentEndPoint'] = endpoint
            const gameOverMessageAction = getStreamAction(global.pullMenuMainUrl + global.keyLearningGameOverMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate),1,false)

            const actions = [
                gameOverMessageAction,
                ...currentUserObj.currentContentActions,
                DTMFInputAction(endpoint)
            ]

            return {
                isExitRequest:true,
                nextActions:actions
            }
        }
        else{
            await storeLog(userPhoneNumber,digits,"Invalid Option")

            const actions = [
                DTMFInputAction(currentEndPoint,2)
            ]
            return {
                isExitRequest:false,
                nextActions:actions
            }
        }
    }
}

module.exports = {KeyLearning}