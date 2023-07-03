  function exchangeTheRoles(gameId){
    for(const number in handCricketInfo[gameId]['userInfo']){
      if(getTheRoleOfUserInGame(gameId,number) == "batsman"){
        setTheRoleOfUserInGame(gameId,number,"bowler")
        setUserEnteredCurrentDigit(gameId,number,null)
      }
      else{
        setTheRoleOfUserInGame(gameId,number,"batsman")
        setUserEnteredCurrentDigit(gameId,number,null)
      }
    }
  }

  function mapPhoneNumbersToGameId(gameId,numbers){
    for(const number of numbers){
      global.phoneNumberToHandCricketGameId[number] = gameId
    }
  }

  function deleteGameIdFromPhoneNumber(userPhoneNumber){
    delete global.phoneNumberToHandCricketGameId[userPhoneNumber]
  }

  function getGameIdFromPhoneNumber(userPhoneNumber){
    return global.phoneNumberToHandCricketGameId[userPhoneNumber]
  }

  function messageToTheUserBeforeTheyJoinTheGame(){
    return "please wait while we are connecting you to the Game Room."
  }

  function createNewHandCricketGameObject(gameId,numbers){
    global.handCricketInfo[gameId] = {'userInfo':{}}
    global.handCricketInfo[gameId]['userInfo'][numbers[0]] = {
      status:null,
      inRoom:false,
      enteredDigit:null,
      role:'batsman',
      score:0
    }

    global.handCricketInfo[gameId]['userInfo'][numbers[1]] = {
        status:null,
        inRoom:false,
        enteredDigit:null,
        role:'bowler',
        score:0
    }

    handCricketInfo[gameId]["phase"] = 0
    
  }

  function deleteHandCricketGameObject(gameId){
    delete global.handCricketInfo[gameId]
  }

  function isHandCricketGameActive(gameId){
    return global.handCricketInfo.hasOwnProperty(gameId)
  }

  function changeCallStatusOfUserInHandCricket(gameId,userPhoneNumber,status){
    global.handCricketInfo[gameId]["userInfo"][userPhoneNumber]['status'] = status
  }

  function setRoomStatusOfUserInHandCricket(gameId,userPhoneNumber,status){
    global.handCricketInfo[gameId]["userInfo"][userPhoneNumber]['inRoom'] = status
  }

  function getOtherUserPhoneNumber(gameId,currentPhoneNumber){
    for(const number in global.handCricketInfo[gameId]['userInfo']){
      if(number != currentPhoneNumber){
        return number
      }
    }
  }

  function checkOtherUserinRoom(gameId,phoneNumber){
    const OtherPhoneNumber = getOtherUserPhoneNumber(gameId,phoneNumber)
    return global.handCricketInfo[gameId]['userInfo'][OtherPhoneNumber]['inRoom']
  }

  function getAllThePhoneNumbersInTheGame(gameId){
    const phoneNumbers = []
    for(const number in global.handCricketInfo[gameId]['userInfo']){
      phoneNumbers.push(number)
    }
    return phoneNumbers
  }

  function initialMessageWhenBotheUsersJoinedTheGame(){
    return 'Both are in same Room. Anybody, Press 5, to start the game.'
  }

  function messageWhenOneUserJoinedButNotOtherYet(){
    return 'Your friend is yet to join the Game Room. we will let you know once he joins'
  }

  function setPhaseLevelOfGame(gameId,phaseLevel){
    global.handCricketInfo[gameId]["phase"] = phaseLevel
  }

  function getPhaseLevelOfGame(gameId){
    return global.handCricketInfo[gameId]["phase"]
  }

  function getTheRoleOfUserInGame(gameId,userPhoneNumber){
    return global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['role']
  }

  function setTheRoleOfUserInGame(gameId,userPhoneNumber,role){
    global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['role'] = role
  }

  function getUserEnteredCurrentDigit(gameId,userPhoneNumber){
    return global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['enteredDigit']
  }
  function setUserEnteredCurrentDigit(gameId,userPhoneNumber,digit){
    global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['enteredDigit'] = digit
  }

  function getTheScoreOfuserInGame(gameId,userPhoneNumber){
    return global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['score']
  }
  function setTheScoreOfuserInGame(gameId,userPhoneNumber,score){
    global.handCricketInfo[gameId]['userInfo'][userPhoneNumber]['score'] = score
  }

  function handleInputFromUser(gameId,userPhoneNumber,digit){
    if(getPhaseLevelOfGame(gameId) === 0){
      console.log("entered phase 0.")
      if(digit === "5"){
        if(checkOtherUserinRoom(gameId,userPhoneNumber)){
          setPhaseLevelOfGame(gameId,1)
          const batsmanMessage = "Game is started, You are the Batsman, press any digit between 1 to 6."
          const bowlerMessage = "Game is started, You are the Bowler, press any digit between 1 to 6."

          var batsmanPhoneNumber = ""
          var bowlerPhoneNumber = ""

          const OtherUserPhoneNumber = getOtherUserPhoneNumber(gameId,userPhoneNumber)

          if(getTheRoleOfUserInGame(gameId,userPhoneNumber) === 'batsman'){
            batsmanPhoneNumber = userPhoneNumber
            bowlerPhoneNumber = OtherUserPhoneNumber
          }
          else{
            batsmanPhoneNumber = OtherUserPhoneNumber
            bowlerPhoneNumber = userPhoneNumber
          }
          
          const params = {
            batsmanPhoneNumber:batsmanPhoneNumber,
            bowlerPhoneNumber:bowlerPhoneNumber,
            batsmanMessage:batsmanMessage,
            bowlerMessage:bowlerMessage
          }

          // calling Communication API to play messages accordingly to start the game.
          communicationApi.startHandCricketGame(gameId,params)
        }
      }
    }
    else{
      if(getUserEnteredCurrentDigit(gameId,userPhoneNumber) === null){
        setUserEnteredCurrentDigit(gameId,userPhoneNumber,digit)
        
        var batsmanPhoneNumber = ""
        var bowlerPhoneNumber = ""
        var batsmanMessage = ""
        var bowlerMessage = ""
        var batsmanScore = 0
        
        const OtherUserPhoneNumber = getOtherUserPhoneNumber(gameId,userPhoneNumber)
        const OtherDigit = getUserEnteredCurrentDigit(gameId,OtherUserPhoneNumber)

        if(getTheRoleOfUserInGame(gameId,userPhoneNumber) === 'batsman'){
          batsmanPhoneNumber = userPhoneNumber
          bowlerPhoneNumber = OtherUserPhoneNumber
          batsmanScore = getTheScoreOfuserInGame(gameId,userPhoneNumber)
        }
        else{
          batsmanPhoneNumber = OtherUserPhoneNumber
          bowlerPhoneNumber = userPhoneNumber
          batsmanScore = getTheScoreOfuserInGame(gameId,OtherUserPhoneNumber)
        }

        console.log(`batsman Score = ${batsmanScore}`)

        if(OtherDigit !== null){
          setUserEnteredCurrentDigit(gameId,userPhoneNumber,null)
          setUserEnteredCurrentDigit(gameId,OtherUserPhoneNumber,null)

          if(digit === OtherDigit){
            if(getPhaseLevelOfGame(gameId) === 1){
              exchangeTheRoles(gameId)
              setPhaseLevelOfGame(gameId,2)
            
              batsmanMessage = `Both have chosen the same numbers. So, you got out. Now, you are the bowler. try to press the same digit as batsman to bowl him.`
              bowlerMessage = `Both have chosen the same numbers. So, the batsman got out. Now, you are the Batsman. try to press the different digit from bowler to chase the score of ${batsmanScore}.`

              const params = {
                batsmanPhoneNumber:batsmanPhoneNumber,
                bowlerPhoneNumber:bowlerPhoneNumber,
                batsmanMessage:batsmanMessage,
                bowlerMessage:bowlerMessage,
              }

              communicationApi.playMessageAndContinueTheHandCricketGame(gameId,params)
            }
            else if(getPhaseLevelOfGame(gameId) === 2){

              var winnerPhoneNumber = ""
              var loserPhoneNumber = ""
              var winnerMessage = ""
              var loserMessage = ""

              const score1 = getTheScoreOfuserInGame(gameId,userPhoneNumber)
              const score2 = getTheScoreOfuserInGame(gameId,OtherUserPhoneNumber)

              if(score1 === score2){
                winnerPhoneNumber = userPhoneNumber
                loserPhoneNumber = OtherUserPhoneNumber
                winnerMessage = `Both have chosen  the same numbers. Your final score is ${score1}. Match is Draw`
                loserMessage = `Both have chosen  the same numbers. Your final score is ${score2}. Match is Draw`
              }
              else if(score1 > score2){
                winnerPhoneNumber = userPhoneNumber
                loserPhoneNumber = OtherUserPhoneNumber
                winnerMessage = `Both have chosen  the same numbers. Your final score is ${score1}. You won the Match.`
                loserMessage = `Both have chosen  the same numbers. Your final score is ${score2}. You lost the Match.`
              }
              else{
                winnerPhoneNumber = OtherUserPhoneNumber
                loserPhoneNumber = userPhoneNumber
                winnerMessage = `Both have chosen  the same numbers. Your final score is ${score2}. You won the Match.`
                loserMessage = `Both have chosen  the same numbers. Your final score is ${score1}. You lost the Match.`
              }

              const params = {
                winnerPhoneNumber:winnerPhoneNumber,
                loserPhoneNumber:loserPhoneNumber,
                winnerMessage:winnerMessage,
                loserMessage:loserMessage
              }

              communicationApi.playMessageAndFinishTheHandCricketGame(gameId,params)
            }
          }
          else{
            var batsmanCurrentScore = ""
            var bowlerScore = ""

            if(getTheRoleOfUserInGame(gameId,userPhoneNumber) === 'batsman'){
              let currentScore = getTheScoreOfuserInGame(gameId,userPhoneNumber)
              currentScore += Number(digit)
              setTheScoreOfuserInGame(gameId,userPhoneNumber,currentScore)
              batsmanCurrentScore = currentScore
              bowlerScore = getTheScoreOfuserInGame(gameId,OtherUserPhoneNumber)
            }
            else{
              let currentScore = getTheScoreOfuserInGame(gameId,OtherUserPhoneNumber)
              currentScore += Number(OtherDigit)
              setTheScoreOfuserInGame(gameId,OtherUserPhoneNumber,currentScore)
              batsmanCurrentScore = currentScore
              bowlerScore = getTheScoreOfuserInGame(gameId,userPhoneNumber)
            }

            if(getPhaseLevelOfGame(gameId) === 2 && batsmanCurrentScore > bowlerScore){
              batsmanMessage = `Both have chosen different numbers. You got the score as ${batsmanCurrentScore}. So, you won the game.`
              bowlerMessage = `Both have chosen different numbers. Batsman got the score as ${batsmanCurrentScore}. So, batsman won the game.`
              
              const params = {
                winnerPhoneNumber: batsmanPhoneNumber,
                loserPhoneNumber: bowlerPhoneNumber,
                winnerMessage: batsmanMessage,
                loserMessage: bowlerMessage
              }

              communicationApi.playMessageAndFinishTheHandCricketGame(gameId,params)
              
            }
            else {
              batsmanMessage = `Both have chosen different numbers. You got the score as ${batsmanCurrentScore}. Try to press a different digit from bowler to score again.`
              bowlerMessage = `Both have chosen different numbers. Batsman got the score as ${batsmanCurrentScore}. Try to press the same digit as batsman to bowl him.`
    
              const params = {
                batsmanPhoneNumber:batsmanPhoneNumber,
                bowlerPhoneNumber:bowlerPhoneNumber,
                batsmanMessage:batsmanMessage,
                bowlerMessage:bowlerMessage,
              }

              communicationApi.playMessageAndContinueTheHandCricketGame(gameId,params)
            }
          }
        }
        else{
          const message = `You have chosen digit ${digit}`
          communicationApi.playMessageToUserInHandCricketGame(gameId,userPhoneNumber,message)
        }
      }
      else{
        return;
      }      
    }
  }

  module.exports = {
    deleteHandCricketGameObject,
    messageToTheUserBeforeTheyJoinTheGame,
    handleInputFromUser,
    setPhaseLevelOfGame,
    getPhaseLevelOfGame,
    messageWhenOneUserJoinedButNotOtherYet,
    getAllThePhoneNumbersInTheGame,
    initialMessageWhenBotheUsersJoinedTheGame,
    deleteGameIdFromPhoneNumber,
    setRoomStatusOfUserInHandCricket,
    changeCallStatusOfUserInHandCricket,
    isHandCricketGameActive,
    getGameIdFromPhoneNumber,
    createNewHandCricketGameObject,
    mapPhoneNumbersToGameId,
    checkOtherUserinRoom
  }
