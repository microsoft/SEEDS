const { sendResponse, sendMessageToMQ } = require("../utils");


module.exports = async function deleteQuiz(context, req){
    if(!req.body.id){
        throw {
            message:"id property not defined.",
            statusCode:400
        }
    }
    var containerName = 'output-container'
    const destinationContainerClient = global.blobServiceClient.getContainerClient(containerName);
    const listOptions = {
      includeMetadata: false,
      includeSnapshots: false,
      includeTags: false,
      includeVersions: false,
      prefix: `Quiz/${req.body.id}/`
    };
    let iter = destinationContainerClient.listBlobsFlat(listOptions);
    for await (const item of iter) {
      const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(item.name);
      await outputBlockBlobClient.deleteIfExists()
    }
    var message = {'type': global.deleteQuiz, "id": req.body.id};
    await sendMessageToMQ(message)
    sendResponse(context, 200, "DELETED ALL AUDIO FILES FOR QUIZ " + req.body.id);
}