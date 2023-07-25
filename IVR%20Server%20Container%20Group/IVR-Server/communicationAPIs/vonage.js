class VonageAPI {
  constructor(){
      const Vonage = require("@vonage/server-sdk");
      const privateKey = require("fs").readFileSync(process.env.PRIVATE_KEY_NAME);
      const Queue = require("@supercharge/queue-datastructure");
      const express = require("express");
      const { tryCatchWrapperForReqResModel } = require("../controllers/Conference/utils");
      const { tryCatchWrapper1, tryCatchWrapper2 } = require("../controllers/monoCall/utils");
      // This model captures how much time a user spends on pull model on current day
      this.userSpentTimeForPullModel = require("../models/userSpentTimeForPullModel");
      this.isSlotFreeToMakeCalls = true;
      // This Queue will store all the conference call and pull call requests
      this.queue = new Queue();
      this.vonage = new Vonage({
        apiKey: process.env.VONAGE_API_KEY,
        apiSecret: process.env.VONAGE_API_SECRET,
        applicationId: process.env.VONAGE_APPLICATION_ID,
        privateKey: privateKey,
      });
      // Mappings from vonage call status to standard call status that a client can understand
      this.callStatusMappings = {
        "started":global.callStatuses.started,
        "ringing":global.callStatuses.ringing,
        "answered":global.callStatuses.answered,
        "cancelled":global.callStatuses.cancelled,
        "busy":global.callStatuses.busy,
        "timeout":global.callStatuses.busy,
        "unanswered":global.callStatuses.busy,
        "failed":global.callStatuses.failed,
        "rejected":global.callStatuses.failed,
        "completed":global.callStatuses.completed
      }
      this.UUIDToPhoneNumber = {}
      this.UUIDToConfId = {}
      this.phoneNumberToUUID = {}
      this.confIdToVonageWebsocket = {};
      this.confIdToVonageWebsocketUUID = {};
      this.confIdToActivity = {};
      this.confIdToAudioControls = {};
      this.audioData = {}

      // HandCricket Game
      this.handCricketData = {}
      this.UUIDToGameID = {}

      // MonoCall
      this.monoCallData = {} // It will store all the user related settings in pull model, which are specific to vonage
      this.conversationUUIDToPhoneNumber = {}

      // audio configurations
      this.chunkDurationInSeconds = 25
      this.frameSize = 20 // in MilliSecons. it is Vonage's config and fixed for vonage. So, please don't change as long as you use vonage.
      this.sampleRateOfAudio = 8000 // it is also vonage's specific.
      this.numberOfBytesPerSample = 2 // It is beacause we used 16-bit (or) 2-byte depth while chunking the audio in azure function. It is also vonage's specific.

      const milliSecondsPerSecond = 1000
      this.framesPerSecond = milliSecondsPerSecond / this.frameSize
      this.numberOfFramesPerChunk = this.framesPerSecond * this.chunkDurationInSeconds

      const numberOfSamplesPerFrame = this.sampleRateOfAudio / milliSecondsPerSecond * this.frameSize
      this.numberOfBytesPerFrame = numberOfSamplesPerFrame * this.numberOfBytesPerSample

      const bytesPerSecond = this.sampleRateOfAudio * this.numberOfBytesPerSample
      this.numberOfBytesPerChunk = this.chunkDurationInSeconds * bytesPerSecond

      // routes
      this.router = express.Router();
      this.router.post("/conference_call/recording", tryCatchWrapperForReqResModel(
        async (req,res) => {
          this.handleRecording(req,res)
        }
      ))
      this.router.post("/conference_call/events", (req,res) => {
        if(!req.body || !req.body.uuid){
          res.status(400).json({message:"Body OR UUID Not Available."})
        }
        else{
          this.handleConferenceCallEvents(req)
          res.send("got it.")
        }
      })
      this.router.post("/conversation_events", (req, res) => {
        // console.log("RTC event.");
        // console.log(req.body)
        if(req.body.hasOwnProperty("type") && req.body.type === "audio:dtmf"){
          // console.log(req.body)
          this.handleDTMFFromConversation(req)
        }
        res.send("got it.");
      });
      this.router.post(
        "/conference_websocket_events",(req,res) => {
          this.handleVonageClientWebSocketEventsForConference(req,res)
        }
      );
      
      //HandCricket routes
      this.router.post("/handCricket/events",(req,res) => {
        this.handleHandCricketEvents(req,res)
      })

      // MonoCall routes
      this.router.post("/mono_call/events",tryCatchWrapper1(async (req, res) => {
        await this.handleMonoCallEvents(req);
        res.send("success.");
      }));
      this.router.post("/mono_call/lang_input",tryCatchWrapper2(async (req,res) => {
        await this.handleMonoCallLanguageInput(req,res)
      }))
      this.router.post("/mono_call/theme/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleThemeTypeInputInMonoCall(req,res)
      }))
      this.router.post("/mono_call/experience_type_input",tryCatchWrapper2(async (req,res) => {
        await this.handleMonoCallExperienceTypeInput(req,res)
      }))
      this.router.post(
        "/monocall_websocket_events",(req,res) => {
          this.handleVonageClientWebSocketEventsForMonoCall(req,res)
        }
      );
      // Audio Streaming Experiences
      this.router.post("/mono_call/story/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleMainMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/poetry/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleMainMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/music/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleMainMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/snippet/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleMainMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/story/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleContentListMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/poetry/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleContentListMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/music/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleContentListMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/snippet/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleContentListMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/story/content_playing",tryCatchWrapper2(async (req,res) => {
        await this.handleContentPlayingMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/poetry/content_playing",tryCatchWrapper2(async (req,res) => {
        await this.handleContentPlayingMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/music/content_playing",tryCatchWrapper2(async (req,res) => {
        await this.handleContentPlayingMenuOfAudioExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/snippet/content_playing",tryCatchWrapper2(async (req,res) => {
        await this.handleContentPlayingMenuOfAudioExperienceInMonoCall(req,res)
      }))

      // KeyLearning Experience
      this.router.post("/mono_call/keyLearning/user_input",tryCatchWrapper2(async (req,res) => {
        await this.handleUserInputOfKeyLearningInMonoCall(req,res)
      }))
      // Scramble Experience
      this.router.post("/mono_call/scramble/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleScrambleMainMenuInMonoCall(req,res)
      }))
      this.router.post("/mono_call/scramble/explore_poem_line_ordering",tryCatchWrapper2(async (req,res) => {
        await this.handleScrambleExplorePoemLineOrderingInMonoCall(req,res)
      }))
      this.router.post("/mono_call/scramble/entered_poem_line_sequence",tryCatchWrapper2(async (req,res) => {
        await this.handleScrambleEnteredPoemLineSequenceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/scramble/enterSequenceAgain_or_repeatContent_input",tryCatchWrapper2(async (req,res) => {
        await this.handleScrambleEnterSequenceAgainOrRepeatContentInputInMonoCall(req,res)
      }))

      // Quiz Experience
      this.router.post("/mono_call/quiz/main_menu",tryCatchWrapper2(async (req,res) => {
        await this.handleQuizMainMenuInMonoCall(req,res)
      }))
      this.router.post("/mono_call/quiz/multiple_choice_selection",tryCatchWrapper2(async (req,res) => {
        await this.handleQuizMultipleChoiceSelectionInMonoCall(req,res)
      }))

      // Riddle Experience
      this.router.post("/mono_call/riddle/content_list",tryCatchWrapper2(async (req,res) => {
        await this.handleContentListMenuOfRiddleExperienceInMonoCall(req,res)
      }))
      this.router.post("/mono_call/riddle/content_playing",tryCatchWrapper2(async (req,res) => {
        await this.handleRiddleContentPlayingInMonoCall(req,res)
      }))
  }

  // It checks if vonage is free to make calls. If yes, take request from queue and assigns it to vonage.
  // Note: As there is a limit from vonage side that vonage can make only 3 calls per second, we are doing this thing.
  // This function gets called recursively in every 1 sec. Go though definition to better understand exactly what is happening.
  async checkSlotAndQueue() {
      try{
      if (this.isSlotFreeToMakeCalls && this.queue.isNotEmpty()) {
          this.isSlotFreeToMakeCalls = false;
          const callObj = this.queue.dequeue()
          if(callObj.callType === "conference"){
          const { saveErrorAndSendToAndroidClient } = require("../controllers/Conference/conferenceCall");
          const conference = callObj
          const numbers = conference.numbers;
          const confId = conference.confId
          setTimeout(async () => {
              try{
              await this.makeTeacherCall(confId,numbers);
              }
              catch(error){
              if(confId){
                  saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
              }
              this.isSlotFreeToMakeCalls = true
              this.startCheckingSlot(0)
              }
          }, 1000);
          }
          else if(callObj.callType === "mono"){
          setTimeout(async () => {
              try{
              await this.makeMonoCall(callObj.phoneNumber);
              }
              catch(error){
              console.log(error.message)
              this.isSlotFreeToMakeCalls = true
              this.startCheckingSlot(0)
              }
          }, 1000);
          }
          else if(callObj.callType === "handCricket"){
          
            setTimeout(async () => {
              try{
                this.makeHandCricketCall(callObj.gameId,callObj.numbers)
              }
              catch(error){
                console.log(error.message)
                this.isSlotFreeToMakeCalls = true
                this.startCheckingSlot(0)
              }
            }, 1000)
          }
          else if(callObj.callType === "addToConference"){
          const confId = callObj.confId
          setTimeout(async () => {
              try{
              await this.makeCallToUserInConference(confId,callObj.phoneNumber)
              }
              catch(error){
              if(confId){
                  saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
              }
              this.isSlotFreeToMakeCalls = true
              this.startCheckingSlot(0)
              }
          }, 1000)
          }
          else{
          this.isSlotFreeToMakeCalls = true
          this.startCheckingSlot(0)
          }
      } else {
          // console.log("Not Free Or Queue is Empty");
          this.startCheckingSlot(0)
      }
      }
      catch(error){
        console.log(error)
        this.isSlotFreeToMakeCalls = true
        this.startCheckingSlot(0)
      }
  }

  startCheckingSlot(AfterHowManySeconds=0){
    // slot checking...
      setTimeout(() => {
        this.checkSlotAndQueue()
      }
      , AfterHowManySeconds
      );
  }
  
  // It will simply queue up the conference call request
  makeConference(confId,listOfUserPhoneNumbers){
    this.queue.enqueue({
      callType:"conference",
      numbers: listOfUserPhoneNumbers,
      confId: confId
    });
  }

  async hangUpTheMonoCallAsync(userPhoneNumber){
    const uuid = this.phoneNumberToUUID[userPhoneNumber]
    await this.updateCallLeg(uuid, "hangup")
  }

  hangUpTheMonoCall(userPhoneNumber){
    const uuid = this.phoneNumberToUUID[userPhoneNumber]
    this.updateCallLeg(uuid, "hangup")
    .then(console.log)
    .catch(console.log)
  }

  // It will be invoked once the time limit for a user on pull model on that day, is over
  playTimeOverMessageAndCutTheCall(userPhoneNumber){
    const uuid = this.phoneNumberToUUID[userPhoneNumber]
    const message = "Your Time For Today is Over. So we are going to End the Call. Please try this on tomorrow."
    this.playTTSMessage(uuid,message)
    const timeToWaitToEndTheCall = Math.ceil(message.length/10) * 1000
    setTimeout(() => { 
      this.updateCallLeg(uuid, "hangup")
      .then(console.log)
      .catch(console.log)
    }, timeToWaitToEndTheCall)
  }

  // it makes a call to a phone Number with specific NCCO (Nexmo Call control object. It is vonage's version of CCO)
  makeVonageCall(phoneNumber,eventUrl,call_back_func,ncco){
      this.vonage.calls.create(
        {
          to: [
            {
              type: "phone",
              number: phoneNumber,
            },
          ],
          from: {
            type: "phone",
            number: process.env.VONAGE_NUMBER,
          },
          event_url: eventUrl,
          ncco: ncco,
        },call_back_func
      )
  }

  // As name indicates, it creates the NCCO to connect to a websocket endpoint
  createWebSocketNCCO(headers,socketEndpoint,eventEndpoint){
      const obj = {
        action: "connect",
        from: "NexmoTest",
        eventType: "synchronous",
        eventUrl: [global.remoteUrl + eventEndpoint],
        endpoint: [
          {
            type: "websocket",
            uri: `${process.env.SEEDS_IVR_SERVER_WEBSOCKET_BASE_URL}${socketEndpoint}`,
            "content-type": "audio/l16;rate=8000",
            headers: headers,
          },
        ],
      }
      return obj
  }
  
  // this is to mute,unmute,hangup using UUID of a call
  updateCallLeg(uuid, action) {
      // console.log(`${uuid} ${action}.`);
      return new Promise((resolve, reject) => {
        this.vonage.calls.update(uuid, { action: action }, (err, res) => {
          if (err) {
            reject(err);
          } else {
            resolve(res);
          }
        });
      });
  }

  // make the teacher call, after teacher picks up, connect teacher to websocket and then add both of them to conference named with confID.
  async makeTeacherCall(confId, numbers) {
      const { 
        getConferenceObjectById, 
        saveErrorAndSendToAndroidClient, 
        mapPhoneNumbersToConfId,
        deletePhoneNumberToConferenceMapping 
      } = require("../controllers/Conference/conferenceCall");
      const conference = await getConferenceObjectById(confId)
      if (!conference.isEnded) {
        var teacherNumber = numbers[0];
        const socketEndpoint = "vonage/websocket_conference"
        const eventEndpoint = "vonage/conference_websocket_events"
        const headers = {
          confId: confId,
        }
        const ncco = [
          this.createWebSocketNCCO(headers,socketEndpoint,eventEndpoint),
          {
            action: "conversation",
            name: confId,
            // record:true,
            eventMethod:"POST",
            // eventUrl:[global.remoteUrl + "vonage/conference_call/recording"]
          },
        ];
    
        const eventUrl = [global.remoteUrl + "vonage/conference_call/events"]
    
        const call_back_func = async (err, resp) => {
          var conference = undefined
          try{
            if (err) {
              deletePhoneNumberToConferenceMapping(number)
              throw {
                message:`Vonage is not able to make call to this number ${teacherNumber}`
              }
            }
            conference = await getConferenceObjectById(confId)
            if (conference.isEnded) {
              await this.updateCallLeg(resp.uuid, "hangup")
            }
          }
          catch(error){
            if(confId){
              saveErrorAndSendToAndroidClient(confId,error)
              .then(console.log)
              .catch(console.log)
            }
          }
          if (!conference || (conference.isEnded || numbers.length == 1)) {
            this.isSlotFreeToMakeCalls = true;
            this.startCheckingSlot(0)
          }
          else {
            setTimeout(async () => {
              try{
                await this.makeCallsToStudents(confId, numbers, 1, numbers.length)
              }
              catch(error){
                if(confId){
                  saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
                }
                this.isSlotFreeToMakeCalls = true
                this.startCheckingSlot(500)
              }
            }, 1000);
          }
        }
        mapPhoneNumbersToConfId([teacherNumber],confId)
        this.makeVonageCall(teacherNumber,eventUrl,call_back_func,ncco)
      } else {
        // I think we can ignore this below logic as we are mapping it only when the *if condition* falls to true.
        for(const number of numbers){
          deletePhoneNumberToConferenceMapping(number)
        }
        this.isSlotFreeToMakeCalls = true;
        this.startCheckingSlot(0)
      }
    }

    // It makes calls to students. Once calls are over, it releases the lock of vonage, it means vonage is free to take other requests from then
    async makeCallsToStudents(confId, numbers, startIndex, n) {
      const { 
        getConferenceObjectById, 
        saveErrorAndSendToAndroidClient, 
        mapPhoneNumbersToConfId, 
        messageToBePlayedForStudentsOnPickingTheCallInConference,
        deletePhoneNumberToConferenceMapping 
      } = require("../controllers/Conference/conferenceCall");
      const conference = await getConferenceObjectById(confId)

      if (!conference.isEnded) {
        var ct = 0;
        var current_length = Math.min(n - startIndex, 3);
        const eventUrl = [global.remoteUrl + "vonage/conference_call/events"]
        const ncco = [
          {
            action:"talk",
            text:messageToBePlayedForStudentsOnPickingTheCallInConference(confId),
            language:"en-IN",
            style:4
          },
          {
            action: "conversation",
            name: confId,
            // record:true,
            eventMethod:"POST",
            // eventUrl:[global.remoteUrl + "vonage/conference_call/recording"]
          },
        ];
    
        for (let i = startIndex; i < Math.min(startIndex + 3, n); i++) {

          const call_back_func = async (err, resp) => {
            // console.log(numbers[i] + "followed by ");
            var conference = undefined
            try{
              if(err){
                deletePhoneNumberToConferenceMapping(numbers[i])
                throw {
                  message:`Vonage is not able to make call to this number ${numbers[i]}`
                }
              }
              conference = await getConferenceObjectById(confId)
              if (conference.isEnded) {
                await this.updateCallLeg(resp.uuid, "hangup")
              }
            }
            catch(error){
              if(confId){
                saveErrorAndSendToAndroidClient(confId,error)
                .then(console.log)
                .catch(console.log)
              }
            }
            ct += 1;
            if (ct == current_length) {
              if (!conference || (conference.isEnded || startIndex + 3 >= n)) {
                this.isSlotFreeToMakeCalls = true;
                this.startCheckingSlot(0)
              } else {
                setTimeout(async () => {
                  try{
                    await this.makeCallsToStudents(confId, numbers, startIndex + 3, n);
                  }
                  catch(error){
                    if(confId){
                      saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
                    }
                    this.isSlotFreeToMakeCalls = true
                    this.startCheckingSlot(500)
                  }
                }, 1000);
              }
            }
          }
          mapPhoneNumbersToConfId([numbers[i]],confId)
          this.makeVonageCall(numbers[i],eventUrl,call_back_func,ncco)
        }
      } else {
        for(let i=startIndex;i<n;i++){
          deletePhoneNumberToConferenceMapping(numbers[i])
        }
        this.isSlotFreeToMakeCalls = true;
        this.startCheckingSlot(0)
      }
    }
  
  handleRecording(req,res){
    console.log("From Recording...")
    console.log(req.body)
    this.vonage.files.save(req.body.recording_url, `recordings/${req.body.conversation_uuid}.mp3`, (err, res) => {
      if(err) { console.error(err); }
      else {
          console.log(res);
      }
    });
    res.send("got it.")
  }
  
  // This name might be confusing. Apologies for that.
  // But the intention of this function is to save any required data in DB and then removes from Cache (Local Memory) Upon Ending the Conference
  async SaveAndDeleteConferenceInfo(confId) {
    const { 
      deleteTeacherNameFromConference, 
      deleteTeacherNumberFromConference,
      deleteMusicStateInConference,
      deleteLeaderPhoneNumberFromConference,
      deleteLeaderNameFromConference,
      deleteIntendedLeaderPhoneNumberFromConference,

    } = require("../controllers/Conference/conferenceCall");
    // This function will be called even after few minutes of endConference function called. i.e. teacher ends the call deliberately, to clear any dependencies.
    if (
      this.confIdToAudioControls.hasOwnProperty(confId) &&
      this.confIdToAudioControls[confId].hasOwnProperty("setTimeOutId")
    ) {
      clearTimeout(this.confIdToAudioControls[confId].setTimeOutId);
      delete this.confIdToAudioControls[confId];
    }
    if (this.confIdToVonageWebsocket.hasOwnProperty(confId)) {
      delete this.confIdToVonageWebsocket[confId];
    }
    if (this.confIdToActivity.hasOwnProperty(confId)) {
      delete this.confIdToActivity[confId];
    }
    deleteTeacherNumberFromConference(confId)
    deleteTeacherNameFromConference(confId)
    deleteMusicStateInConference(confId)
    deleteLeaderPhoneNumberFromConference(confId)
    deleteLeaderNameFromConference(confId)
    deleteIntendedLeaderPhoneNumberFromConference(confId)

    if(this.audioData.hasOwnProperty(confId)){
      delete this.audioData[confId]
    }
  }
  
  async disConnectWebsocket(confId) {
    if (this.confIdToVonageWebsocketUUID.hasOwnProperty(confId)) {
      const uuid = this.confIdToVonageWebsocketUUID[confId];
      await this.vonage.calls.update(uuid, { action: "hangup" })
      delete this.confIdToVonageWebsocketUUID[confId];
    }
  }

  // Not a good name for this function. Apologies for that. Feel free to change it but take care of breakages that will occur
  // if active user count is 0, websocket will get disconnected, set EndState of Conference to true in DB.
  async checkConferenceExistency(confId) {
    const { updateConferenceCallAttributes, saveErrorAndSendToAndroidClient } = require("../controllers/Conference/conferenceCall");
    try{
      if (this.confIdToActivity.hasOwnProperty(confId)) {
        if (this.confIdToActivity[confId]["activeUserCount"] == 0) {
          await this.disConnectWebsocket(confId);
          await this.SaveAndDeleteConferenceInfo(confId);
          await updateConferenceCallAttributes(confId,{ isEnded: true })
        }
      }
    }
    catch(error){
      saveErrorAndSendToAndroidClient(confId,error)
      .then(console.log)
      .catch(console.log)
    }
  }

  // send message to everybody except the originator. send different message to originator.
  async sendMessageToEverybodyInConference(confId,message,originatorPhoneNumber,originMessage){
    const { 
      getConferenceObjectById,
      doesThisConferenceHaveMusicState,
      getMusicStateFromConference,
      handleConferenceCallControls, 
    } = require("../controllers/Conference/conferenceCall");
    const conference = await getConferenceObjectById(confId);
    const participants = conference.participants;
    var text = ""
    const originUUID = this.phoneNumberToUUID[originatorPhoneNumber]
    // const audioId = confIdToAudioControls[confId].currentAudioId
  
    if(doesThisConferenceHaveMusicState(confId)){
      // here 1 means playing, 0 means it is in pause state.
      if(getMusicStateFromConference(confId) === 1){
        await handleConferenceCallControls(confId,"pause",{})
      }
    }
    
    for (const participant of participants) {
      const uuid = participant.uuid;
      if (uuid !== "null") {
        if(uuid === originUUID){
          text = originMessage
        }
        else{
          text = message
        }
        this.playTTSMessage(uuid,text)
      }
    }
  }

  // handle conference call events from vonage.
  async handleConferenceCallEvents(req){
    const { 
      logClientRequest, 
      updateCallStatus, 
      sendRefreshEvent,
      saveErrorAndSendToAndroidClient,
      isThisLeaderInConferene, 
      deleteLeaderPhoneNumberFromConference, 
      deleteLeaderNameFromConference,
      checkUserExistInConference,
      createMessageWhenUserLeftTheConference,
      getIntendedLeaderPhoneNumberFromConference,
      createMessageWhenUserJoinedTheConference,
      deletePhoneNumberToConferenceMapping,
      getConferenceIdFromPhoneNumber,
      handleConferenceCallControls,
      updateCallUUIDInConference,
      updateLeader
    } = require("../controllers/Conference/conferenceCall");
    var confId = undefined
    try{
      const uuid = req.body.uuid;
      if (req.body.hasOwnProperty("status")) {
        var number = req.body.to;
  
        if(this.UUIDToConfId.hasOwnProperty(uuid)){
          confId = this.UUIDToConfId[uuid];
        }
        else{
          confId = getConferenceIdFromPhoneNumber(number)
          this.UUIDToConfId[uuid] = confId
          this.UUIDToPhoneNumber[uuid] = number
          this.phoneNumberToUUID[number] = uuid
          await updateCallUUIDInConference(confId,number,uuid)
        }
  
        const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
        await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(req.body))
  
        const status = req.body.status;
        
        if(["busy","failed","rejected","cancelled","timeout","unanswered"].includes(status)) {
          await updateCallStatus(confId, number, this.callStatusMappings[status]);
          if(status === "failed"){
            deletePhoneNumberToConferenceMapping(number)
            delete this.UUIDToConfId[uuid]
            delete this.UUIDToPhoneNumber[uuid]
            delete this.phoneNumberToUUID[number]
          }
          // sending refresh event to the android app to let that get this updated data
          await sendRefreshEvent(confId);
        } else if (status !== "completed") {
          await updateCallStatus(confId, number, this.callStatusMappings[status]);
          await sendRefreshEvent(confId);
        } else if (status === "completed") {
          // delete all the global variable for this user.
          deletePhoneNumberToConferenceMapping(number)
          delete this.UUIDToConfId[uuid]
          delete this.UUIDToPhoneNumber[uuid]
          delete this.phoneNumberToUUID[number]

          if (req.body.duration === "0") {
            return;
          }
          await updateCallStatus(confId, number, this.callStatusMappings[status]);
          if(isThisLeaderInConferene(confId,number)){
            deleteLeaderPhoneNumberFromConference(confId)
            deleteLeaderNameFromConference(confId)
            await updateLeader(confId,"null")
          }
          await sendRefreshEvent(confId);
          if (this.confIdToActivity.hasOwnProperty(confId)) {
            this.confIdToActivity[confId]["activeUserCount"] -= 1;
            if (this.confIdToActivity[confId]["activeUserCount"] == 0) {
              setTimeout(() => {
                this.checkConferenceExistency(confId);
              }, 60000);
            }
          }
          const user = await checkUserExistInConference(confId,number)
          if(user){
            const message = createMessageWhenUserLeftTheConference(user.name)
            await this.sendMessageToEverybodyInConference(confId,message)
          }
        }
      }
      if (req.body.hasOwnProperty("type")) {
        if (req.body.type === "transfer") {
          const phoneNumber = this.UUIDToPhoneNumber[uuid]
          confId = this.UUIDToConfId[uuid]
          
          const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
          await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(req.body))
  
          if (!this.confIdToActivity.hasOwnProperty(confId)) {
            this.confIdToActivity[confId] = {};
            this.confIdToActivity[confId]["activeUserCount"] = 0;
          }
          this.confIdToActivity[confId]["activeUserCount"] += 1;
          await updateCallStatus(confId, phoneNumber, global.callStatuses.joined);
          // handle if the user who joined is intended leader or not.
          if(phoneNumber === getIntendedLeaderPhoneNumberFromConference(confId)){
            const params = {phoneNumber:phoneNumber,event:"add"}
            await handleConferenceCallControls(confId,"lead",params)
          }
          else{
            await sendRefreshEvent(confId);
            const user = await checkUserExistInConference(confId,phoneNumber)
            if(user){
              const message = createMessageWhenUserJoinedTheConference(user.name)
              const joinerMessage = createMessageWhenUserJoinedTheConference('You')
              await this.sendMessageToEverybodyInConference(confId,message,user.phoneNumber,joinerMessage)
            }
          }
        }
      }
      console.log(req.body);
    }
    catch(error){
      if(confId){
        saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
      }
    }
  }

