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
