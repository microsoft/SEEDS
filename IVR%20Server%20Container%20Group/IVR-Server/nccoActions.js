/*
Important! : You can rename this filename as ccoActions.js as eventhough i took all these actions as a reference from Vonage website, but my intention is to make all these actions as generic, so that should work irrespctive of Communication API
That's why i put it here separately instead in *vonage.js* file. So, you can use these actions as generic ones like how i am using currently.
*/

// This is for playing TTS Message
function getTalkAction(
  textToTalk,
  lang='en-IN',
  needBargeIn = true,
  loop = 1
) {
  var speechRate = "medium";
  if(lang === "kn-IN"){
    speechRate = "slow"
  }
  let style = 0
  let talkAction = {
    action: "talk",
    text:
      "<speak><prosody rate='" +
      `${speechRate}` +
      "'>" +
      `${textToTalk}</prosody></speak>`,
    bargeIn: needBargeIn,
    language: lang,
    loop:loop,
    style: style,
    level: 1,
  };
  return talkAction;
}

// This is to prompt the user for input(Voice input only)
function speechInputAction(
  endpoint,
  inputLang = 'en-IN'
) {
  let inputAction = {
    action: "input",
    eventUrl: [endpoint],
    type: ["speech"],
    speech: {
      language:inputLang,
      startTimeout: 4,
    },
  };
  return inputAction;
}

// This is to prompt the user for input(DTMF only)
function DTMFInputAction(endpoint,maxDigits = 1,timeOut=3){
  let inputAction = {
    action: "input",
    eventUrl: [endpoint],
    type: ["dtmf"],
    dtmf: {
      "maxDigits": maxDigits,
      "timeOut":timeOut
    }
  };
  return inputAction;
}

// This is for playing audio messages
function getStreamAction(
  streamUrl,
  loop=1,
  needBargeIn = true
) {
  const actionObj = {
    action: "stream",
    streamUrl: [streamUrl],
    level:1,
    loop: loop,
    level:1,
    bargeIn: needBargeIn,
  }
  return actionObj
}

module.exports = { getTalkAction, DTMFInputAction,getStreamAction };