stopAudioStreamInConference(confId){
  if (
    this.confIdToAudioControls.hasOwnProperty(confId) &&
    this.confIdToAudioControls[confId].hasOwnProperty("setTimeOutId")
  ) {
    clearTimeout(this.confIdToAudioControls[confId].setTimeOutId);
  }
}

async hangUpAllInConference(confId){
  const { getConferenceObjectById,logError } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;
  for (const participant of participants) {
    const uuid = participant.uuid;
    if (uuid !== "null") {
      this.updateCallLeg(uuid, "hangup").then(console.log).catch(async (error) => {
        try{
          const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
          await logError(confId,indianTime,error)
        }
        catch(error){
          console.log(error.message)
        }
      })
    }
  }
  await this.disConnectWebsocket(confId)
}

// it plays tts message in call using call UUID
playTTSMessage(uuid,message,ttsLang='en-IN',style=4,speechRate='medium',loop=1){
  this.vonage.calls.talk.start(uuid, { text: `<speak> <prosody rate='${speechRate}'> ${message}</prosody> </speak>`, language:ttsLang,style:style,loop:loop }, (err, res) => {
    if(err) { console.error(err); }
  });
}

// it stops ongoing tts message in call using call UUID
stopTTSMessage(uuid){
  this.vonage.calls.talk.stop(uuid,(err,res) => {
    if(err){
      // console.error(err);
      console.log(`unable to stop`)
      console.log(`UUID = ${uuid}`)
    }
  })
}

