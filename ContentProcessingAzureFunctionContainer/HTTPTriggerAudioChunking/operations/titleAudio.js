const { addForInOptionAudio, getURLForPLACE, convertMessageFromTextToAudio } = require("../utils")

module.exports = async function createTitleAudio(body){
    const titleText = body.localTitle?body.localTitle.trim():body.localTitle
    const audioId = body.id?body.id.trim():body.id
    const language = body.lang?body.lang.trim():body.lang
    const experienceType = body.expType?body.expType.trim():body.expType
    const theme = body.theme?body.theme.trim():body.theme
    const localTheme = body.localTheme?body.localTheme.trim():body.localTheme
  
    if(titleText && audioId && language && experienceType && theme && localTheme){
      var result = {}
      var experienceTitleContainerName = 'experience-titles'
      var themeTitleContainerName = "theme-titles"

      const experienceTitleContainerClient = global.blobServiceClient.getContainerClient(experienceTitleContainerName);
      const themeTitleContainerClient = global.blobServiceClient.getContainerClient(themeTitleContainerName);
  
      const extension = ".mp3"
      const separator = "/"
      const experienceTitleFilePath = audioId
      const themeTitleFilePath = theme
  
      const transLang = global.humanLanguageCodeToTranslationLanguageCode[language.toLowerCase()]
  
      var url = ''
  
      for(const speechRate of global.speechRates){
        const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio(transLang, titleText), transLang, speechRate - global.speechRateMargin)
        const fullFilePath = experienceTitleFilePath + separator + speechRate + extension
        const outputBlockBlobClient = experienceTitleContainerClient.getBlockBlobClient(fullFilePath);
        await outputBlockBlobClient.uploadStream(audioStream);
        url = outputBlockBlobClient.url
        console.log(`FINISHED PROCESSING ${fullFilePath}`);
      }
      result['titleAudio'] = encodeURI(getURLForPLACE(decodeURIComponent(url)))
  
      const themeBlobName = themeTitleFilePath + separator + "1.0" + extension
      const themeBlobClient = themeTitleContainerClient.getBlockBlobClient(themeBlobName);
      if(await themeBlobClient.exists()){
        url = themeBlobClient.url
        console.log(`existing url = ${url}`)
      }
      else{
        for(const speechRate of global.speechRates){
          const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio(transLang, localTheme), transLang, speechRate - global.speechRateMargin)
          const fullFilePath = themeTitleFilePath + separator + speechRate + extension
          const outputBlockBlobClient = themeTitleContainerClient.getBlockBlobClient(fullFilePath);
          await outputBlockBlobClient.uploadStream(audioStream);
          url = outputBlockBlobClient.url
          console.log(`FINISHED PROCESSING ${fullFilePath}`);
        }
      }

      result['themeAudio'] = encodeURI(getURLForPLACE(decodeURIComponent(url)))

      return result
    }
    else{
      throw {
        message:"Please Provide All the Parameters.",
        statusCode:400
      }
    }
  
  }
