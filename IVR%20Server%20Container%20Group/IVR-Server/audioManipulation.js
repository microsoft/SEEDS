const { BlobServiceClient } = require('@azure/storage-blob');
require('dotenv').config({ path: ".env.dev" })

// Create the BlobServiceClient object which will be used to create a container client
const blobServiceClient = BlobServiceClient.fromConnectionString(
process.env.BLOB_STORAGE_CONNECTION_STRING
);

// Create a unique name for the container or use existed one
const containerName = "output-container";

// Get a reference to a container
const containerClient = blobServiceClient.getContainerClient(containerName);

function chunkArray(array, chunkSize) {
  var chunkedArray = [];
  for (var i = 0; i < array.length; i += chunkSize)
    chunkedArray.push(array.slice(i, i + chunkSize));
  return chunkedArray;
}


function divideIntoFrames(buffer){
  const arr = [...buffer]
  const numberOfBytesPerFrame = 320
  return chunkArray(arr,numberOfBytesPerFrame)
}

//here 80000 means number of Bytes per Chunk.
async function getChunkAsFrames(blobName,startBytePos,numberOfBytesRequired=80000){
    // Get a block blob client
    const blockBlobClient = containerClient.getBlockBlobClient(blobName);
    const buffer = await blockBlobClient.downloadToBuffer(startBytePos,numberOfBytesRequired)
    return divideIntoFrames(buffer)
}

async function getLengthOfBlob(blobName){
  const blockBlobClient = containerClient.getBlockBlobClient(blobName);
  const props = await blockBlobClient.getProperties()
  return props.contentLength
}

async function blobExists(blobName){
  const blockBlobClient = containerClient.getBlockBlobClient(blobName);
  const existsOrNot = await blockBlobClient.exists()
  return existsOrNot
}

module.exports = {
  getChunkAsFrames,
  getLengthOfBlob,
  blobExists,
}