// it plays audio file in call using UUID
playAudioMessage(uuid,streamUrl,loop=1,level=1){
  this.vonage.calls.stream.start(uuid,{stream_url:streamUrl,loop:loop,level:level},(err,resp) => {
    if(err) console.error(err)
  })
}

// it stops ongoing audio message in call using UUID
stopAudioMessage(uuid){
  this.vonage.calls.stream.stop(uuid,(err,resp) => {
    if(err) console.error(err)
  })
}

// Mute Everybody except Originator/Initiator
async muteAll(confId,by='Teacher'){
  const { 
    getConferenceObjectById, 
    updateMutePropertyOfUserInConference,
    getTeacherNumberFromConference,
    getLeaderPhoneNumberFromConference,
    getMutedOrUnMutedMessage,
    sendRefreshEvent,

  } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;

  var originatorPhoneNumber = ''
  if(by==='Teacher'){
    originatorPhoneNumber = getTeacherNumberFromConference(confId)
  }
  else if(by==='Leader'){
    originatorPhoneNumber = getLeaderPhoneNumberFromConference(confId)
  }

  for (const participant of participants) {
    const uuid = participant.uuid;
    const phoneNumber = participant.phoneNumber
    
    if (originatorPhoneNumber !== phoneNumber && uuid !== "null") {
      if(participant.isMuted){
        continue;
      }
      await this.updateCallLeg(uuid, "mute")
      await updateMutePropertyOfUserInConference(confId,phoneNumber,true)
      // await sendRefreshEvent(confId);

      const message = getMutedOrUnMutedMessage(confId,phoneNumber,true,by)
      this.playTTSMessage(uuid,message)

    }
  }
}

// Unmute Everybody including Originator/Initiator
async unMuteAll(confId,by='Teacher'){
  const { 
    getConferenceObjectById, 
    updateMutePropertyOfUserInConference,
    getMutedOrUnMutedMessage,
    sendRefreshEvent,

  } = require("../controllers/Conference/conferenceCall");

  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;

  for (const participant of participants) {
    const uuid = participant.uuid;
    const phoneNumber = participant.phoneNumber
    if (uuid !== "null" && participant.isMuted) {
      await this.updateCallLeg(uuid, "unmute")
      await updateMutePropertyOfUserInConference(confId,phoneNumber,false)
      // await sendRefreshEvent(confId);
      const message = getMutedOrUnMutedMessage(confId,phoneNumber,false,by)
      this.playTTSMessage(uuid,message)
    }
  }
}

// Gets Frames of an audio Chunk (it is 25-sec audio chunk currently) and populate it in our cache to stream it further
async loadChunkAndMapToFrames(conversationId,blobName,currentChunkNumber,contentLength){
  // conversationId can be confId(In Conference call) or userPhoneNumber(in Pull Model) as these are unique to a conversation
  const { getChunkAsFrames } = require("../audioManipulation")
  // This if condition is to satisfy the security constraints of github's code security scanning.
  if(conversationId === '__proto__' || conversationId === 'constructor' || conversationId === 'prototype') {
    throw "Prototype-polluting assignment (Prototype pollution Attack)"
  }
  this.audioData[conversationId] = {}
  if(currentChunkNumber > 0){
    const startBytePos = (currentChunkNumber - 1) * this.numberOfBytesPerChunk
    if(startBytePos < contentLength){
      const endBytePos = Math.min(currentChunkNumber * this.numberOfBytesPerChunk, contentLength)
      const numberOfBytesRequired = endBytePos - startBytePos
      const frames = await getChunkAsFrames(blobName,startBytePos,numberOfBytesRequired)
      this.audioData[conversationId][currentChunkNumber] = frames
    }
  }
}

// It streams audio in conference by sending audio bytes to vonage using websocket connection
async streamAudioInConference(confId,audioId,gap) {
  try{
    // This if condition is to satisfy the security constraints of github's code security scanning.
    if(confId === '__proto__' || confId === 'constructor' || confId === 'prototype') {
      throw "Prototype-polluting assignment (Prototype pollution Attack)"
    }
    var currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
    if(this.audioData[confId].hasOwnProperty(currentChunkNumber)){
      var currentFrameNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"]
      const frame = this.audioData[confId][currentChunkNumber][currentFrameNumber]
      if(frame){
        const buffer = Uint8Array.from(frame).buffer
        // writing this try..catch just to not interrupt other packets from being sent
        try{
          this.confIdToVonageWebsocket[confId].send(
            buffer
          );
        }
        catch(error){
          console.log(error.message)
        }
        if(currentFrameNumber === this.numberOfFramesPerChunk - 1){
          delete this.audioData[confId][currentChunkNumber]
          currentChunkNumber += 1
          this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] += 1
          this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = 0
          const contentLength = this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"]
  
          const startBytePos = (currentChunkNumber - 1) * this.numberOfBytesPerChunk
  
          if(startBytePos < contentLength){
            const { getChunkAsFrames } = require("../audioManipulation")
            const endBytePos = Math.min(currentChunkNumber * this.numberOfBytesPerChunk,contentLength)
            const numberOfBytesRequired = endBytePos - startBytePos
            const speechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
            const speechRate = global.speechRates[speechRateIndex]
            const blobName = `${audioId}/${speechRate}.wav`
            const frames = await getChunkAsFrames(blobName,startBytePos,numberOfBytesRequired)
            this.audioData[confId][currentChunkNumber] = frames
          }
          else{
            console.log("audio is over while going nextChunk.")
            return;
          }
        }
        else{
          this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] += 1
        }
        this.confIdToAudioControls[confId].setTimeOutId = setTimeout(() => {
          this.streamAudioInConference(confId, audioId,20);
        }, gap);
      }
      else{
        // This is to make sure that when we check next time, this chunkNumber should result out of contentLength.
        this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] += 1
        this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = 0
        delete this.audioData[confId][currentChunkNumber]
        console.log("audio is over as frame is undefined in conference call.")
      }
    }
    else{
      console.log(this.audioData[confId])
      console.log(`audio is over as invalid ChunkNumber. ${currentChunkNumber}`)
    }
  }
  catch(error){
    const { saveErrorAndSendToAndroidClient } = require("../controllers/Conference/conferenceCall");
    console.log("error occured in Play Audio function...")
    console.log(error.message)
    saveErrorAndSendToAndroidClient(confId,error)
    .then(console.log)
    .catch(console.log)
  }
}

async setAudioConfigInTheConference(confId,audioId){
  const { getLengthOfBlob } = require("../audioManipulation")
  const currentSpeechRate = global.speechRates[global.defaultSpeechRateIndex]
  const contentLength = await getLengthOfBlob(`${audioId}/${currentSpeechRate}.wav`)
  this.confIdToAudioControls[confId].audioIdToState[audioId] = {
    "chunkNumber":1,
    "frameNumber":0,
    "speechRateIndex":global.defaultSpeechRateIndex,
    "contentLength":contentLength
  };
}

// Not a good name for this function. Apologies for that.
// It does some pre-processing before calling streaming function. Feel free to change the name and take care of breakages
async playAudioStreamInConference(confId,audioMetaData){
  const { blobExists } = require("../audioManipulation")
  const { updateAudioIdInConference, updateAudioStateInConference, sendRefreshEvent, logError } = require("../controllers/Conference/conferenceCall")
  const { audioId } = audioMetaData
  if(this.confIdToAudioControls.hasOwnProperty(confId)){
    // Here i am using 1.0 as it exists in each audio Folder. Note: it is impossible to check by using folder name only.
    if(await blobExists(`${audioId}/1.0.wav`)){
      this.stopAudioStreamInConference(confId)
      if (!this.confIdToAudioControls[confId].audioIdToState.hasOwnProperty(audioId)) {
        await this.setAudioConfigInTheConference(confId,audioId)
      } 
      else {
        this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] = 1
        this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = 0
      }
      await updateAudioIdInConference(confId,audioId)
      this.confIdToAudioControls[confId].currentAudioId = audioId
      
      const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
      const currentSpeechRate = global.speechRates[currentSpeechRateIndex]
      const blobName = `${audioId}/${currentSpeechRate}.wav`
      const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
      const contentLength = this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"]
      await this.loadChunkAndMapToFrames(confId,blobName,currentChunkNumber,contentLength)

      this.streamAudioInConference(confId,audioId,18);

      global.confIdToMusicState[confId] = 1
      await updateAudioStateInConference(confId,"play")
      // await sendRefreshEvent(confId)
    }
    else{
      const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
      await logError(confId,indianTime,`${audioId} Blob Not Exists Error.`)
    }
  }        
}

async pauseAudioStreamInConference(confId,audioMetaData){
  const { updateAudioStateInConference, sendRefreshEvent } = require("../controllers/Conference/conferenceCall")
  if(this.confIdToAudioControls.hasOwnProperty(confId)){
    this.stopAudioStreamInConference(confId)
    global.confIdToMusicState[confId] = 0
    await updateAudioStateInConference(confId,"pause")
    // await sendRefreshEvent(confId)
  }
}

