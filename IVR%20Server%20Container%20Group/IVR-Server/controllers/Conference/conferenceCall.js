
const { WebPubSubServiceClient } = require("@azure/web-pubsub");

const Conference = require("../../models/conference");
const ConferenceLog = require("../../models/conferenceLog")

const serviceClient = new WebPubSubServiceClient(
  process.env.PUBSUB_CONNECTION_STRING,
  process.env.AZURE_PUBSUB_HUB_NAME
);

async function sendRefreshEvent(confId) {
  await serviceClient.sendToUser(confId, `refresh`, {
    contentType: "text/plain",
  });
}

async function sendMessageToAndroidUser(confId,message){
  await serviceClient.sendToUser(confId, message, {
    contentType: "text/plain",
  });
}

async function createConferenceObject(confId, numbers, names=[]) {
  const exists = await Conference.exists({_id: confId});
  if(!exists){
    const conference = await Conference.create({
      _id: confId,
      participants: [],
      audio:{
        id:"null",
        state:"null"
      },
      leaderPhoneNumber:"null",
      isEnded: false,
    });
    const length = numbers.length
    for (let i=0;i<length;i++) {
      var number = numbers[i]
      const name = names[i]
      const participant = {
        phoneNumber: number,
        name:name,
        status: "null",
        isMuted: false,
        raiseHand:false,
        uuid: "null",
      };
      conference.participants.push(participant);
    }
    await conference.save();
  }
  return "success";
}

async function logClientRequest(confId,date,url,method,type,body){
  const exists = await ConferenceLog.exists({_id: confId});
  if(!exists){
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    await ConferenceLog.create({_id:confId,date:indianTime})
  }
  const clientRequest = {
      date:date,
      url:url,
      method:method,
      type:type,
      body:body
  }
  await ConferenceLog.findById(confId).update({
    $push: { clientRequests : clientRequest },
  });
}

// Reference: https://stackoverflow.com/questions/18391212/is-it-not-possible-to-stringify-an-error-using-json-stringify
function replaceErrors(key, value) {
  if (value instanceof Error) {
      var error = {};

      Object.getOwnPropertyNames(value).forEach(function (propName) {
          error[propName] = value[propName];
      });

      return error;
  }

  return value;
}

async function logError(confId,date,errorObject){
  try{
    const exists = await ConferenceLog.exists({_id: confId});
    if(!exists){
      const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
      await ConferenceLog.create({_id:confId,date:indianTime})
    }
    const error = { date:date, error: JSON.stringify(errorObject,replaceErrors) }
  
    await ConferenceLog.findById(confId).update({
      $push: { errors : error },
    });
  }
  catch(error){
    console.log(error.message)
  }
}

async function updateAudioStateInConference(confId,state){
  await Conference.findById(confId).update(
    {
      $set: {
        "audio.state":state
      },
    }
  );
}

async function updateAudioIdInConference(confId,id){
  await Conference.findById(confId).update(
    {
      $set: {
        "audio.id":id
      },
    }
  );
}

async function getAllPhoneNumbersInAConference(confId){
  const conference = await Conference.findById(confId);
  const participants = conference.participants;
  const phoneNumbers = []
  for(const participant of participants){
    phoneNumbers.push(participant.phoneNumber)
  }
  return phoneNumbers
}
async function endConference(confId) {
  await Conference.findById(confId).update({ isEnded: true });
  communicationApi.stopAudioStreamInConference(confId)
  await communicationApi.hangUpAllInConference(confId)
  const phoneNumbers = await getAllPhoneNumbersInAConference(confId)
  deMapPhoneNumberToConfId(confId,phoneNumbers,global.milliSecondsToWaitToReleaseTheLockOfAPhoneNumberFromAConference)
}

async function endAllConferencesInThisServer(){
  for(const confId of Object.keys(global.confIdToTeacherNumber)){
    try{
      await endConference(confId)
    }
    catch(err){
      console.log(`Failed to end the conference with ID = ${confId}`)
    }
  }
}

async function saveErrorAndSendToAndroidClient(confId,error){
  //log error message and send error message to Android.
  console.log(error)
  const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  await logError(confId,indianTime,error)
  const jsonErrorMessage = JSON.stringify({error:500,message:error.message})
  await serviceClient.sendToUser(confId,jsonErrorMessage, {
    contentType: "text/plain",
  });

}

