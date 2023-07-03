// azure-cognitiveservices-speech.js

require('dotenv').config()

const sdk = require('microsoft-cognitiveservices-speech-sdk');
const { Buffer } = require('buffer');
const { PassThrough } = require('stream');
const fs = require('fs');

var subscriptionKey = process.env.TTS_KEY
var region = process.env.TTS_REGION

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
    return new Promise((resolve, reject) => {
        
        const speechConfig = sdk.SpeechConfig.fromSubscription(subscriptionKey, region);
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
    });
};

module.exports = {
    textToSpeech
};