async resumeAudioStreamInConference(confId,audioMetaData){
  if(this.confIdToAudioControls.hasOwnProperty(confId)){
    const { blobExists } = require("../audioManipulation")
    const { updateAudioIdInConference, updateAudioStateInConference, sendRefreshEvent } = require("../controllers/Conference/conferenceCall")
    const { audioId } = audioMetaData
    if(await blobExists(`${audioId}/1.0.wav`)){
      this.stopAudioStreamInConference(confId)
      var flag = false
      var isIdLastRecentOne = true
      if (!this.confIdToAudioControls[confId].audioIdToState.hasOwnProperty(audioId)) {
        await this.setAudioConfigInTheConference(confId,audioId)
        flag = true
      }
      if(flag || this.confIdToAudioControls[confId].currentAudioId !== audioId){
        isIdLastRecentOne = false
        const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
        const currentSpeechRate = global.speechRates[currentSpeechRateIndex]
        const blobName = `${audioId}/${currentSpeechRate}.wav`
        const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
        const contentLength = this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"]
        await this.loadChunkAndMapToFrames(confId,blobName,currentChunkNumber,contentLength)
      }

      if(!isIdLastRecentOne){
        await updateAudioIdInConference(confId,audioId)
        this.confIdToAudioControls[confId].currentAudioId = audioId
      }

      this.streamAudioInConference(confId,audioId,18);

      global.confIdToMusicState[confId] = 1
      await updateAudioStateInConference(confId,"play")
      // await sendRefreshEvent(confId)
    }
  }
}

async forwardAudioStreamInConference(confId,audioMetaData){
  const secondsToForward = 10
  const numberOfFramesToMove = secondsToForward * this.framesPerSecond
  const audioId = this.confIdToAudioControls[confId].currentAudioId
  const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
  const currentFrameNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"]
  if(this.audioData[confId].hasOwnProperty(currentChunkNumber)){
    this.stopAudioStreamInConference(confId)
    const numberOfFramesLeftInCurrentChunk = this.numberOfFramesPerChunk - currentFrameNumber
    if(numberOfFramesLeftInCurrentChunk >= numberOfFramesToMove){
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = currentFrameNumber + numberOfFramesToMove - 1
    }
    else{
      const numberOfFramesRequiredYet = numberOfFramesToMove - numberOfFramesLeftInCurrentChunk
      this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] += 1
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = numberOfFramesRequiredYet - 1
      const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
      const currentSpeechRate = global.speechRates[currentSpeechRateIndex]
      const blobName = `${audioId}/${currentSpeechRate}.wav`
      const contentLength = this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"]
      await this.loadChunkAndMapToFrames(confId,blobName,currentChunkNumber+1,contentLength)
    }
    this.streamAudioInConference(confId,audioId,18);
  }
}

async rewindAudioStreamInConference(confId,audioMetaData){
  const secondsToBackward = 10
  const numberOfFramesToMove = secondsToBackward * this.framesPerSecond
  const audioId = this.confIdToAudioControls[confId].currentAudioId
  const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
  const currentFrameNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"]
  if(this.audioData[confId].hasOwnProperty(currentChunkNumber)){
    this.stopAudioStreamInConference(confId)
    const numberOfFramesPlayedInCurrentChunk = currentFrameNumber
    if(numberOfFramesPlayedInCurrentChunk >= numberOfFramesToMove){
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = currentFrameNumber - numberOfFramesToMove
    }
    else{
      const numberOfFramesRequiredYet = numberOfFramesToMove - numberOfFramesPlayedInCurrentChunk
      const requiredFramePos = this.numberOfFramesPerChunk - numberOfFramesRequiredYet
      this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] -= 1
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = requiredFramePos
      const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
      const currentSpeechRate = global.speechRates[currentSpeechRateIndex]
      const blobName = `${audioId}/${currentSpeechRate}.wav`
      const contentLength = this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"]
      await this.loadChunkAndMapToFrames(confId,blobName,currentChunkNumber-1,contentLength)
    }
    this.streamAudioInConference(confId,audioId,18);
  }
}

getChunkNumberFromFrameNumber(frameNumber){
  const currentChunkNumber = Math.ceil(frameNumber / this.numberOfFramesPerChunk)
  // const globalFrameNumber = (currentChunkNumber - 1) * numberOfFramesPerChunk + something
  return currentChunkNumber
}

// To get a frame Number in a Chunk from a frameNumber in entire Audio
getLocalFrameNumberFromGlobalFrameNumber(globalFrameNumber){
  const remainder = globalFrameNumber % this.numberOfFramesPerChunk
  return remainder
}

async increasePlaybackRateOfAudioStreamInConference(confId,audioMetaData){
  const { getLengthOfBlob } = require("../audioManipulation")
  const audioId = this.confIdToAudioControls[confId].currentAudioId
  const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
  if(this.audioData[confId].hasOwnProperty(currentChunkNumber)){
    const currentFrameNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"]
    const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
    if(currentSpeechRateIndex != global.speechRates.length-1){
      this.stopAudioStreamInConference(confId)
      const nextSpeechRateIndex = currentSpeechRateIndex + 1
      const currentSpeechRate = Number(global.speechRates[currentSpeechRateIndex])
      const nextSpeechRate = Number(global.speechRates[nextSpeechRateIndex])

      const globalFramePos = (currentChunkNumber - 1) * this.numberOfFramesPerChunk + currentFrameNumber
      const nextSpeechRateGlobalFramePos = Math.floor((globalFramePos + 1) * (currentSpeechRate / nextSpeechRate))
      const nextSpeechRateLocalFramePos = this.getLocalFrameNumberFromGlobalFrameNumber(nextSpeechRateGlobalFramePos)
      const nextSpeechRateChunkNumber = this.getChunkNumberFromFrameNumber(nextSpeechRateGlobalFramePos)

      const blobName = `${audioId}/${global.speechRates[nextSpeechRateIndex]}.wav`
      const contentLength = await getLengthOfBlob(blobName)

      this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"] = nextSpeechRateIndex
      this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] = nextSpeechRateChunkNumber
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = nextSpeechRateLocalFramePos
      this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"] = contentLength

      await this.loadChunkAndMapToFrames(confId,blobName,nextSpeechRateChunkNumber,contentLength)

      this.streamAudioInConference(confId,audioId,18);
    }
  }
}
async decreasePlaybackRateOfAudioStreamInConference(confId,audioMetaData){
  const { getLengthOfBlob } = require("../audioManipulation")
  const audioId = this.confIdToAudioControls[confId].currentAudioId
  const currentChunkNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"]
  if(this.audioData[confId].hasOwnProperty(currentChunkNumber)){
    const currentFrameNumber = this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"]
    const currentSpeechRateIndex = this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"]
    if(currentSpeechRateIndex != 0){
      this.stopAudioStreamInConference(confId)
      const nextSpeechRateIndex = currentSpeechRateIndex - 1
      const currentSpeechRate = Number(global.speechRates[currentSpeechRateIndex])
      const nextSpeechRate = Number(global.speechRates[nextSpeechRateIndex])

      const globalFramePos = (currentChunkNumber - 1) * this.numberOfFramesPerChunk + currentFrameNumber
      const nextSpeechRateGlobalFramePos = Math.floor((globalFramePos + 1) * (currentSpeechRate / nextSpeechRate))
      const nextSpeechRateLocalFramePos = this.getLocalFrameNumberFromGlobalFrameNumber(nextSpeechRateGlobalFramePos)
      const nextSpeechRateChunkNumber = this.getChunkNumberFromFrameNumber(nextSpeechRateGlobalFramePos)

      const blobName = `${audioId}/${global.speechRates[nextSpeechRateIndex]}.wav`
      const contentLength = await getLengthOfBlob(blobName)

      this.confIdToAudioControls[confId].audioIdToState[audioId]["speechRateIndex"] = nextSpeechRateIndex
      this.confIdToAudioControls[confId].audioIdToState[audioId]["chunkNumber"] = nextSpeechRateChunkNumber
      this.confIdToAudioControls[confId].audioIdToState[audioId]["frameNumber"] = nextSpeechRateLocalFramePos
      this.confIdToAudioControls[confId].audioIdToState[audioId]["contentLength"] = contentLength

      await this.loadChunkAndMapToFrames(confId,blobName,nextSpeechRateChunkNumber,contentLength)

      this.streamAudioInConference(confId,audioId,18);
    }
  }
}

async muteUserInConference(confId,userPhoneNumber,by){
  const { 
    getConferenceObjectById, 
    updateMutePropertyOfUserInConference, 
    sendRefreshEvent, 
    getMutedOrUnMutedMessage 
  } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;
  const participant = participants.find(
    (participant) => participant.phoneNumber === userPhoneNumber
  );
  if(participant.isMuted){
    return;
  }
  const uuid = participant.uuid;
  await this.updateCallLeg(uuid, "mute")
  await updateMutePropertyOfUserInConference(confId,userPhoneNumber,true)
  // await sendRefreshEvent(confId)
  const message = getMutedOrUnMutedMessage(confId,userPhoneNumber,true,by)
  this.playTTSMessage(uuid,message)
}

async unMuteUserInConference(confId,userPhoneNumber,by){
  const { 
    getConferenceObjectById, 
    updateMutePropertyOfUserInConference, 
    sendRefreshEvent, 
    getMutedOrUnMutedMessage 
  } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;
  const participant = participants.find(
    (participant) => participant.phoneNumber === userPhoneNumber
  );
  if(!participant.isMuted){
    return;
  }
  const uuid = participant.uuid;
  await this.updateCallLeg(uuid, "unmute")
  await updateMutePropertyOfUserInConference(confId,userPhoneNumber,false)
  // await sendRefreshEvent(confId)
  const message = getMutedOrUnMutedMessage(confId,userPhoneNumber,false,by)
  this.playTTSMessage(uuid,message)
}

// It will be invoked when the teacher wants to add a participant in the middle of an ongoing conference
addParticipantToConference(confId,userPhoneNumber){
  this.queue.enqueue({
    callType: "addToConference",
    phoneNumber: userPhoneNumber,
    confId:confId
  });
}

async removeParticipantFromConference(confId,userPhoneNumber){
  const { 
    getConferenceObjectById, 
    saveErrorAndSendToAndroidClient,
    createMessageForUserBeforeGettingDisconnectedByTeacher
  } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  const participants = conference.participants;
  const participant = participants.find(
    (participant) => participant.phoneNumber === userPhoneNumber
  );
  const uuid = participant.uuid;
  const message = createMessageForUserBeforeGettingDisconnectedByTeacher()
  this.playTTSMessage(uuid,message)
  setTimeout(() => {
    this.updateCallLeg(uuid, "hangup")
      .then(() => {
      })
      .catch((err) => {
        saveErrorAndSendToAndroidClient(confId,err).then(console.log).catch(console.log)
      });
  },Math.floor(message.length/10*1000))
}

// It handles all the DTMF inputs from user.
// vonage sends all the key presses by users during a conversation(can be conference or pullModel as each call itself is a conversation) to this function
// currently using it for Conference call only because for pull model we have a separate way of collecting DTMF inputs from user (using request-response cycle)
// It will be helpful to know what key pressed and by whom also.
async handleDTMFFromConversation(req){
  const { 
    checkRaiseHand,
    getLeaderPhoneNumberFromConference,
    doesThisConferenceHaveMusicState,
    handleConferenceCallControls,
    getMusicStateFromConference,
    saveErrorAndSendToAndroidClient,
  } = require("../controllers/Conference/conferenceCall");
  // const conversation_uuid = req.body.conversation_id
  const innerBody = req.body.body
  const uuid = innerBody.channel.id

  // console.log("Digit received.")
  // console.log(innerBody.digit)
  
  var confId = undefined
  if(this.UUIDToConfId.hasOwnProperty(uuid)){
    try{
      confId = this.UUIDToConfId[uuid];
      const userPhoneNumber = this.UUIDToPhoneNumber[uuid]
      if(innerBody.digit === "0"){
        await checkRaiseHand(confId,userPhoneNumber)
        return;
      }
      if(global.confIdToLeaderPhoneNumber.hasOwnProperty(confId)){
        const leaderPhoneNumber = getLeaderPhoneNumberFromConference(confId)
        if(leaderPhoneNumber === userPhoneNumber){
          // console.log(`${uuid} pressed the digit=${innerBody.digit} in the Conference ${confId}`)
          const digit = innerBody.digit
          var action = ""
          if(global.audioControlDigits.includes(digit)){
            if(!doesThisConferenceHaveMusicState(confId)){
              return;
            }
          }
          if(digit === "2"){
            action = global.digitToAction[digit][1-getMusicStateFromConference(confId)]
          }
          else if(global.digitToAction.hasOwnProperty(digit)){
            action = global.digitToAction[digit]
            console.log(`Action = ${action}`)
          }
          if(global.nonAudioControls.has(action)){
            await handleConferenceCallControls(confId,action,{},'Leader')
          }
          else if(global.audioControls.has(action)){
            if(this.confIdToAudioControls.hasOwnProperty(confId)){
              const audioId = this.confIdToAudioControls[confId].currentAudioId
              if(audioId !== null){
                await handleConferenceCallControls(confId,action,{audioId:audioId})
              }
            }
          }
        }
      }
    }
    catch(error){
      if(confId){
        saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
      }
    }
  }
  else if(this.UUIDToGameID.hasOwnProperty(uuid)){
    try{
      this.handleDTMFfromHandCricket(uuid,innerBody.digit)
    }
    catch(error){
      console.log("error occuring here...")
      console.log(error.message)
    }
  }
}

