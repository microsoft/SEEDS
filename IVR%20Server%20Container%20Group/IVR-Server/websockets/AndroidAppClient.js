"use strict";

const { WebPubSubEventHandler } = require("@azure/web-pubsub-express");

const {
  logClientRequest,
  handleUserEventFromAndroidPubsub,
  endConference
} = require("../controllers/Conference/conferenceCall");
const { tryCatchWrapperForAndroidPubSub } = require("../controllers/Conference/utils");


let handler = new WebPubSubEventHandler(process.env.AZURE_PUBSUB_HUB_NAME, {
  path: "/",
  onConnected: tryCatchWrapperForAndroidPubSub(async (req) => {
    const userId = req.context.userId;
    if(global.confIdToEndConferenceTimeoutId[userId] != undefined){
      console.log("Android websocket timeoutId entered.")
      clearTimeout(global.confIdToEndConferenceTimeoutId[userId])
      delete global.confIdToEndConferenceTimeoutId[userId]
    }
    console.log(`${userId} connected`);
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    await logClientRequest(userId,indianTime,"/azurepubsubhook","AndroidWebSocket: onConnected","WebSocket","AndroidWebSocket:Connected")
  }),
  onDisconnected: tryCatchWrapperForAndroidPubSub(async (req) => {
    const confId = req.context.userId
    console.log(`Android Websocket with confId ${confId} is disconnected`)
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    const timeToWaitBeforeEndingConference = 180000
    // when the Android app is disconnected, we are initiating some timer with time period *timeToWaitBeforeEndingConference*
    //  if the android app is not re-connected within this time, we will end the whole Conference running in that App. so, below function is to achieve the same.
    global.confIdToEndConferenceTimeoutId[confId] = setTimeout(() =>{
      console.log("Android websocket timeout happened.");
      endConference(confId).then(console.log).catch(console.log);
      delete global.confIdToEndConferenceTimeoutId[confId]
    },timeToWaitBeforeEndingConference)
    await logClientRequest(confId,indianTime,"/azurepubsubhook","AndroidWebSocket: onDisConnected","WebSocket","AndroidWebSocket:DisConnected")
  }),
  handleUserEvent: async (req, res) => {
    tryCatchWrapperForAndroidPubSub(handleUserEventFromAndroidPubsub)(req)
    res.success();
  },
});

module.exports = handler;
