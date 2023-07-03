const { translateTextIntoMultipleLanguages } = require("../translation")
const { convertAllTranslationsToAudio } = require("../utils")

module.exports = async function doTranslateAndTTSInMultipleLanguages(body){
    if(body.text && body.toLanguages && body.url && body.baseFolder){
        const text = body.text
        const toLanguages = body.toLanguages
        const url = body.url
        const baseFolder = body.baseFolder
        const folder = body.folder || text
      
        var toLangsInTranslationCode = []
        for(const lang of toLanguages){
          toLangsInTranslationCode.push(global.humanLanguageCodeToTranslationLanguageCode[lang])
        }
      
        const translations = await translateTextIntoMultipleLanguages(url,text,toLangsInTranslationCode)
        await convertAllTranslationsToAudio(translations,baseFolder,folder)
    }
    else
    {
        throw {
            message:"Please Provide All the Required parameters in body",
            statusCode:400
        }
    }
}