// It makes call to a user to connect to ongoing conference
// Note: this request was enqueued by *addParticipantToConference* Method above
async makeCallToUserInConference(confId,phoneNumber){
  const { 
    getConferenceObjectById, 
    saveErrorAndSendToAndroidClient,
    getTeacherNumberFromConference,
    messageToBePlayedForStudentsOnPickingTheCallInConference,
    mapPhoneNumbersToConfId,
    deletePhoneNumberToConferenceMapping
  } = require("../controllers/Conference/conferenceCall");
  const conference = await getConferenceObjectById(confId);
  if (!conference.isEnded) {
    var ncco = [];
    const teacherNumber = getTeacherNumberFromConference(confId)
    if (teacherNumber === phoneNumber) {
      if (!this.confIdToVonageWebsocketUUID.hasOwnProperty(confId)) {
        const socketEndpoint = "vonage/websocket_conference"
        const eventEndpoint = "vonage/conference_websocket_events"
        const headers = {
          confId: confId,
        }
        ncco.push(this.createWebSocketNCCO(headers,socketEndpoint,eventEndpoint));
      }
    } else {
      ncco.push(
        {
          action:"talk",
          text:messageToBePlayedForStudentsOnPickingTheCallInConference(confId),
          language:"en-IN",
          style:4
        });
    }
    ncco.push({
      action: "conversation",
      name: confId,
      // record:true,
      eventMethod:"POST",
      // eventUrl:[global.remoteUrl + "vonage/conference_call/recording"]
    });
    
    const eventUrl = [global.remoteUrl + "vonage/conference_call/events"]

    const call_back_func = async (err, resp) => {
      try{
        if(err){
          deletePhoneNumberToConferenceMapping(phoneNumber)
          throw {
            message:`Vonage is not able to make call to this number ${phoneNumber}`
          }
        }
        const conference = await getConferenceObjectById(confId)
        if (conference.isEnded) {
          await this.updateCallLeg(resp.uuid, "hangup")
        }
      }
      catch(error){
        saveErrorAndSendToAndroidClient(confId,error)
        .then(console.log)
        .catch(console.log)
      }
      this.isSlotFreeToMakeCalls = true;
      this.startCheckingSlot(0)
    }
    mapPhoneNumbersToConfId([phoneNumber],confId)
    this.makeVonageCall(phoneNumber,eventUrl,call_back_func,ncco)
  }
  else{
    deletePhoneNumberToConferenceMapping(phoneNumber)
  }
}

// It handles the websocket connection request came from vonage client
handleWebSocketConnection(pathname,request,socket,head){
  if (pathname === "/vonage/websocket_conference") {
    const vonageConferenceWebSocket = this.createWebSocketForConference()
    vonageConferenceWebSocket.handleUpgrade(request, socket, head, function done(ws) {
      vonageConferenceWebSocket.emit("connection", ws, request);
    });
  }
  else if(pathname === "/vonage/websocket_monocall"){
    const vonageMonocallWebSocket = this.createWebSocketForMonoCall()
    vonageMonocallWebSocket.handleUpgrade(request, socket, head, function done(ws) {
      vonageMonocallWebSocket.emit("connection", ws, request);
    });
  }
}

createWebSocketForConference() {
  const WebSocket = require("ws");
  const wss = new WebSocket.Server({ noServer: true });
  wss.setMaxListeners(20); // Set maxListeners for WebSocket.Server instance

  const { logClientRequest, saveErrorAndSendToAndroidClient, sendMessageToAndroidUser } = require("../controllers/Conference/conferenceCall");

  wss.on("connection", (ws, req) => {
    console.log("New Client Connected.");
    ws.setMaxListeners(20); // Set maxListeners for Socket object
    ws.send("Welcome to New Client");

    ws.on("message", async (message) => {
      try {
        message = JSON.parse(message);
        if (message.hasOwnProperty("event")) {
          console.log(message)
          if (message["event"] === "websocket:connected") {
            var confId = undefined
            try{
              // console.log("confId ", message["confId"]);
              confId = message["confId"]
              const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
              await logClientRequest(confId,indianTime,"/vonage/conference","VonageWebSocket: Message","WebSocket","VonageWebSocket:Connected")
              this.confIdToVonageWebsocket[confId] = ws;
              this.confIdToAudioControls[confId] = {
                setTimeOutId:null,
                audioIdToState:{
                },
                currentAudioId:null
              };
              await sendMessageToAndroidUser(confId,"vonageWebsocket:connected")
            }
            catch(error){
              if(confId){
                saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
              }
            }
          }
        }
      } catch (err) {
        
      }
    });
    ws.on("close", (code, reason) => {
      console.log("Connection closed.");
      console.log(code);
      console.log(reason);
    });
  });
  return wss;
}

// As vonage treats each websocket as a participant, it sends all the events of the websocket, like started,ringing,answered etc.
async handleVonageClientWebSocketEventsForConference(req,res){
  const { logClientRequest, saveErrorAndSendToAndroidClient, sendMessageToAndroidUser } = require("../controllers/Conference/conferenceCall");
  var confId = undefined
  try{
    const body = req.body;
    console.log(body);
    if (body.hasOwnProperty("status")) {
      confId = body.headers.confId;
      const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
      await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(body))
      if (body.status === "answered") {
        this.confIdToVonageWebsocketUUID[confId] = body.uuid;
        this.UUIDToConfId[body.uuid] = confId
      } else if (body.status === "completed") {
        await sendMessageToAndroidUser(confId,"vonageWebsocket:completed")
        if (this.confIdToVonageWebsocketUUID.hasOwnProperty(confId)) {
          delete this.confIdToVonageWebsocketUUID[confId];
        }
        delete this.UUIDToConfId[body.uuid]
      } else if (["disconnected", "failed", "unanswered"].includes(body.status)) {
        const ncco = [
          {
            action: "connect",
            from: "NexmoTest",
            eventType: "synchronous",
            eventUrl: [global.remoteUrl + "vonage/conference_websocket_events"],
            endpoint: [
              {
                type: "websocket",
                uri: `${process.env.SEEDS_IVR_SERVER_WEBSOCKET_BASE_URL}vonage/websocket_conference`,
                "content-type": "audio/l16;rate=8000",
                headers: {
                  confId: confId,
                },
              },
            ],
          },
          {
            action: "conversation",
            name: confId,
          },
        ];
        if (body.status === "disconnected") {
          // delete confIdToVonageWebsocketUUID[confId]
          await sendMessageToAndroidUser(confId,"vonageWebsocket:disconnected")
        } else if (body.status === "failed" || body.status === "unanswered") {
          await sendMessageToAndroidUser(confId,"vonageWebsocket:failed")
        }
        return res.json(ncco);
      }
    }
    if (body.hasOwnProperty("type")) {
      if (body.type === "transfer") {
        confId = this.UUIDToConfId[body.uuid]
        const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
        await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(body))
        await sendMessageToAndroidUser(confId,"vonageWebsocket:joined")
      }
    }
  }
  catch(error){
    if(confId){
      saveErrorAndSendToAndroidClient(confId,error).then(console.log).catch(console.log)
    }
  }
  res.send("got it.")
}

createHandCricketGame(gameId,phoneNumbers){
  this.queue.enqueue({
    callType: "handCricket",
    numbers: phoneNumbers,
    gameId:gameId
  });
}

makeHandCricketCall(gameId,numbers) {
  const { messageToTheUserBeforeTheyJoinTheGame } = require("../controllers/handCricket")

  this.handCricketData[gameId] = { "waitingForInput":false }

  const ncco = [
    {
      action:"talk",
      text:messageToTheUserBeforeTheyJoinTheGame(),
      language:"en-IN",
      style:4
    },
    {
      action: "conversation",
      name: gameId,
      // record:true,
      eventMethod:"POST",
      // eventUrl:[global.remoteUrl + "conference_call/recording"]
    },
  ];
  
  var ct = 0

  const call_back_func = (err, resp) => {
    if (err) console.error(err);
    if (resp) { console.log(resp)}
    ct += 1
    if(ct === numbers.length){
      this.isSlotFreeToMakeCalls = true;
      this.startCheckingSlot(0)
    }
  }

  const eventUrl = [global.remoteUrl + "vonage/handCricket/events"]
  
  for(let i=0;i<2;i++){
    console.log(`number = ${numbers[i]}`)
    this.makeVonageCall(numbers[i],eventUrl,call_back_func,ncco)
  }
}

sendGroupMessageInHandCricketGame(gameId,message){
  const { getAllThePhoneNumbersInTheGame } = require("../controllers/handCricket")
  const phoneNumbers = getAllThePhoneNumbersInTheGame(gameId)
  for(const phoneNumber of phoneNumbers){
    const uuid = this.phoneNumberToUUID[phoneNumber]
    this.playTTSMessage(uuid,message)
  }
}

handleHandCricketEvents(req,res){
  const { 
    getGameIdFromPhoneNumber,
    isHandCricketGameActive,
    changeCallStatusOfUserInHandCricket,
    setRoomStatusOfUserInHandCricket,
    checkOtherUserinRoom,
    deleteGameIdFromPhoneNumber,
    initialMessageWhenBotheUsersJoinedTheGame,
    messageWhenOneUserJoinedButNotOtherYet,

  } = require("../controllers/handCricket")
  const body = req.body
  const uuid = body.uuid
  
  if(body.hasOwnProperty("status")){
    const userPhoneNumber = body.to
    const gameId = getGameIdFromPhoneNumber(userPhoneNumber)
    if(!this.UUIDToPhoneNumber.hasOwnProperty(uuid)){
      this.UUIDToPhoneNumber[uuid] = userPhoneNumber
      this.phoneNumberToUUID[userPhoneNumber] = uuid
      this.UUIDToGameID[uuid] = gameId
    }
    
    const status = body.status
    if(status === "completed"){
      delete this.UUIDToPhoneNumber[uuid]
      delete this.phoneNumberToUUID[userPhoneNumber]
      delete this.UUIDToGameID[uuid]
      deleteGameIdFromPhoneNumber(userPhoneNumber)

      if(isHandCricketGameActive(gameId)){
        this.finishTheHandCricketGame(gameId)
      }
      
      return res.send("got it.")
    }
    if(isHandCricketGameActive(gameId)){
      changeCallStatusOfUserInHandCricket(gameId,userPhoneNumber,this.callStatusMappings[status])
    }
  }
  if (body.hasOwnProperty("type")) {
    if (body.type === "transfer") {
      const userPhoneNumber = this.UUIDToPhoneNumber[uuid]
      const gameId = getGameIdFromPhoneNumber(userPhoneNumber)
      if(!isHandCricketGameActive(gameId)){
        return res.send("game already ended. you joined late or your friend cut the call.")
      }
      
      setRoomStatusOfUserInHandCricket(gameId,userPhoneNumber,true)

      if(checkOtherUserinRoom(gameId,userPhoneNumber)){
        const message = initialMessageWhenBotheUsersJoinedTheGame()
        const time = Math.floor((message.length / 10) * 1000)
        this.sendGroupMessageInHandCricketGame(gameId,message)
        setTimeout(() => {
          this.handCricketData[gameId]["waitingForInput"] = true
        },time)
      }
      else{
        const message = messageWhenOneUserJoinedButNotOtherYet()
        this.playTTSMessage(uuid,message)
      }
    }
  }
  return res.send("got it.")
}

handleDTMFfromHandCricket(uuid,digit){
  const { 
    isHandCricketGameActive,
    handleInputFromUser,

  } = require("../controllers/handCricket")
  const gameId = this.UUIDToGameID[uuid]
  if(!isHandCricketGameActive(gameId)){
    return;
  }
  if(["0","7","8","9","#","*"].includes(digit) || this.handCricketData[gameId]["waitingForInput"] === false){
    return;
  }

  const phoneNumber = this.UUIDToPhoneNumber[uuid]

  handleInputFromUser(gameId,phoneNumber,digit)
}

