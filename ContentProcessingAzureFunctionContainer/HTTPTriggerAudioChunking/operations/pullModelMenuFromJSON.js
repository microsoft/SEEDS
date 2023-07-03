const { translateTextIntoMultipleLanguages } = require("../translation");
const { convertAllTranslationsToAudioWithFilePaths } = require("../utils");


module.exports = async function populateAudioDBFromObject(obj, filePath) {
    for (let key in obj) {
      newPath = global.path.join(filePath, key);
      if (typeof obj[key] === "object") {
        await populateAudioDBFromObject(obj[key], newPath);   
      } else {
          const text = obj[key];
          newPath = newPath.replace("/text", "")
          newPath = global.path.join(newPath, `${text}`);
  
          //DO THE OPERATIONS
          var toLang = ['en'];
          if(newPath.includes('kannada')){
               toLang = ['kn'];
          }
  
          await translateTextIntoMultipleLanguages('translate', text, toLang)
          .then((translations) => convertAllTranslationsToAudioWithFilePaths(translations, [newPath]))
          .then(() => console.log(`successfully processed ${text} to All Requested Languages to Audio.`))
          .catch((error) => console.log(error.message))
      }
    }
}