// in-built libraries
require('dotenv').config()
global.fs = require('fs')
global.path = require('path')

// third-party libraries
const { BlobServiceClient } = require('@azure/storage-blob');
global.blobServiceClient = BlobServiceClient.fromConnectionString(
  process.env.BLOB_STORAGE_CONNECTION_STRING
);

// internal libraries
require("./globalData")
const {pull_model_data} = require('./pull_model_menu_data');
const {content_title} = require('./content_title');
const createTitleAudio  = require('./operations/titleAudio')
const deleteBlob = require('./operations/deleteBlob')
const deleteTitleAudio = require("./operations/deleteTitleAudio")
const createQuizAudios = require('./operations/createQuiz')
const deleteQuiz = require('./operations/deleteQuiz')
const populateAudioDBFromObject = require('./operations/pullModelMenuFromJSON')
const createTitleAudioFromJSON = require('./operations/titleAudioFromJSON');
const doTranslateAndTTSInMultipleLanguages = require('./operations/translate-tts-in-multiple-languages')
const { sendResponse } = require('./utils');


module.exports = async function (context, req) {
  try{
    var resp = undefined
    switch(req.body.type){
      case global.createTitleAudio:
        resp = await createTitleAudio(req.body)
        sendResponse(context,200,resp)
        break;
      case global.deleteTitleAudio:
        resp = await deleteTitleAudio(req.body)
        sendResponse(context,200,`Deleted Blob With Id = ${req.body.id} from experience-titles`)
        break;
      case global.deleteBlob:
        await deleteBlob(req.body)
        sendResponse(context,200,`Deleted Blob With Id = ${req.body.id}`)
        break;
      case global.createQuiz:
        await createQuizAudios(context, req);
        break;
      case global.deleteQuiz:
        await deleteQuiz(context, req);
        break;
      case global.createPullModelMenuFromJSON:
        await populateAudioDBFromObject(pull_model_data, "");
        break;
      case global.createTitleAudioFromJSON:
        await createTitleAudioFromJSON(content_title);
        break;
      case global.translateTTSMultiLang:
        await doTranslateAndTTSInMultipleLanguages(req.body)
        sendResponse(context,200,`Successfully translated text ${req.body.text} and converted to speech.`)
        break;
      default:
        throw {
          message:"Invalid Request Type. Please check the type param.",
          statusCode:400
        }
    }
  }
  catch(error){
    sendResponse(context,error.statusCode?error.statusCode:500,`Error Occurred. Message = ${error.message}`)
  }
}