startHandCricketGame(gameId,params){
  const {
    batsmanPhoneNumber,
    bowlerPhoneNumber,
    batsmanMessage,
    bowlerMessage 
  } = params

  this.handCricketData[gameId]["waitingForInput"] = false

  const batsmanUUID = this.phoneNumberToUUID[batsmanPhoneNumber]
  const bowlerUUID = this.phoneNumberToUUID[bowlerPhoneNumber]

  this.playTTSMessage(batsmanUUID,batsmanMessage)
  this.playTTSMessage(bowlerUUID,bowlerMessage)

  setTimeout(() => {
    this.handCricketData[gameId]["waitingForInput"] = true
  },6000)

}

playMessageAndContinueTheHandCricketGame(gameId,params){
  const {
    batsmanPhoneNumber,
    bowlerPhoneNumber,
    batsmanMessage,
    bowlerMessage
  } = params

  this.handCricketData[gameId]["waitingForInput"] = false
  
  const batsmanUUID = this.phoneNumberToUUID[batsmanPhoneNumber]
  const bowlerUUID = this.phoneNumberToUUID[bowlerPhoneNumber]

  this.playTTSMessage(batsmanUUID,batsmanMessage)
  this.playTTSMessage(bowlerUUID,bowlerMessage)

  setTimeout(() => {
    this.handCricketData[gameId]["waitingForInput"] = true
  },12000)

}

playMessageAndFinishTheHandCricketGame(gameId,params){
  const {
    winnerPhoneNumber,
    loserPhoneNumber,
    winnerMessage,
    loserMessage
  } = params

  this.handCricketData[gameId]["waitingForInput"] = false
  
  const time = Math.floor((Math.max(winnerMessage.length,loserMessage.length) / 10) * 1000)

  const winnerUUID = this.phoneNumberToUUID[winnerPhoneNumber]
  const loserUUID = this.phoneNumberToUUID[loserPhoneNumber]

  this.playTTSMessage(winnerUUID,winnerMessage)
  this.playTTSMessage(loserUUID,loserMessage)
  
  setTimeout(() => {
    this.finishTheHandCricketGame(gameId)
  },time)
}

playMessageToUserInHandCricketGame(gameId,userPhoneNumber,message){
  // Not currently i am not using gameId here but it might be helpful if we move to other communication API in future.
  const uuid = this.phoneNumberToUUID[userPhoneNumber]
  this.playTTSMessage(uuid,message)
}

finishTheHandCricketGame(gameId){
  const { getAllThePhoneNumbersInTheGame,deleteHandCricketGameObject } = require("../controllers/handCricket")
  const userPhoneNumbers = getAllThePhoneNumbersInTheGame(gameId)
  for(const userPhoneNumber of userPhoneNumbers){
    const uuid = this.phoneNumberToUUID[userPhoneNumber]
    this.updateCallLeg(uuid, "hangup")
    .then(console.log)
    .catch(console.log)
  }
  deleteHandCricketGameObject(gameId)
  delete this.handCricketData[gameId]
}

// It enqueues the Pull Call request
createMonoCall(userPhoneNumber){
  this.queue.enqueue({
    callType: "mono",
    phoneNumber: userPhoneNumber,
  });
}

playTTSMessageToUserInMonoCall(userPhoneNumber,message,ttsLang){
  const uuid = this.phoneNumberToUUID[userPhoneNumber]
  this.playTTSMessage(uuid,message,ttsLang)
}

playAudioMessageToUserInMonoCall(userPhoneNumber,streamUrl){
  const uuid = this.phoneNumberToUUID[userPhoneNumber]
  this.playAudioMessage(uuid,streamUrl)
}

getEndPointForLanguageInput(){
  return `${global.remoteUrl}vonage/mono_call/lang_input`
}

getEndPointForExperienceTypeInput(){
  return `${global.remoteUrl}vonage/mono_call/experience_type_input`
}

getEndPointForAudioExperienceMainMenu(experienceName){
  return `${global.remoteUrl}vonage/mono_call/${experienceName}/main_menu`
}

getEndPointForAudioContentListInput(audioExperienceName){
  return `${global.remoteUrl}vonage/mono_call/${audioExperienceName}/content_list`
}

getEndPointForAudioContentPlayingInput(experienceName){
  return `${global.remoteUrl}vonage/mono_call/${experienceName}/content_playing`
}

getEndPointForKeyLearningUserInputInMonoCall(){
  return `${global.remoteUrl}vonage/mono_call/keyLearning/user_input`
}

getEndPointForScrambleMainMenu(){
  return `${global.remoteUrl}vonage/mono_call/scramble/main_menu`
}

getEndPointForScrambleExploringPoemLineOrdering(){
  return `${global.remoteUrl}vonage/mono_call/scramble/explore_poem_line_ordering`
}

getEndPointForScramblePoemLineSequenceInput(){
  return `${global.remoteUrl}vonage/mono_call/scramble/entered_poem_line_sequence`
}

getEndPointForScrambleEnterSequenceOrRepeatContentInput(){
  return `${global.remoteUrl}vonage/mono_call/scramble/enterSequenceAgain_or_repeatContent_input`
}

getEndPointForQuizMainMenuInMonoCall(){
  return `${global.remoteUrl}vonage/mono_call/quiz/main_menu`
}

getEndPointForQuizMultipleChoiceSelectionInput(){
  return `${global.remoteUrl}vonage/mono_call/quiz/multiple_choice_selection`
}

getEndPointForRiddleContentPlayingInput(experienceName){
  return `${global.remoteUrl}vonage/mono_call/${experienceName}/content_playing`
}

async makeMonoCall(number) {
  const { createMonoCallObjectForUser } = require("../controllers/monoCall/utils");
  const { prepareContentForLanguageMenu  }  = require("../controllers/monoCall/monoCall")

  createMonoCallObjectForUser(number)
  const actions = prepareContentForLanguageMenu(number)
  /* 
    Note: As we are following NCCO as the general call flow structure. so , we don't need to do any
    pre-processing to create call control object (cco). But if we move to Other Communication APIs, we will
    have to pre-process the actions object to create cco for that API.
    Every communication API has it's own CCO
    NCCO means Nexmo Call control object.
  */
  const ncco = actions
  
  const call_back_func = async (err, resp) => {
    if (err) {
      delete global.monoCallInfo[number]
      console.error(err)
    };
    if (resp) console.log(resp)
    this.isSlotFreeToMakeCalls = true;
    this.startCheckingSlot(0)
  }

  const eventUrl = [global.remoteUrl + "vonage/mono_call/events"]
  this.makeVonageCall(number,eventUrl,call_back_func,ncco)
}

// It handles all the Pull Call Events from Vonage
async handleMonoCallEvents(req){
  const { setTimerToSaveUserTimeInDB } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")
  console.log(req.body)
  const body = req.body
  const uuid = body.uuid
  
  var userPhoneNumber = undefined

  if(body.type === "transfer"){
    userPhoneNumber = this.UUIDToPhoneNumber[uuid]
    const conv_id_from = body.conversation_uuid_from
    const conv_id_to = body.conversation_uuid_to
    delete this.conversationUUIDToPhoneNumber[conv_id_from]
    this.conversationUUIDToPhoneNumber[conv_id_to] = userPhoneNumber
  }
  else if(["started","ringing","answered"].includes(body.status)){
    if(!this.UUIDToPhoneNumber.hasOwnProperty(uuid)){
      userPhoneNumber = body.to
      this.UUIDToPhoneNumber[uuid] = userPhoneNumber
      this.phoneNumberToUUID[userPhoneNumber] = uuid
      this.conversationUUIDToPhoneNumber[body.conversation_uuid] = userPhoneNumber
    }
    if(body.status === "answered"){
      userPhoneNumber = body.to
      // This if condition is to satisfy the security constraints of github's code security scanning.
      if(userPhoneNumber === '__proto__' || userPhoneNumber === 'constructor' || userPhoneNumber === 'prototype') {
        throw "Prototype-polluting assignment (Prototype pollution Attack)"
      }
      // start the Time for Inactive time
      setTimerForInactivityOfUserInPullModel(userPhoneNumber)


      // fetch document with this phoneNumber.
      // if it doc doesn't exist, create doc with milliSeconds to 0
      // if exists, check if date is today?
      // if yes, take that previousMilliSeconds.
            // if previousMilliSeconds exceeds 45 seconds cut the call with a message.
            // if it don't exceed, resume from that time.
      // if no, change date to current date and set milliSeconds to 0

      // after above steps, every 5 mins update db with current calculated milliSeconds.
      // but before updating everytime, we will check if the time exceeds, if yes update db and cut the call with message.

      const user = await this.userSpentTimeForPullModel.findOne({phoneNumber:userPhoneNumber}).exec();
      if(user){
        console.log(user.date)
        var currentDate = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'})
        currentDate = currentDate.split(",")[0]
        if(currentDate === user.date){
          if(user.timeInMilliSeconds >= global.maxTimeLimitForUserInMilliseconds){
            // play message and cut the call
            this.playTimeOverMessageAndCutTheCall(userPhoneNumber)
          }
          else{
            global.monoCallInfo[userPhoneNumber]['timeSpentInMilliSeconds'] = user.timeInMilliSeconds
            setTimerToSaveUserTimeInDB(userPhoneNumber)
          }
        }
        else{
          console.log("date not matching...")
          await this.userSpentTimeForPullModel.findOne({phoneNumber:userPhoneNumber}).update({timeInMilliSeconds:0,date:currentDate})
          global.monoCallInfo[userPhoneNumber]['timeSpentInMilliSeconds'] = 0
          setTimerToSaveUserTimeInDB(userPhoneNumber)
        }
      }
      else{
        var currentDate = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'})
        currentDate = currentDate.split(",")[0]

        await this.userSpentTimeForPullModel.create({
          phoneNumber: userPhoneNumber,
          timeInMilliSeconds: 0,
          date: currentDate
        })
        global.monoCallInfo[userPhoneNumber]['timeSpentInMilliSeconds'] = 0
        setTimerToSaveUserTimeInDB(userPhoneNumber)
      }
    }
  }
  else if(["completed","failed"].includes(body.status)){
    const { 
      handleCallEndedEvent
    } = require("../controllers/monoCall/utils")

    userPhoneNumber = body.to
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
    const conversationUUID = body.conversation_uuid
    if(this.monoCallData.hasOwnProperty(userPhoneNumber)){
      clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
      const vonageWebSocketUUID = this.monoCallData[userPhoneNumber]["vonageWebSocketUUID"]
      if(vonageWebSocketUUID){
        this.vonage.calls.update(vonageWebSocketUUID, { action: "hangup" }, (err, res) => {
          if (err) {
            console.log(`Could not hangUp Vonage WebSocket with UUID ${vonageWebSocketUUID}`)
          } else {
            console.log(`Voange WebSocket with UUID ${vonageWebSocketUUID} HungUp Successfully.`)
          }
        });
      }
    }
    delete this.phoneNumberToUUID[userPhoneNumber]
    delete this.audioData[userPhoneNumber]
    delete this.conversationUUIDToPhoneNumber[conversationUUID]
    delete this.UUIDToPhoneNumber[uuid]
    delete this.monoCallData[userPhoneNumber]
    
    await handleCallEndedEvent(userPhoneNumber)
  }
}

// It takes the call event body and extracts only required params for further processing
getRequiredParamsFromVonageBodyForMonoCall(body){
  const params = {}
  const uuid = body.uuid
  if(uuid){
    params['userPhoneNumber'] = this.UUIDToPhoneNumber[body.uuid]
  }
  else{
    params['userPhoneNumber'] = this.conversationUUIDToPhoneNumber[body.conversation_uuid]
  }
  params['digits'] = body.dtmf.digits
  return params
}

getRequiredParamsFromCommunicationApiBody(req){
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  return params
}

transferCallWithNCCO(uuid,ncco) {
  this.vonage.calls.update(
    uuid,
    {
      action: "transfer",
      destination: {
        type: "ncco",
        ncco: ncco,
      },
    },
    (err, res) => {
      if (err) console.log(err);
      if (res) console.log(res);
    }
  );
}

async handleMonoCallLanguageInput(req,res){
  const { handleLanguageInput } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)

  const {userPhoneNumber, digits} = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleLanguageInput(params)

  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }

  const ncco = nextActions // here we will do some processing if we use some other communication api. explained before clearly take a look.
  return res.send(ncco)
}

