const { sendResponse, convertMessageFromTextToAudio, addForInOptionAudio, getURLForPLACE, sendMessageToMQ } = require("../utils")


module.exports = async function createQuizAudios(context, req){
    if(!req.body.language || !req.body.questions || !req.body.options || !req.body.id){
        throw {
            message:"language, questions, options or id property not defined.",
            statusCode:400
        }
    }
    sendResponse(context, 200, "CREATING... Check IVR PULL MODEL AFTER FEW MINUTES")
    console.log(req.body);
    var lang = global.humanLanguageCodeToTranslationLanguageCode[req.body.language.toLowerCase()]
    var containerName = 'output-container'
    
    const extension = ".mp3"
    const separator = "/"
    const destinationContainerClient = global.blobServiceClient.getContainerClient(containerName);
    
    //CREATE TITLE AUDIO
    var titleAudioContainerName = 'experience-titles'
    const titleAudioContainerClient = global.blobServiceClient.getContainerClient(titleAudioContainerName);
    var titleAudioUrl = ''
    const filePath = 'quiz' + separator + req.body.id
    for(const speechRate of global.speechRates){
      const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio(lang, req.body.title), lang, speechRate - global.speechRateMargin)
      const fullFilePath = filePath + separator + speechRate + extension
      const outputBlockBlobClient = titleAudioContainerClient.getBlockBlobClient(fullFilePath);
      await outputBlockBlobClient.uploadStream(audioStream);
      titleAudioUrl = outputBlockBlobClient.url
      console.log(`FINISHED TITLE AUDIO PROCESSING ${fullFilePath}`);
    }
    req.body.titleAudio = encodeURI(getURLForPLACE(decodeURIComponent(titleAudioUrl)))

    // CREATE QUESTION AUDIOS
    var questionFps = [];
    var i = 1;
    for(let val of req.body.questions) {
      const filePath = 'Quiz' + separator + req.body.id + separator + `question_${i}`
      var url = ''
      for(const speechRate of global.speechRates){
        const audioStream = await convertMessageFromTextToAudio(val, lang, speechRate - global.speechRateMargin)
        const fullFilePath = filePath + separator + speechRate + extension
        const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
        await outputBlockBlobClient.uploadStream(audioStream);
        url = outputBlockBlobClient.url
        console.log(`FINISHED PROCESSING ${fullFilePath}`);
      }
      i++
      questionFps.push(encodeURI(getURLForPLACE(decodeURIComponent(url))))
    };
    console.info(questionFps)

    // CREATE OPTIONS AUDIOS
    var optionFps = [];
    var i = 1;
    for(let val of req.body.options) {
      var optionNum = 1;
      op = []
      for(let option of val){
        const filePath = 'Quiz' + separator + req.body.id + separator + `option_${i}`+ separator + `${optionNum}`
        var url = ''
        for(const speechRate of global.speechRates){
          const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio(lang, option), lang, speechRate - global.speechRateMargin)
          const fullFilePath = filePath + separator + speechRate + extension
          const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
          await outputBlockBlobClient.uploadStream(audioStream);
          url = outputBlockBlobClient.url
          console.log(`FINISHED PROCESSING ${fullFilePath}`);
        }
        optionNum++
        op.push(encodeURI(getURLForPLACE(decodeURIComponent(url))))
      }
      i++
      optionFps.push(op)
    }
    console.info(optionFps)
    req.body.questions = questionFps
    req.body.options = optionFps
    req.body.speechRates = global.speechRates
    console.log(JSON.stringify(req.body))
    await sendMessageToMQ(req.body)
}
