async function deleteBlobFromAContainer(containerName,blobNamePrefix){
  const containerClient = global.blobServiceClient.getContainerClient(containerName);
  const options = {
    deleteSnapshots: 'include' // or 'only'
  }
  const blobList = containerClient.listBlobsFlat({prefix:blobNamePrefix})
  for await (const blob of blobList){
    await containerClient.deleteBlob(blob.name,options)
    console.log(`Deleted blob with name = ${blob.name}`)
  }
}

module.exports = async function deleteTitleAudio(body){
  if(body.id){
    var containerName = "experience-titles"
    var blobNamePrefix = body.id
    await deleteBlobFromAContainer(containerName,blobNamePrefix)
  }
  else{
    throw {
      message:"id param is Required.",
      statusCode:400
    }
  }
}