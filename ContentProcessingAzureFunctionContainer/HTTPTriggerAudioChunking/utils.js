
async function convertMessageFromTextToAudio(menuMessageText,lang,rate) {
    const { textToSpeech } = require('./tts');

    const audioStream = await textToSpeech(menuMessageText, global.translationLanguageCodeToAzureSpeechCode[lang],rate);

    return audioStream
}

async function convertAllTranslationsToAudio(translations,baseFolder,folder){

    const extension = ".mp3"
    const destinationContainerClient = global.blobServiceClient.getContainerClient('pull-model-menus');
  
    // const speechRates = ["x-slow","slow","medium","fast","x-fast"]
  
    for(const translation of translations){
      const convertedText = translation.text
      const convertedLang = translation.to
      for(const speechRate of global.speechRates){
        const audioStream = await convertMessageFromTextToAudio(convertedText,convertedLang,speechRate - global.speechRateMargin)
        const fullFilePath = global.path.join(baseFolder,global.translationLanguageCodeToHumanLanguageCode[convertedLang],folder,speechRate+extension)
        const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
        await outputBlockBlobClient.uploadStream(audioStream);
        console.log(`FINISHED PROCESSING ${fullFilePath}`);
      }
    }
}

function sendResponse(context,status,body){
    context.res = {
      status:status,
      body:body
    }
}

// TODO FOR TAMIL
function addForInOptionAudio(lang, option){
    var res = option.trim()
    switch(lang){
        case 'kn': // KANNADA
          res += 'ಗಾಗಿ'
          break;
        case 'en': // ENGLISH
          res = 'for ' + res
          break;
        case 'mr': // MARATHI
          res += 'साठी'
          break;
        case 'hi': // HINDI
          res += ' के लिए'
          break;
        case 'bn': // BENGALI
          res += ' জন্য'
          break;
        default:
          break;
    }
    return res
}

function getURLForPLACE(url){
    parts = url.split('/')
    newParts = parts.slice(0, parts.length-1)
    return newParts.join('/')
}

async function sendMessageToMQ(message){
    const { ServiceBusClient, ServiceBusMessage } = require("@azure/service-bus");
    const connectionString = process.env.SERVICE_BUS_CONNECTION_STRING;
    const queueName = process.env.SERVICE_BUS_QUEUE_NAME;
  
    const sbClient = new ServiceBusClient(connectionString);
      const sender = sbClient.createSender(queueName);
    try{
      await sender.sendMessages({body: message});
      await sender.close();
    }catch(ex){
      console.log(ex)
    }finally {
        await sbClient.close();
    }
}

async function convertAllTranslationsToAudioWithFilePaths(translations, filepaths){
    if (translations.length != filepaths.length) {
      throw new Error("NUMBER OF TRANSLATIONS != NUMBER OF FILEPATHS in convertAllTranslationsToAudioWithFilePaths()");
    }
    console.log(`convertAllTranslationsToAudioWithFilePaths ${translations[0].text} ${translations[0].to}`);
    const destinationContainerClient = global.blobServiceClient.getContainerClient('pull-model-menus');
  
    const extension = ".mp3"
    const separator = "/"
    // const speechRates = ["x-slow","slow","medium","fast","x-fast"]
    var i = 0;
    for(const translation of translations){
      const convertedText = translation.text
      const convertedLang = translation.to
      var speechRateFps = []
      var fp = ''
      for(const speechRate of global.speechRates){
        const audioStream = await convertMessageFromTextToAudio(convertedText,convertedLang, speechRate - global.speechRateMargin)
        const fullFilePath = filepaths[i] + separator + speechRate + extension
        const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
        await outputBlockBlobClient.uploadStream(audioStream);
        fp = encodeURI(getURLForPLACE(decodeURIComponent(outputBlockBlobClient.url)))
        console.log(`FINISHED PROCESSING ${fullFilePath}`);
      }
      var message = {'type': "pullModelMenuItem", 'filePaths': {}, 'basePath': filepaths[i], 'speechRates': global.speechRates};
      message['filePaths']['path'] = fp
      await sendMessageToMQ(message);
      i++;
    }
}

module.exports = {
    convertMessageFromTextToAudio,
    convertAllTranslationsToAudio,
    sendResponse,
    addForInOptionAudio,
    getURLForPLACE,
    sendMessageToMQ,
    convertAllTranslationsToAudioWithFilePaths,
    
}