function getMutedOrUnMutedMessage(confId,userPhoneNumber,isMuted,originatorType){
  var message = ""
  var isMutedText = ""
  if (isMuted) {
    isMutedText = "Muted"
  }
  else{
    isMutedText = "UnMuted"
  }
  if(originatorType==='Teacher' && getTeacherNumberFromConference(confId) === userPhoneNumber){
    message = `You are ${isMutedText}.`
  }
  else if(originatorType==="Leader" && getLeaderPhoneNumberFromConference(confId) === userPhoneNumber){
    message = `You are ${isMutedText}.`
  }
  else{
    message = `You are ${isMutedText} by your ${originatorType}`
  }
  return message
}

async function updateCallStatus(confId, number, status) {
  await Conference.findById(confId).update(
    { "participants.phoneNumber": number },
    {
      $set: {
        "participants.$.status": status,
      },
    }
  );
}

// This function might be deleted if we move to Other Communication API
async function updateCallUUIDInConference(confId,phoneNumber,uuid){
  await Conference.findById(confId).update(
    { "participants.phoneNumber": phoneNumber },
    {
      $set: {
        "participants.$.uuid": uuid,
      },
    }
  );
}

async function updateRaiseHand(confId, number, flag) {
  await Conference.findById(confId).update(
    { "participants.phoneNumber": number },
    {
      $set: {
        "participants.$.raiseHand": flag,
      },
    }
  );
  if(flag){
    sendRefreshEvent(confId)
    // play Message to Teacher that this person raised the Hand.
    const teacherNumber = confIdToTeacherNumber[confId]
    const conference = await Conference.findById(confId);
    const participants = conference.participants;
    const length = participants.length
    var teacherUUID = ""
    var handRaisedUserName = ""
    var flag1=false,flag2=false;
    for(let i=0;i<length;i++){
      const participant = participants[i]
      if(participant.phoneNumber === teacherNumber){
        teacherUUID = participant.uuid
        flag1=true
      }
      if(participant.phoneNumber === number){
        handRaisedUserName = participant.name
        flag2=true
      }
      if(flag1 && flag2){break;}
    }
    playMessage(teacherUUID,`${handRaisedUserName} raised the Hand.`)
  }
}

async function updateMutePropertyOfUserInConference(confId,phoneNumber,flag){
  await Conference.findById(confId).update(
    { "participants.phoneNumber": phoneNumber },
    {
      $set: {
        "participants.$.isMuted": flag,
      },
    }
  );
  if(flag === false){
    await updateRaiseHand(confId, phoneNumber, flag)
  }
}

