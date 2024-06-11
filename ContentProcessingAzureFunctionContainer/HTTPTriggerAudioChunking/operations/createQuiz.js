const { sendResponse, convertMessageFromTextToAudio, addForInOptionAudio, getURLForPLACE } = require("../utils")

async function createQuizAudiosBackgroundTask(context, req){
    console.log("REQUEST BODY " + JSON.stringify(req.body));
    var lang = global.humanLanguageCodeToTranslationLanguageCode[req.body.language.toLowerCase()]
    var containerName = 'output-container'
    let responseData = {}
    
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
    responseData.titleAudio = encodeURI(getURLForPLACE(decodeURIComponent(titleAudioUrl))) + '/1.0.mp3';

    // CREATE/FETCH THEME AUDIO
    const theme = req.body.theme.trim()
    const localTheme = req.body.localTheme.trim()
    var themeTitleContainerName = "theme-titles"
    const themeTitleContainerClient = global.blobServiceClient.getContainerClient(themeTitleContainerName);
    const themeTitleFilePath = theme + separator + req.body.language.toLowerCase()
    const themeBlobName = themeTitleFilePath + separator + "1.0" + extension
    const themeBlobClient = themeTitleContainerClient.getBlockBlobClient(themeBlobName);
    if(await themeBlobClient.exists()){
      url = themeBlobClient.url
      console.log(`existing theme url = ${url}`)
    }
    else{
      for(const speechRate of global.speechRates){
        const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio(lang, localTheme), lang, speechRate - global.speechRateMargin)
        const fullFilePath = themeTitleFilePath + separator + speechRate + extension
        const outputBlockBlobClient = themeTitleContainerClient.getBlockBlobClient(fullFilePath);
        await outputBlockBlobClient.uploadStream(audioStream);
        url = outputBlockBlobClient.url
        console.log(`FINISHED PROCESSING ${fullFilePath}`);
      }
    }
    responseData.themeAudio = encodeURI(getURLForPLACE(decodeURIComponent(url)) + '/1.0.mp3')

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
      questionFps.push(encodeURI(getURLForPLACE(decodeURIComponent(url)) + '/1.0.mp3'))
    };

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
        op.push(encodeURI(getURLForPLACE(decodeURIComponent(url)) + '/1.0.mp3'))
      }
      i++
      optionFps.push(op)
    }
    responseData.questionAudios = questionFps
    responseData.optionsAudios = optionFps
    // ADD AUDIO DATA TO REQUEST OBJECT TO COMPLETE THE QUIZ INFO
    req.body.quizAudioData = responseData
    console.log(JSON.stringify(req.body))
    
    fetch(process.env.SEEDS_SERVER_BASE_URL + "content/quiz", {
      method: 'PATCH',
      headers: {
          'Content-Type': 'application/json',
          'authToken' :'postman'
      },
      body: JSON.stringify(req.body)
  })
  .then(response => response.json())
  .then(data => console.log('Success response from SEEDS server for PATCH Quiz Request:', data))
  .catch((error) => console.error('Error response from SEEDS server for PATCH Quiz Request:', error));
}

module.exports = async function createQuizAudios(context, req){
    if(!req.body.language || !req.body.questions || !req.body.options || !req.body.id || !req.body.title || !req.body.theme || !req.body.localTheme){
        throw {
            message:"language, questions, options, id, title, theme or localTheme property not defined.",
            statusCode:400
        }
    }
    createQuizAudiosBackgroundTask(context, req)
    sendResponse(context, 200, "CREATING... Check IVR PULL MODEL AFTER FEW MINUTES")
}
