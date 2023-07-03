/* This simple app uses the '/translate' resource to translate text from
one language to another. */

/* This template relies on the request module, a simplified and user friendly
way to make HTTP requests. */
require('dotenv').config()

const axios = require('axios');
const { v4: uuidv4 } = require("uuid");


var subscriptionKey = process.env.TRANSLATOR_KEY;

var baseUrl = 'https://api.cognitive.microsofttranslator.com/';

var region = process.env.TRANSLATOR_REGION;

function createOptions(url,text,toLangs){
    let options = {
        method: 'POST',
        baseURL: baseUrl,
        url: url,
        params: {
          'api-version': '3.0',
          'to': toLangs
        },
        headers: {
          'Ocp-Apim-Subscription-Key': subscriptionKey,
          'Ocp-Apim-Subscription-Region': region,
          'Content-type': 'application/json',
          'X-ClientTraceId': uuidv4()
        },
        data: [{
              'text': text
        }],
    };
    return options
}

/* If you encounter any issues with the base_url or path, make sure that you are
using the latest endpoint: https://docs.microsoft.com/azure/cognitive-services/translator/reference/v3-0-translate */
async function translateTextIntoMultipleLanguages(url,textToConvert,toLangs){
    const options = createOptions(url,textToConvert,toLangs)

    const resp = await axios(options)
    
    return resp.data[0].translations
};


module.exports = {
    translateTextIntoMultipleLanguages
}