async handleMonoCallExperienceTypeInput(req,res){
  const { handleExperienceTypeInput } = require("../controllers/monoCall/monoCall")
  const { getStreamAction } = require("../nccoActions")
  const { getCurrentExperience } = require("../controllers/monoCall/utils")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber,digits } = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleExperienceTypeInput(params)

  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }

  if(global.audioExperienceNames.includes(getCurrentExperience(userPhoneNumber))){
    // send empty Stream Url as a quick response to stay alive.
    res.send([
      getStreamAction(global.silenceStreamUrl,0,false)
    ])
    
    const uuid = req.body.uuid
    const ncco = [
      {
        action: "connect",
        from: process.env.VONAGE_NUMBER,
        eventType: "synchronous",
        eventUrl: [global.remoteUrl + "vonage/monocall_websocket_events"],
        endpoint: [
          {
            type: "websocket",
            uri: `${process.env.SEEDS_IVR_SERVER_WEBSOCKET_BASE_URL}vonage/websocket_monocall`,
            "content-type": "audio/l16;rate=8000",
            headers: {
              userPhoneNumber: userPhoneNumber,
            },
          },
        ],
      },
    ]
    
    for(const action of nextActions){
      ncco.push(action)
    }

    this.transferCallWithNCCO(uuid,ncco)  
  }
  else if(getCurrentExperience(userPhoneNumber) === "keyLearning"){
    const ncco = [
      ...nextActions[0],
      getStreamAction(global.silenceStreamUrl,0),
      nextActions[1]
    ]

    return res.send(ncco)
  }
  else{
    const ncco = nextActions
    return res.send(ncco)
  }
}

createWebSocketForMonoCall() {
  const WebSocket = require("ws");
  const wss = new WebSocket.Server({ noServer: true });

  wss.setMaxListeners(20); // Set maxListeners for WebSocket.Server instance

  wss.on("connection", (ws, req) => {
    console.log("New Client Connected.");
    ws.setMaxListeners(20); // Set maxListeners for Socket object

    ws.send("Welcome to New Client");
    
    ws.on("message", async (message) => {
      try {
        message = JSON.parse(message);
        if (message.hasOwnProperty("event")) {
          console.log(message);
          if (message["event"] === "websocket:connected") {
            const userPhoneNumber = message["userPhoneNumber"];
            // This if condition is to satisfy the security constraints of github's code security scanning.
            if(userPhoneNumber === '__proto__' || userPhoneNumber === 'constructor' || userPhoneNumber === 'prototype') {
              throw "Prototype-polluting assignment (Prototype pollution Attack)"
            }
            if(!this.monoCallData.hasOwnProperty(userPhoneNumber)){
              this.monoCallData[userPhoneNumber] = {}
            }
            this.monoCallData[userPhoneNumber]["vonageWebSocketObj"] = ws;
            this.monoCallData[userPhoneNumber]["playAudioTimeoutId"] = undefined;
          }
        }
      } catch (err) {}
      // ws.send(
      //   "To Vonage Voice API, I received the following from you, " + message
      // );
    });
    ws.on("close", (code, reason) => {
      console.log("Connection closed.");
      console.log(code);
      console.log(reason);
    });
  });
  return wss;
}

handleVonageClientWebSocketEventsForMonoCall(req,res){
  try{
    const body = req.body;
    console.log(body);
    if (body.hasOwnProperty("status")) {
      const userPhoneNumber = body.headers.userPhoneNumber;
      // This if condition is to satisfy the security constraints of github's code security scanning.
      if(userPhoneNumber === '__proto__' || userPhoneNumber === 'constructor' || userPhoneNumber === 'prototype') {
        throw "Prototype-polluting assignment (Prototype pollution Attack)"
      }
      if (body.status === "answered") {
        if(!this.monoCallData.hasOwnProperty(userPhoneNumber)){
          this.monoCallData[userPhoneNumber] = {}
        }
        this.monoCallData[userPhoneNumber]["vonageWebSocketUUID"] = body.uuid;
      } else if (body.status === "completed") {
        // to do.
      } else if (["disconnected", "failed", "unanswered"].includes(body.status)) {
        const ncco = [
          {
            action: "connect",
            from: process.env.VONAGE_NUMBER,
            eventType: "synchronous",
            eventUrl: [global.remoteUrl + "vonage/monocall_websocket_events"],
            endpoint: [
              {
                type: "websocket",
                uri: `${process.env.SEEDS_IVR_SERVER_WEBSOCKET_BASE_URL}vonage/websocket_monocall`,
                "content-type": "audio/l16;rate=8000",
                headers: {
                  userPhoneNumber: userPhoneNumber,
                },
              },
            ],
          },
        ];
        res.json(ncco);
      }
    }
  }
  catch(error){
    console.log(error.message)
  }
  res.end("ok");
}

// Currently we are not using it. Instead directly taking user to content List Menu (to Method named *handleContentListMenuOfAudioExperienceInMonoCall*)
async handleMainMenuOfAudioExperienceInMonoCall(req,res){
  const { handleMainMenuOfAudioExperience } = require("../controllers/monoCall/monoCall")
  const { getCurrentExperience } = require("../controllers/monoCall/utils")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber,digits } = params
  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleMainMenuOfAudioExperience(params)
  
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }

  if(getCurrentExperience(userPhoneNumber) === undefined){
    // hangup websocket before leaving this audio experience.
    await this.updateCallLeg(this.monoCallData[userPhoneNumber]["vonageWebSocketUUID"],"hangup")
  }

  const ncco = nextActions
  return res.send(ncco)
}

async setAudioConfigInMonoCall(userPhoneNumber,audioId,speechRateIndex){
  const { getLengthOfBlob } = require("../audioManipulation")

  const currentSpeechRate = global.speechRates[speechRateIndex]
  const path = `${audioId}/${currentSpeechRate}.wav`
  const contentLength = await getLengthOfBlob(path)
  if(!this.monoCallData.hasOwnProperty(userPhoneNumber)){
    this.monoCallData[userPhoneNumber] = {}
  }
  this.monoCallData[userPhoneNumber]['chunkNumber'] = 1
  this.monoCallData[userPhoneNumber]['frameNumber'] = 0
  this.monoCallData[userPhoneNumber]['contentLength'] = contentLength
}

// It will populate audio bytes in cache to stream it further
async initializeAudioContentToPlayInMonoCall(userPhoneNumber,audioId){
  const { getCurrentSpeechRateIndex } = require("../controllers/monoCall/utils")
  const currentSpeechRateIndex = getCurrentSpeechRateIndex(userPhoneNumber)
  await this.setAudioConfigInMonoCall(userPhoneNumber,audioId,currentSpeechRateIndex)
  const speechRate = global.speechRates[currentSpeechRateIndex]
  const blobName = `${audioId}/${speechRate}.wav`
  const chunkNumber = this.monoCallData[userPhoneNumber]['chunkNumber']
  const contentLength = this.monoCallData[userPhoneNumber]['contentLength']
  await this.loadChunkAndMapToFrames(userPhoneNumber,blobName,chunkNumber,contentLength)
}

// it will be triggered when the audio stream is over in pull call, to take further actions
async handleAudioStreamFinishedInMonoCall(userPhoneNumber){
  const { handleAudioStreamFinished } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")
  setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  const audioFinishedMessage = await handleAudioStreamFinished(userPhoneNumber)
  
  const uuid = this.phoneNumberToUUID[userPhoneNumber]
  // here we are repeating this *audio Finished* message unless they press 9 to exit from this section.
  this.playAudioMessage(uuid,audioFinishedMessage,0)
}

async streamAudioInMonoCall(userPhoneNumber,audioId,gap){
  try{
    // This if condition is to satisfy the security constraints of github's code security scanning.
    if(userPhoneNumber === '__proto__' || userPhoneNumber === 'constructor' || userPhoneNumber === 'prototype') {
      throw "Prototype-polluting assignment (Prototype pollution Attack)"
    }
    var currentChunkNumber = this.monoCallData[userPhoneNumber]['chunkNumber']
    if(this.audioData[userPhoneNumber].hasOwnProperty(currentChunkNumber)){
      var currentFrameNumber = this.monoCallData[userPhoneNumber]['frameNumber']
      const frame = this.audioData[userPhoneNumber][currentChunkNumber][currentFrameNumber]
      if(frame){
        const buffer = Uint8Array.from(frame).buffer
        this.monoCallData[userPhoneNumber]["vonageWebSocketObj"].send(
          buffer
        );
        if(currentFrameNumber === this.numberOfFramesPerChunk - 1){
          delete this.audioData[userPhoneNumber][currentChunkNumber]
          currentChunkNumber += 1
          this.monoCallData[userPhoneNumber]['chunkNumber'] += 1
          this.monoCallData[userPhoneNumber]['frameNumber'] = 0
          const contentLength = this.monoCallData[userPhoneNumber]['contentLength']
  
          const startBytePos = (currentChunkNumber - 1) * this.numberOfBytesPerChunk
  
          if(startBytePos < contentLength){
            const { getChunkAsFrames } = require("../audioManipulation")
            const { getCurrentSpeechRateIndex } = require("../controllers/monoCall/utils")

            const endBytePos = Math.min(currentChunkNumber * this.numberOfBytesPerChunk,contentLength)
            const numberOfBytesRequired = endBytePos - startBytePos
            const speechRateIndex = getCurrentSpeechRateIndex(userPhoneNumber)
            const speechRate = global.speechRates[speechRateIndex]
            const blobName = `${audioId}/${speechRate}.wav`
            const frames = await getChunkAsFrames(blobName,startBytePos,numberOfBytesRequired)
            this.audioData[userPhoneNumber][currentChunkNumber] = frames
          }
          else{
            console.log("audio is over while going nextChunk.")
            await this.handleAudioStreamFinishedInMonoCall(userPhoneNumber)
            return;
          }
        }
        else{
          this.monoCallData[userPhoneNumber]['frameNumber'] += 1
        }
        this.monoCallData[userPhoneNumber]["playAudioTimeoutId"] = setTimeout(() => {
          this.streamAudioInMonoCall(userPhoneNumber,audioId,20)
        }, gap);
      }
      else{
        // This is to make sure that when we check next time, this chunkNumber should result out of contentLength.
        this.monoCallData[userPhoneNumber]['chunkNumber'] += 1
        this.monoCallData[userPhoneNumber]['frameNumber'] = 0
        delete this.audioData[userPhoneNumber][currentChunkNumber]
        console.log("audio is over as frame is undefined in monocall.")
        await this.handleAudioStreamFinishedInMonoCall(userPhoneNumber)
      }
    }
    else{
      if(currentChunkNumber > 0){
        console.log('audio is over.')
        await this.handleAudioStreamFinishedInMonoCall(userPhoneNumber)
      }
      console.log(`invalid ChunkNumber. ${currentChunkNumber}`)
    }
  }
  catch(error){
    console.log(error.message)
  }
}

async handleContentListMenuOfAudioExperienceInMonoCall(req,res){
  const { handleContentListMenuOfAudioExperience } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")
  const { getCurrentExperience } = require("../controllers/monoCall/utils")
  
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)

  const { userPhoneNumber, digits } = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const response = await handleContentListMenuOfAudioExperience(params)

  if(Array.isArray(response) && response.length === 0){
    return res.send([])
  }

  const { audioSelected } = response
  if(audioSelected){
    const { setAudioStreamState } = require("../controllers/monoCall/utils")
    const { getStreamAction } = require("../nccoActions")

    const { userPhoneNumber } = params
    const { audioMetaData, nextActions } = response

    const { audioId } = audioMetaData // Here i am not using audio Title but might be helpful in future.
    await this.initializeAudioContentToPlayInMonoCall(userPhoneNumber,audioId)

    const func = () => {
      try{
        setAudioStreamState(userPhoneNumber,"play")
        this.streamAudioInMonoCall(userPhoneNumber,audioId,18)
      }
      catch(error){
        console.log(error);
      }
    }
    
    const announcementActions = nextActions[0]
    
    const milliSecondsToWaitBeforePlayingAudio = 8000
    
    setTimeout(func,milliSecondsToWaitBeforePlayingAudio)

    const ncco = [
      ...announcementActions,
      getStreamAction(global.silenceStreamUrl,0),
      nextActions[1]
    ]

    return res.send(ncco)
  }
  else{
    if(getCurrentExperience(userPhoneNumber) === undefined){
      // hangup websocket before leaving this audio experience.
      await this.updateCallLeg(this.monoCallData[userPhoneNumber]["vonageWebSocketUUID"],"hangup")
    }
    if(digits !== ""){
      setTimerForInactivityOfUserInPullModel(userPhoneNumber)
    }
    const { nextActions } = response
    const ncco = nextActions
    return res.send(ncco)
  }
}

