const { convertMessageFromTextToAudio, addForInOptionAudio, getURLForPLACE } = require("../utils");


module.exports = async function createTitleAudioFromJSON(content_title){
  // console.log(content_title)
  var containerName = 'experience-titles'
  
  const destinationContainerClient = global.blobServiceClient.getContainerClient(containerName);
  var knTTS = content_title['kn']
  var enTTS = content_title['en']
  for(var item of knTTS){
    const extension = ".mp3"
    const separator = "/"
    const filePath = item['type'] + separator + item['id']
    var url = ''
    for(const speechRate of global.speechRates){
      const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio('kn', item['transliteration']), 'kn', speechRate - global.speechRateMargin)
      const fullFilePath = filePath + separator + speechRate + extension
      const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
      await outputBlockBlobClient.uploadStream(audioStream);
      url = outputBlockBlobClient.url
      console.log(`FINISHED PROCESSING ${fullFilePath}`);
    }
    item['url'] = encodeURI(getURLForPLACE(decodeURIComponent(url)))
    console.log("COMPLETED FOR " + item['title'])
  }

  for(var item of enTTS){
    const extension = ".mp3"
    const separator = "/"
    const filePath = item['type'] + separator + item['id']
    var url = ''
    for(const speechRate of global.speechRates){
      const audioStream = await convertMessageFromTextToAudio(addForInOptionAudio('en', item['title']), 'en', speechRate - global.speechRateMargin)
      const fullFilePath = filePath + separator + speechRate + extension
      const outputBlockBlobClient = destinationContainerClient.getBlockBlobClient(fullFilePath);
      await outputBlockBlobClient.uploadStream(audioStream);
      url = outputBlockBlobClient.url
      console.log(`FINISHED PROCESSING ${fullFilePath}`);
    }
    item['url'] = encodeURI(getURLForPLACE(decodeURIComponent(url)))
    console.log("COMPLETED FOR " + item['title'])
  }

  const op = {'kn' : knTTS, 'en' : enTTS}
  global.fs.writeFile('titlesAudioOp.txt', JSON.stringify(op), (err) => {
    if (err) throw err;
  })
  console.log(op)

}