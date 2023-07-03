/* This simple app uses the '/translate' resource to translate text from
one language to another. */

/* This template relies on the request module, a simplified and user friendly
way to make HTTP requests. */
require('dotenv').config()

const axios = require('axios');


var subscriptionKey = process.env.TRANSLATOR_KEY;

var baseUrl = 'https://api.cognitive.microsofttranslator.com';

var region = process.env.TRANSLATOR_REGION;
function createOptions(text, ln, fromScript, toScript){
    let options = {
        method: 'POST',
        baseURL: baseUrl,
        url: 'transliterate',
        params: {
          'api-version': '3.0',
          'language': ln,
          'fromScript': fromScript,
          'toScript': toScript
        },
        headers: {
          'Ocp-Apim-Subscription-Key': subscriptionKey,
          'Ocp-Apim-Subscription-Region': region,
          'Content-type': 'application/json',
        },
        data: [{
              'Text': text
        }],
    };
    return options
}

async function transliterateToKnText(textToConvert){
    const options = createOptions(textToConvert, 'kn', 'Latn', 'Knda')

    const resp = await axios(options)
    
    return resp.data[0].text
};


module.exports = {
    transliterateToKnText
}