async function handleConferenceCallControls(confId,action,params,by='Teacher'){
        conf_obj = await Conference.findById(confId)
        if(conf_obj.isEnded){
          return;
        }
        if (action === "end") {
          await endConference(confId);
        }
        else if(action === "muteAll"){
          await communicationApi.muteAll(confId,by)
          await serviceClient.sendToUser(confId, `muteAllDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "unMuteAll"){
          await communicationApi.unMuteAll(confId,by)
          await serviceClient.sendToUser(confId, `unMuteAllDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "forwardStream"){
          await communicationApi.forwardAudioStreamInConference(confId,{})
          await serviceClient.sendToUser(confId, `forwardStreamDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "backwardStream"){
          await communicationApi.rewindAudioStreamInConference(confId,{})
          await serviceClient.sendToUser(confId, `backwardStreamDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "incrementSpeechRate"){
          await communicationApi.increasePlaybackRateOfAudioStreamInConference(confId,{})
          await serviceClient.sendToUser(confId, `incrementSpeechRateDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "decrementSpeechRate"){
          await communicationApi.decreasePlaybackRateOfAudioStreamInConference(confId,{})
          await serviceClient.sendToUser(confId, `decrementSpeechRateDone`, {
            contentType: "text/plain",
          });
        } 
        else if (action === "play") {
          const audioMetaData = {audioId:params.audioId}
          await communicationApi.playAudioStreamInConference(confId,audioMetaData)
          await serviceClient.sendToUser(confId, `playDone`, {
            contentType: "text/plain",
          });
        } 
        else if (action === "pause") {
          await communicationApi.pauseAudioStreamInConference(confId,{})
          await serviceClient.sendToUser(confId, `pauseDone`, {
            contentType: "text/plain",
          });
        } 
        else if (action === "resume") {
          const audioMetaData = {audioId:params.audioId}
          await communicationApi.resumeAudioStreamInConference(confId,audioMetaData)
          await serviceClient.sendToUser(confId, `resumeDone`, {
            contentType: "text/plain",
          });
        }
        else if(action === "mute"){
          await communicationApi.muteUserInConference(confId,params.phoneNumber,by)
          await serviceClient.sendToUser(confId, `muteDone:${params.phoneNumber}`, {
            contentType: "text/plain",
          });
        }
        else if(action === "unmute"){
          await communicationApi.unMuteUserInConference(confId,params.phoneNumber,by)
          await serviceClient.sendToUser(confId, `unmuteDone:${params.phoneNumber}`, {
            contentType: "text/plain",
          });
        }
        else if(action === "lead"){
            var event = params.event

            if(event === "add"){
              var phoneNumber = params.phoneNumber;
              const user = await checkUserExistInConference(confId,phoneNumber)
              if(user){
                if(user.status === "joined"){
                    setLeaderPhoneNumberInConference(confId,phoneNumber)
                    setLeaderNameInConference(confId,user.name)
                    setIntendedLeaderPhoneNumberInConference(confId,phoneNumber)
                    await updateLeader(confId,phoneNumber)
                    await sendRefreshEvent(confId)
                    const messageToParticipant = `${user.name} is assigned as a leader.`
                    const messageToLeader = "you are assigned as a leader"
                    await communicationApi.sendMessageToEverybodyInConference(confId,messageToParticipant,user.phoneNumber,messageToLeader)
                }
                else{
                  setIntendedLeaderPhoneNumberInConference(confId,phoneNumber)
                }
              }
              else{
                const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
                await logError(confId,indianTime,"Leader PhoneNumber Not available in this conference.")
              }
            }
            else if(event === "delete"){
              if(global.confIdToLeaderPhoneNumber.hasOwnProperty(confId)){
                const phoneNumber = getLeaderPhoneNumberFromConference(confId)
                const nameOfLeader = getLeaderNameFromConference(confId)
                deleteLeaderPhoneNumberFromConference(confId)
                deleteLeaderNameFromConference(confId)
                deleteIntendedLeaderPhoneNumberFromConference(confId)
                await updateLeader(confId,"null")
                await sendRefreshEvent(confId)
                const messageToParticipant = `${nameOfLeader} is unassigned from leadership.`
                const messageToLeader = "you are unassigned from leadership"
                await communicationApi.sendMessageToEverybodyInConference(confId,messageToParticipant,phoneNumber,messageToLeader)
              }
              else if(confIdToIntendedLeaderPhoneNumber.hasOwnProperty(confId)){
                deleteIntendedLeaderPhoneNumberFromConference(confId)
              }
            }
        } 
        else if (action === "add") {
            const phoneNumber = params.phoneNumber;
            if(!global.phoneNumberToConfId.hasOwnProperty(phoneNumber)){
              const nameOfParticipant = params.name?params.name:"anonymous"
              const exists = await Conference.exists({
                _id: confId,
                participants: { $elemMatch: { phoneNumber: phoneNumber } },
              });
              if (exists) {
                await Conference.findById(confId).update(
                  { "participants.phoneNumber": phoneNumber },
                  {
                    $set: {
                      "participants.$.name": nameOfParticipant,
                      "participants.$.status": "null",
                      "participants.$.isMuted": false,
                      "participants.$.raiseHand": false,
                      "participants.$.uuid": "null",
                    },
                  }
                );
              } else {
                const newParticipant = {
                  phoneNumber: phoneNumber,
                  name: nameOfParticipant,
                  status: "null",
                  isMuted: false,
                  raiseHand:false,
                  uuid: "null",
                };
                await Conference.findById(confId).update({
                  $push: { participants: newParticipant },
                });
              }
              communicationApi.addParticipantToConference(confId,phoneNumber)
            }
        } 
        else if (action === "remove") {
          const phoneNumber = params.phoneNumber;
          await communicationApi.removeParticipantFromConference(confId,phoneNumber)
        }
      }

async function checkRaiseHand(confId,userPhoneNumber){
  const conference = await Conference.findById(confId);
  const participants = conference.participants;
  const participant = participants.find(
    (participant) => participant.phoneNumber === userPhoneNumber
  );
  if(participant.isMuted){
    await updateRaiseHand(confId, userPhoneNumber, true)
  }
}

async function checkUserExistInConference(confId,userPhoneNumber){
  const conference = await Conference.findById(confId);
  const participants = conference.participants;
  const participant = participants.find(
    (participant) => participant.phoneNumber === userPhoneNumber
  );
  return participant
}

async function updateLeader(confId,userPhoneNumber){
  await Conference.findById(confId).update({leaderPhoneNumber:userPhoneNumber})
}

async function handleUserEventFromAndroidPubsub(req){
  if (req.context.eventName === "message") {
    const confId = req.context.userId;
    //Logging...
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    await logClientRequest(confId,indianTime,"/azurepubsubhook","AndroidWebSocket:Message","WebSocket",req.data)
    const data = req.data;
    const controls = new Set([
      "end","muteAll","unMuteAll",
      "forwardStream","backwardStream",
      "incrementSpeechRate","decrementSpeechRate"
    ])
    if(controls.has(data)){
      await handleConferenceCallControls(confId,data,{})
    }
    else{
      const action = data.split(":")[0]
      switch(action){
        case "play":
          var audioId = data.split(":")[1]
          if(audioId != ""){
            await handleConferenceCallControls(confId,action,{audioId:audioId})
          }
          break;
        case "pause":
          await handleConferenceCallControls(confId,action,{})
          break;
        case "resume":
          var audioId = data.split(":")[1]
          if(audioId !== ""){
            await handleConferenceCallControls(confId,action,{audioId:audioId})
          }
          break;
        case "mute":
        case "unmute":
        case "add":
        case "remove":
          var phoneNumber = data.split(":")[1]
          const params = {phoneNumber:phoneNumber}
          if(action === "add"){
            params['name'] = data.split(":")[2]
          }
          if(phoneNumber !== ""){
            await handleConferenceCallControls(confId,action,params)
          }
          break;
        case "lead":
          if(data.split(":")[1] === "delete"){
            const params = {event:"delete"}
            await handleConferenceCallControls(confId,action,params)
          }
          else{
            var phoneNumber = data.split(":")[1]
            if(phoneNumber.startsWith("91")){
              const params = {phoneNumber:phoneNumber,event:"add"}
              await handleConferenceCallControls(confId,action,params)
            }
          }
          break;
        default:
          console.log(`received invalid action=${action}.`)
          break;
      }
    }
  }
}

function deMapPhoneNumberToConfId(confId,phoneNumberList,afterHowManyMilliSeconds){
  setTimeout(()=>{
    for(const phoneNumber of phoneNumberList){
      if(global.phoneNumberToConfId.hasOwnProperty(phoneNumber)){
        if(global.phoneNumberToConfId[phoneNumber] === confId){
          delete global.phoneNumberToConfId[phoneNumber]
        }
      }
    }
  },afterHowManyMilliSeconds)
}

async function getConferenceObjectById(confId){
  return await Conference.findById(confId);
}

function mapPhoneNumbersToConfId(phoneNumbers,confId){
  for(const phoneNumber of phoneNumbers){
    phoneNumberToConfId[phoneNumber] = confId
  }
}

function messageToBePlayedForStudentsOnPickingTheCallInConference(confId){
  const nameOfTeacher = confIdToTeacherName[confId]
  return `${nameOfTeacher} is inviting you to the conference from the SEEDS App.`
}

function isThisLeaderInConferene(confId,phoneNumber){
  return confIdToLeaderPhoneNumber[confId] === phoneNumber
}

function setLeaderPhoneNumberInConference(confId,userPhoneNumber){
  confIdToLeaderPhoneNumber[confId] = userPhoneNumber
}

function getLeaderPhoneNumberFromConference(confId){
  return confIdToLeaderPhoneNumber[confId]
}

function deleteLeaderPhoneNumberFromConference(confId){
  if(confIdToLeaderPhoneNumber.hasOwnProperty(confId)){
    delete confIdToLeaderPhoneNumber[confId]
  }
}

function setIntendedLeaderPhoneNumberInConference(confId,userPhoneNumber){
  confIdToIntendedLeaderPhoneNumber[confId] = userPhoneNumber
}

function getIntendedLeaderPhoneNumberFromConference(confId){
  return confIdToIntendedLeaderPhoneNumber[confId]
}

function deleteIntendedLeaderPhoneNumberFromConference(confId){
  if(confIdToIntendedLeaderPhoneNumber.hasOwnProperty(confId)){
    delete confIdToIntendedLeaderPhoneNumber[confId]
  }
}

function setLeaderNameInConference(confId,userName){
  confIdToLeaderName[confId] = userName
}

function getLeaderNameFromConference(confId){
  return confIdToLeaderName[confId]
}

function deleteLeaderNameFromConference(confId){
  if(confIdToLeaderName.hasOwnProperty(confId)){
    delete confIdToLeaderName[confId]
  }
}

async function updateConferenceCallAttributes(confId,attribObj){
  await Conference.findById(confId).update(attribObj)
}

function setTeacherNameInConference(confId,userName){
  confIdToTeacherName[confId] = userName
}

function getTeacherNameFromConference(confId){
  return confIdToTeacherName[confId]
}

function deleteTeacherNameFromConference(confId){
  if(confIdToTeacherName.hasOwnProperty(confId)){
    delete confIdToTeacherName[confId]
  }
}

function setTeacherNumberInConference(confId,userPhoneNumber){
  confIdToTeacherNumber[confId] = userPhoneNumber
}

function getTeacherNumberFromConference(confId){
  return confIdToTeacherNumber[confId]
}

function deleteTeacherNumberFromConference(confId){
  if(confIdToTeacherNumber.hasOwnProperty(confId)){
    delete confIdToTeacherNumber[confId]
  }
}

function deleteMusicStateInConference(confId){
  if(confIdToMusicState.hasOwnProperty(confId)){
    delete confIdToMusicState[confId]
  }
}

function createMessageWhenUserLeftTheConference(userName){
  return `${userName} has left the conference.`
}

function createMessageWhenUserJoinedTheConference(userName){
  return `${userName} joined the conference.`
}

function createMessageForUserBeforeGettingDisconnectedByTeacher(){
  return `You are going to be Disconnected by your Teacher.`
}

function doesThisConferenceHaveMusicState(confId){
  return confIdToMusicState.hasOwnProperty(confId)
}

function getMusicStateFromConference(confId){
  return confIdToMusicState[confId]
}

function deletePhoneNumberToConferenceMapping(userPhoneNumber){
  delete global.phoneNumberToConfId[userPhoneNumber]
}

function getConferenceIdFromPhoneNumber(userPhoneNumber){
  return global.phoneNumberToConfId[userPhoneNumber]
}

module.exports = {
  updateCallUUIDInConference,
  checkRaiseHand,
  updateAudioIdInConference,
  updateAudioStateInConference,
  getConferenceIdFromPhoneNumber,
  sendMessageToAndroidUser,
  createMessageForUserBeforeGettingDisconnectedByTeacher,
  deletePhoneNumberToConferenceMapping,
  setLeaderPhoneNumberInConference,
  setLeaderNameInConference,
  setIntendedLeaderPhoneNumberInConference,
  setTeacherNumberInConference,
  setTeacherNameInConference,
  getMutedOrUnMutedMessage,
  getLeaderNameFromConference,
  getLeaderPhoneNumberFromConference,
  getTeacherNameFromConference,
  getTeacherNumberFromConference,
  createMessageWhenUserJoinedTheConference,
  getIntendedLeaderPhoneNumberFromConference,
  doesThisConferenceHaveMusicState,
  getMusicStateFromConference,
  createMessageWhenUserLeftTheConference,
  deleteIntendedLeaderPhoneNumberFromConference,
  deleteMusicStateInConference,
  deleteTeacherNameFromConference,
  deleteTeacherNumberFromConference,
  updateConferenceCallAttributes,
  deleteLeaderPhoneNumberFromConference,
  deleteLeaderNameFromConference,
  isThisLeaderInConferene,
  messageToBePlayedForStudentsOnPickingTheCallInConference,
  mapPhoneNumbersToConfId,
  getConferenceObjectById,
  createConferenceObject, 
  updateCallStatus, // update call status for a single participant in the conference model in db.
  endAllConferencesInThisServer,
  endConference,
  sendRefreshEvent, // sending `refresh` event to Android through websocket.
  updateMutePropertyOfUserInConference, // update mute property to true or false for a single user.
  logClientRequest,
  logError,
  handleConferenceCallControls,
  checkUserExistInConference, // get user from conf DB
  updateLeader, // updates the leader property in conf DB
  handleUserEventFromAndroidPubsub, // delegate events from android app to handleConferenceCallControls
  saveErrorAndSendToAndroidClient, // write error to DB and push error to Android client
  replaceErrors
};