async handleContentPlayingMenuOfAudioExperienceInMonoCall(req,res){
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber,digits } = params
  const { 
    getAudioStreamState,
    setAudioStreamState,
    storeLog,
    getCurrentLanguage,
    getCurrentEndPoint,
    setCurrentSpeechRateIndex,
    getCurrentSpeechRateIndex,
    getAudioStreamId,
    setTimerForInactivityOfUserInPullModel,
    stopTimerForInactivityOfUserInPullModel
  } = require("../controllers/monoCall/utils")
  
  const { getLengthOfBlob } = require("../audioManipulation")

  const { DTMFInputAction,getStreamAction } = require("../nccoActions")

  const { handleGoingBackToPreviousMenuFromAudioStreaming } = require("../controllers/monoCall/monoCall")

  const audioState = getAudioStreamState(userPhoneNumber)
  const uuid = this.phoneNumberToUUID[userPhoneNumber]

  const language = getCurrentLanguage(userPhoneNumber)
  const ttsLanguage = global.verbalLanguageToIVRTTSLanguageCode[language]
  const currentEndPoint = getCurrentEndPoint(userPhoneNumber)
  const currentSpeechRateIndex = getCurrentSpeechRateIndex(userPhoneNumber)
  const speechRate = global.speechRates[currentSpeechRateIndex]

  const audioId = getAudioStreamId(userPhoneNumber)

  // const experience = this.experienceName

  if(audioState !== "finished"){
    this.stopAudioMessage(uuid)
  }
  
  if(audioState !== "finished" && digits === "0"){
    clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
    setAudioStreamState(userPhoneNumber,"pause")
    await storeLog(userPhoneNumber,digits,"chosen audio controls help message")
    const audioControlsMessages = [
      global.pullMenuMainUrl + global.audioControlDialogUrl.replace('{language}',language).replace('{speechRate}',speechRate)
    ]
    setTimerForInactivityOfUserInPullModel(userPhoneNumber) // as we are pausing the audio, setting the timer.
    this.playAudioMessage(uuid,audioControlsMessages)
  }
  else if(digits === "8"){
    this.stopAudioMessage(uuid)
    clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
    await storeLog(userPhoneNumber,digits,"chosen repeat Audio from start")

    this.monoCallData[userPhoneNumber]['frameNumber'] = 0

    if(this.monoCallData[userPhoneNumber]['chunkNumber'] !== 1){
      const speechRate = global.speechRates[currentSpeechRateIndex]
      const blobName = `${audioId}/${speechRate}.wav`
      this.monoCallData[userPhoneNumber]['chunkNumber'] = 1
      const contentLength = this.monoCallData[userPhoneNumber]['contentLength']
      await this.loadChunkAndMapToFrames(userPhoneNumber,blobName,this.monoCallData[userPhoneNumber]['chunkNumber'],contentLength)
    }
    setAudioStreamState(userPhoneNumber,"play")
    this.streamAudioInMonoCall(userPhoneNumber,audioId,18)
  }
  else if(digits === "9"){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)  // it will be helpful incase the timer starts once the audio is over, so when the user presses 9, then it stops the previous timer
    this.stopAudioMessage(uuid)
    clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
    await storeLog(userPhoneNumber,digits,"chosen exit")
    setAudioStreamState(userPhoneNumber,"pause")
    
    const nextActions = await handleGoingBackToPreviousMenuFromAudioStreaming(params)

    setTimerForInactivityOfUserInPullModel(userPhoneNumber) // as we are pausing and taking user to main menu.
    if(Array.isArray(nextActions) && nextActions.length === 0){
      return res.send([])
    }
    
    const ncco = nextActions
    return res.send(ncco)
  }
  else if(audioState !== "finished" &&  digits === "2"){
    if(audioState === "play"){
      clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
      await storeLog(userPhoneNumber,digits,"chosen pause")
      setAudioStreamState(userPhoneNumber,"pause")
      const audioPausedMessage = [
        global.pullMenuMainUrl + global.audioPausedMessageUrl.replace('{language}',language).replace('{speechRate}',speechRate)
      ]
      setTimerForInactivityOfUserInPullModel(userPhoneNumber)
      this.playAudioMessage(uuid,audioPausedMessage)
    }
    else{
      stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
      await storeLog(userPhoneNumber,digits,"chosen resume")
      setAudioStreamState(userPhoneNumber,"play")
      this.streamAudioInMonoCall(userPhoneNumber,audioId,18)
    }
  }
  else if(audioState !== "finished" && (digits === "#" || digits === "*")){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
    var needToChange = false
    var nextSpeechRateIndex = undefined

    if(digits === "#"){
      await storeLog(userPhoneNumber,digits,"chosen increment Speech Rate")
      if(currentSpeechRateIndex != global.speechRates.length-1){
        nextSpeechRateIndex = currentSpeechRateIndex + 1
        needToChange = true
      }
    }
    else if(digits === "*"){
      await storeLog(userPhoneNumber,digits,"chosen decrement Speech Rate")
      if(currentSpeechRateIndex != 0){
        nextSpeechRateIndex = currentSpeechRateIndex - 1
        needToChange = true
      }
    }
    
    if(needToChange){
      if(this.audioData[userPhoneNumber].hasOwnProperty(this.monoCallData[userPhoneNumber]['chunkNumber'])){
        clearTimeout(this.monoCallData[userPhoneNumber]["playAudioTimeoutId"])
        const currentSpeechRate = Number(global.speechRates[currentSpeechRateIndex])
        const nextSpeechRate = Number(global.speechRates[nextSpeechRateIndex])

        const globalFramePos = (this.monoCallData[userPhoneNumber]['chunkNumber'] - 1) * this.numberOfFramesPerChunk + this.monoCallData[userPhoneNumber]['frameNumber']
        const nextSpeechRateGlobalFramePos = Math.floor((globalFramePos + 1) * (currentSpeechRate / nextSpeechRate))
        const nextSpeechRateLocalFramePos = this.getLocalFrameNumberFromGlobalFrameNumber(nextSpeechRateGlobalFramePos)
        const nextSpeechRateChunkNumber = this.getChunkNumberFromFrameNumber(nextSpeechRateGlobalFramePos)

        const blobName = `${audioId}/${global.speechRates[nextSpeechRateIndex]}.wav`
        const contentLength = await getLengthOfBlob(blobName)

        await setCurrentSpeechRateIndex(userPhoneNumber,nextSpeechRateIndex)

        this.monoCallData[userPhoneNumber]['chunkNumber'] = nextSpeechRateChunkNumber
        this.monoCallData[userPhoneNumber]['frameNumber'] = nextSpeechRateLocalFramePos
        this.monoCallData[userPhoneNumber]['contentLength'] = contentLength

        await this.loadChunkAndMapToFrames(userPhoneNumber,blobName,nextSpeechRateChunkNumber,contentLength)

      }
    } 
    if(needToChange || audioState==="pause"){
      setAudioStreamState(userPhoneNumber,"play")
      this.streamAudioInMonoCall(userPhoneNumber,audioId,18)
    }
  }
  else{
    await storeLog(userPhoneNumber,digits,`chosen Invalid Option`)
  }
  res.send([
    getStreamAction(global.silenceStreamUrl,0),
    DTMFInputAction(currentEndPoint)
  ]);
}

async handleUserInputOfKeyLearningInMonoCall(req,res){
  const { handleUserInputOfKeryLearning } = require("../controllers/monoCall/monoCall")
  const { getStreamAction } = require("../nccoActions")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const response = await handleUserInputOfKeryLearning(params)

  if(Array.isArray(response) && response.length === 0){
    return res.send([])
  }

  const { isExitRequest,nextActions } = response
  if(isExitRequest){
    const ncco = nextActions
    return res.send(ncco)
  }
  else{
    const n = nextActions.length
    const ncco = [
      ...nextActions.slice(0,n-1),
      getStreamAction(global.silenceStreamUrl,0),
      nextActions[n-1]
    ]

    return res.send(ncco)
  }
}

async handleScrambleMainMenuInMonoCall(req,res){
const { handleScrambleMainMenu } = require("../controllers/monoCall/monoCall")
const { getStreamAction } = require("../nccoActions")

const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
const response = await handleScrambleMainMenu(params)

if(Array.isArray(response) && response.length === 0){
  return res.send([])
}

const { isExploringLineOrder,nextActions } = response

if(isExploringLineOrder){
  const n = nextActions.length
  const ncco = [
    ...nextActions.slice(0,n-1),
    getStreamAction(global.silenceStreamUrl,0),
    nextActions[n-1]
  ]

  return res.send(ncco)
}
else{
  const ncco = nextActions
  return res.send(ncco)
}
}

async handleScrambleExplorePoemLineOrderingInMonoCall(req,res){
const { handleScrambleExplorePoemLineOrdering } = require("../controllers/monoCall/monoCall")
const { getStreamAction } = require("../nccoActions")

const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
const response = await handleScrambleExplorePoemLineOrdering(params)

if(Array.isArray(response) && response.length === 0){
  return res.send([])
}

const { isExploringLineOrder,nextActions } = response

if(isExploringLineOrder){
  const n = nextActions.length
  const ncco = [
    ...nextActions.slice(0,n-1),
    getStreamAction(global.silenceStreamUrl,0),
    nextActions[n-1]
  ]

  return res.send(ncco)
}
else{
  const ncco = nextActions
  return res.send(ncco)
}
}

async handleScrambleEnteredPoemLineSequenceInMonoCall(req,res){
  const { handleScrambleEnteredPoemLineSequence } = require("../controllers/monoCall/monoCall") 

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const nextActions = await handleScrambleEnteredPoemLineSequence(params)

  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }

  const ncco = nextActions

  return res.send(ncco)
}

async handleScrambleEnterSequenceAgainOrRepeatContentInputInMonoCall(req,res){
  const { handleScrambleEnterSequenceAgainOrRepeatContentInput } = require("../controllers/monoCall/monoCall") 
  const { getStreamAction } = require("../nccoActions")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const response = await handleScrambleEnterSequenceAgainOrRepeatContentInput(params)

  if(Array.isArray(response) && response.length === 0){
    return res.send([])
  }

  const { isExploringLineOrder,nextActions } = response

  if(isExploringLineOrder){
    const n = nextActions.length
    const ncco = [
      ...nextActions.slice(0,n-1),
      getStreamAction(global.silenceStreamUrl,0),
      nextActions[n-1]
    ]

    return res.send(ncco)
  }
  else{
    const ncco = nextActions
    return res.send(ncco)
  }
}

async handleQuizMainMenuInMonoCall(req,res){
  const { handleQuizMainMenu } = require("../controllers/monoCall/monoCall") 
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const nextActions = await handleQuizMainMenu(params)

  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }

  const ncco = nextActions

  return res.send(ncco)
}

async handleQuizMultipleChoiceSelectionInMonoCall(req,res){
  const { handleQuizMultipleChoiceSelection } = require("../controllers/monoCall/monoCall")
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const nextActions = await handleQuizMultipleChoiceSelection(params)
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }
  const ncco = nextActions

  return res.send(ncco)
}

async handleContentListMenuOfRiddleExperienceInMonoCall(req,res){
  const { handleContentListMenuOfRiddleExperience } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")
  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber, digits } = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleContentListMenuOfRiddleExperience(params)

  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }
  const ncco = nextActions

  return res.send(ncco)
}

async handleRiddleContentPlayingInMonoCall(req,res){
  const { handleRiddleContentPlaying } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber, digits } = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleRiddleContentPlaying(params)

  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }
  const ncco = nextActions

  return res.send(ncco)
}

async handleThemeTypeInputInMonoCall(req,res){
  const { handleThemeTypeInput } = require("../controllers/monoCall/monoCall")
  const { setTimerForInactivityOfUserInPullModel, stopTimerForInactivityOfUserInPullModel } = require("../controllers/monoCall/utils")

  const params = this.getRequiredParamsFromVonageBodyForMonoCall(req.body)
  const { userPhoneNumber,digits } = params
  if(digits !== ""){
    stopTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }

  const nextActions = await handleThemeTypeInput(params)

  if(digits !== ""){
    setTimerForInactivityOfUserInPullModel(userPhoneNumber)
  }
  if(Array.isArray(nextActions) && nextActions.length === 0){
    return res.send([])
  }
  const ncco = nextActions

  return res.send(ncco)
}
}
module.exports = VonageAPI