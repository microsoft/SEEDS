const { parse } = require("url");

// require("dotenv").config({ path: ".env" });

require("./globalVariables/Conference/conferenceCall")
require("./globalVariables/monoCall/monoCall")
require("./globalVariables/handCricket")
require("./globalVariables/monoCall/experiences/keyLearning")
require("./globalVariables/monoCall/experiences/quiz")
require("./globalVariables/monoCall/experiences/scramble")
require("./globalVariables/monoCall/experiences/riddle")

// global Variables.

// SEEDS server urls
global.SEEDSServerBaseUrl = process.env.SEEDS_SERVER_BASE_URL
global.fsmContentIdToPhoneNumberMappingUrl = global.SEEDSServerBaseUrl + 'call/fsmContext'
global.contentUrl = global.SEEDSServerBaseUrl + 'content'

global.fsmPublishEndPoint = 'https://place-seeds.azurewebsites.net/publishFSM'

global.audioData = {};
global.remoteUrl = process.env.SEEDS_IVR_SERVER_BASE_URL
global.speechRates = ["0.5","0.75","1.0","1.5","2.0"]
global.speechRateToName = {
  "0.5":"very low",
  "0.75":"low",
  "1.0":"normal",
  "1.5":"high",
  "2.0":"very high"
}
global.defaultSpeechRateIndex = 2
global.conversationUUIDToPhoneNumber = {}

global.callStatuses = {
  started:"started",
  ringing:"ringing",
  answered:"answered",
  joined:"joined",
  cancelled:"cancelled",
  busy:"busy",
  failed:"failed",
  completed:"completed"
}

const VonageAPI = require("./communicationAPIs/vonage")
global.communicationApi = new VonageAPI()

const app = require("./app");
const { replaceErrors, endAllConferencesInThisServer } = require("./controllers/Conference/conferenceCall");
const unhandledExceptionModel = require("./models/unhandledException")
const warningModel = require("./models/warning");
const { endAllPullModelCallsInThisServer } = require("./controllers/monoCall/utils");

const server = require("http").createServer(app);

server.on("upgrade", function upgrade(request, socket, head) {

  try{
    const { pathname } = parse(request.url);
    
    console.log(`pathname = ${pathname}`)
    if(pathname.startsWith("/vonage/")){
      communicationApi.handleWebSocketConnection(pathname,request,socket,head)
    }
    else {
      socket.destroy();
    }
  }
  catch(error){
    console.log(error.message)
  }
});

// process.on('uncaughtExceptionMonitor', async (err, origin) => {
//   await unhandledExceptionModel.create({error:JSON.stringify(err), origin: origin})
// });

process.on('uncaughtException', async (err, origin) => {
  await endAllConferencesInThisServer()
  await endAllPullModelCallsInThisServer()
  // store error in UnHandledException Model in MongoDB.
  await unhandledExceptionModel.create({error:JSON.stringify(err,replaceErrors), origin: origin})
  process.exit(1);
});

process.on('warning', async (warning) => {
  await warningModel.create({name:warning.name,message:warning.message,stack:warning.stack})
});

startServer = async () => {

  try{
    // importing connectDB function and calling inline
    await require("./db")()
    server.listen(process.env.IVR_PORT, () =>{
      console.log(`Running on port ${process.env.IVR_PORT}`)
      global.communicationApi.startCheckingSlot(0)
    });
  }
  catch(error){
    console.log("error occured.")
    console.log(error.message)
  }
}

startServer()