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

module.exports = async function deleteBlob(body){
  if(body.id){
    var blobNamePrefix = body.id

    var containerName = "output-container"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)

    containerName = "output-original"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)

    containerName = "experience-titles"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)
  }
  else{
    throw {
      message:"id param is Required.",
      statusCode:400
    }
  }
}