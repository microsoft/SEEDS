// const Blob = require('buffer').Blob;
require('dotenv').config()
const axios = require('axios');

async function createSeperateFileForEachSpeechRate(ffmpeg,metadata,myBlob,blobExtension,inputBlobName,outputContainerClient,fetchFile){
  const WaveFile = require("wavefile").WaveFile;
  const speechRates = ["0.5","0.75","1.0","1.5","2.0"]
    for(const speechRate of speechRates){
        ffmpeg.FS("writeFile", `temp_in.${blobExtension}`, await fetchFile(myBlob));
        await ffmpeg.run(
          "-i",
          `temp_in.${blobExtension}`,
          "-filter:a",
          `atempo=${speechRate}`,
          "-vn",
          `temp_out.wav`
        );
        const data = ffmpeg.FS("readFile", `temp_out.wav`);
        const wav = new WaveFile(data);
        wav.toSampleRate(8000);
        wav.toBitDepth("16");
        const rawSamples = wav.getSamples()
        const noOfChannels = rawSamples.length
        var samples = null
        if(noOfChannels > 2){
          samples = wav.getSamples(true);
        }
        else{
          samples = rawSamples[0];
        }
        wav.fromScratch(1, 8000, '16', samples)
        
        var outputBlobNameWithExtension = `${inputBlobName}/${speechRate}.wav`

        const experienceName = metadata['experience']
        if(experienceName === "Riddle"){
          inputBlobName = inputBlobName.split("_")[0]
          if(metadata["question"] === "true"){
            outputBlobNameWithExtension = `${inputBlobName}/question/${speechRate}.wav`
          }
          else{
            outputBlobNameWithExtension = `${inputBlobName}/answer/${speechRate}.wav`
          }
        }

        // Get an output block blob client
        const outputBlockBlobClient = outputContainerClient.getBlockBlobClient(outputBlobNameWithExtension);
        await outputBlockBlobClient.uploadData(wav.toBuffer())
        console.log(`${outputBlobNameWithExtension} blob uploaded.`)
    }
}

async function copyBlobToOriginalContainer(ffmpeg,myBlob,blobExtension,inputBlockBlobClient,outputOriginalBlockBlobClient,fetchFile){
  if(blobExtension !== "mp3"){
      ffmpeg.FS("writeFile", `temp_in.${blobExtension}`, await fetchFile(myBlob));
      await ffmpeg.run(
        "-i",
        `temp_in.${blobExtension}`,
        "-filter:a",
        `atempo=1.0`,
        "-vn",
        `temp_out.mp3`
      );
      const data = ffmpeg.FS("readFile", `temp_out.mp3`);
  
      await outputOriginalBlockBlobClient.uploadData(data)
    }
    else{
      await outputOriginalBlockBlobClient.beginCopyFromURL(inputBlockBlobClient.url)
    }
}

async function deleteBlob(inputBlockBlobClient){
  const options = {
    deleteSnapshots: 'include' // or 'only'
  }
  await inputBlockBlobClient.deleteIfExists(options);
}

