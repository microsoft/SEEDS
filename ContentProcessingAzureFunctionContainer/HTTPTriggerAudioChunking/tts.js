// azure-cognitiveservices-speech.js

require('dotenv').config()

const sdk = require('microsoft-cognitiveservices-speech-sdk');
const { Buffer } = require('buffer');
const { PassThrough } = require('stream');
const fs = require('fs');
const { DefaultAzureCredential } = require('@azure/identity');

async function getCognitiveServicesToken(resource) {
    const credential = new DefaultAzureCredential();
    try {
        const accessToken = await credential.getToken(resource);
        return accessToken.token;
    } catch (error) {
        console.error("Error fetching access token for resource: " + resource, error);
        throw error;
    }
}

async function createSpeechConfig() {
    const token = await getCognitiveServicesToken("https://cognitiveservices.azure.com/.default");
    const region = process.env.TTS_REGION
    const resourceId = process.env.TTS_RESOURCE_ID
    const authorizationToken = "aad#" + resourceId + "#" + token;

    const speechConfig = sdk.SpeechConfig.fromAuthorizationToken(authorizationToken, region);
    return speechConfig;
}

/**
 * Node.js server code to convert text to speech
 * @returns stream
 * @param {*} subscriptionKey your resource key
 * @param {*} region your resource region
 * @param {*} text text to convert to audio/speech
 * @param {*} filename optional - best for long text - temp file for converted speech/audio
 */

const textToSpeech = async (text, lang,rate, filename)=> {
    
    // convert callback function to promise
    return new Promise(async (resolve, reject) => {
        
        const speechConfig = await createSpeechConfig()
        speechConfig.speechSynthesisOutputFormat = 5; //mp3

        const voiceName = global.voiceName[lang]

        let audioConfig = null;
        const ssml = `<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xmlns:mstts="http://www.w3.org/2001/mstts" xml:lang="${lang}">` + 
                    `<voice name="${voiceName}">`+
                    `<prosody rate="${rate}" volume="+100.00%">`+
                    `${text}`+
                    `</prosody>` + 
                    `<mstts:silence type="Leading-exact" value="0ms"/>`+
                    `<mstts:silence type="Tailing-exact" value="0ms"/>`+
                    `</voice></speak>`;

        if (filename) {
            audioConfig = sdk.AudioConfig.fromAudioFileOutput(filename);
        }
        
        const synthesizer = new sdk.SpeechSynthesizer(speechConfig, audioConfig);
        
        synthesizer.speakSsmlAsync(
            ssml,
            result => {
                if (result.errorDetails) {
                    reject(result.errorDetails)
                    // console.error(result.errorDetails);
                } else {
                    const { audioData } = result;
                    if (filename) {
                    
                        // return stream from file
                        const audioFile = fs.createReadStream(filename);
                        resolve(audioFile);
                        
                    } else {
                        
                        // return stream from memory
                        const bufferStream = new PassThrough();
                        bufferStream.end(Buffer.from(audioData));
                        resolve(bufferStream);
                    }
                }
                synthesizer.close();
            },
            error => {
                synthesizer.close();
                reject(error)
            });
    }).catch(
        (err) => console.error(err)
    );
};

module.exports = {
    textToSpeech
};