module.exports = async function (context, myBlob) {
    context.log("JavaScript blob trigger function processed blob \n Blob:", context.bindingData.blobTrigger, "\n Blob Size:", myBlob.length, "Bytes");
    const { BlobServiceClient } = require('@azure/storage-blob');

    const blobServiceClient = BlobServiceClient.fromConnectionString(
    process.env.BLOB_STORAGE_CONNECTION_STRING
    );

    var inputBlobName = context.bindingData.blobName
    const inputBlobExtension = context.bindingData.blobExtension
    
    // Create a unique name for the container
    const inputContainerName = "input-container";
    const outputContainerName = "output-container"
    const outputOriginalContainerName = "output-original"
    
    // Get a reference to input-container
    const inputContainerClient = blobServiceClient.getContainerClient(inputContainerName);
    
    // Get a reference to output-container
    const outputContainerClient = blobServiceClient.getContainerClient(outputContainerName);

    // Get a reference for a output-original container
    const outputOriginalContainerClient = blobServiceClient.getContainerClient(outputOriginalContainerName);

    const inputBlobNameWithExtension = `${inputBlobName}.${inputBlobExtension}`

    var outputOriginalBlobNameWithExtension = `${inputBlobName}.mp3`

    // Get input block blob client and corresponding metadata.
    const inputBlockBlobClient = inputContainerClient.getBlockBlobClient(inputBlobNameWithExtension);
    /*
    WARNING: The metadata object returned in the response will have its keys in lowercase, even if they originally contained uppercase characters.
     This differs from the metadata keys returned by the methods of ContainerClient that list blobs using the includeMetadata option, which will retain their original casing.
    */
    const metadata = (await inputBlockBlobClient.getProperties())['metadata']
    console.log(metadata)
    const experienceName = metadata['experience']
    if(experienceName === "Riddle"){
      inputBlobName = inputBlobName.split("_")[0]
      if(metadata["question"] === "true"){
        outputOriginalBlobNameWithExtension = `${inputBlobName}/question.mp3`
      }
      else{
        outputOriginalBlobNameWithExtension = `${inputBlobName}/answer.mp3`
      }
    }
    
    // Get an output original block blob client
    const outputOriginalBlockBlobClient = outputOriginalContainerClient.getBlockBlobClient(outputOriginalBlobNameWithExtension);
    
    //CHECK IF this content EXISTS IN SEEDS SERVER
    const SEEDSBaseUrl = process.env.SEEDS_SERVER_BASE_URL;
    var shouldProcessTheBlob = true;
    try{
      const resp = await axios.get(`${SEEDSBaseUrl}content/${inputBlobName}`,{headers:{ authToken:'postman'}})
      console.log(`SEEDS CHECK EXPERIENCE RESPONSE`)
      console.log(resp.data)
      shouldProcessTheBlob = resp.data != null;
    }
    catch(error){
      console.log(error)
    }

    if(!shouldProcessTheBlob){
      // Deleting original blob after processed.
      console.log('Experience not present')
      console.log(`Started Deleting blob ${inputBlobName} ...`)
      await deleteBlob(inputBlockBlobClient)
      console.log(`Blob ${inputBlobName} Deleted Successfully.`)
      console.log(`Finished processing blob ${inputBlobName}`)
      return
    }
  
    var { createFFmpeg, fetchFile } = require("@ffmpeg/ffmpeg");
    var ffmpeg = createFFmpeg();

    //loading ffmpeg...
    await ffmpeg.load();
    
    console.log(`Started processing blob ${inputBlobName} ...`)

    await createSeperateFileForEachSpeechRate(ffmpeg,metadata,myBlob,inputBlobExtension,inputBlobName,outputContainerClient,fetchFile)

    console.log(`${inputBlobName} finished.`)

    // converting input file to mp3 into output-original container
    console.log(`Started Copying blob ${inputBlobName} to original container...`)
    await copyBlobToOriginalContainer(ffmpeg,myBlob,inputBlobExtension,inputBlockBlobClient,outputOriginalBlockBlobClient,fetchFile)
    console.log(`Finished Copying blob ${inputBlobName} to original container`)

    // tell SEEDS server that audio is processed successfully if that is final audio with that content id
    try{
      if(metadata["isfinalaudio"] === "true"){
        const resp = await axios.get(`${SEEDSBaseUrl}content/${inputBlobName}/processed`,{headers:{ authToken:'postman'}})
        console.log(`SEEDS response`)
        console.log(resp.data)
      }
    }
    catch(error){
      console.log(error)
    }

    // Deleting original blob after processed.
    console.log(`Started Deleting blob ${inputBlobName} ...`)
    await deleteBlob(inputBlockBlobClient)
    console.log(`Blob ${inputBlobName} Deleted Successfully.`)
    console.log(`Finished processing blob ${inputBlobName}`)
};
