"use strict";
const express = require("express");
const path = require("path");
const Content = require("../models/Content.js");
const QuizCreateRequest = require("../models/QuizCreateRequest.js")
const QuizData = require("../models/QuizData.js")
const BlobService = require('../models/BlobService.js');
const { tryCatchWrapper } = require(path.join("..", "util.js"))
const fetch = (...args) => import('node-fetch').then(({default: fetch}) => fetch(...args));
const blobService = new BlobService();
const router = express.Router();

router.post("/quiz", tryCatchWrapper(async (req, res)=>{
    const quizCreateRequest = new QuizCreateRequest(req.body)
    if (quizCreateRequest.id === 'default-id') {
        return res.status(400).json({ error: "Invalid Quiz format" });
    }
    const quizData = QuizData.fromQuizCreateRequest(quizCreateRequest)
    quizData.creation_time = Math.floor(Date.now() / 1000);
    await quizData.save()
    
    // TRIGGER CREATION OF QUIZ AUDIO FILES
    await fetch(
        process.env.AUDIO_AZURE_FUNCTION_BASE_URL,
        {
            method: 'POST',
            body: JSON.stringify(quizCreateRequest),
            headers: { 'Content-Type': 'application/json' }
        }
    )
    return res.send(quizData)
}))

router.patch("/quiz", tryCatchWrapper(async (req, res)=>{
    const quizCreateRequest = new QuizCreateRequest(req.body)
    const quizData = QuizData.fromQuizCreateRequest(quizCreateRequest)
    quizData.isProcessed = true
    return res.json(await QuizData.findOneAndUpdate(
        { id: quizData._id },                   
        { $set: {
            themeAudio: quizData.themeAudio,
            titleAudio: quizData.titleAudio,
            questions: quizData.questions,
            isProcessed: quizData.isProcessed
        } },
        { new: true }
    ).exec())
}))

router.get("/sasUrl", tryCatchWrapper(async (req, res)=>{
    const url = req.query.url;  // URL is now obtained from query string
    if (!url) {
        return res.status(400).json({ error: "URL parameter is required." });
    }

    const urlWithSAS = await blobService.getURLWithSAS(url);
    return res.json({ url: urlWithSAS });
}))

router.get("/themes",tryCatchWrapper(async (req, res) => {
    const language = req.query.language
    const content = await Content.find({language:language,isPullModel:true,isProcessed:true}).sort({_id:-1})
    const themeSet = new Set()
    const themes = []
    console.log(content.length)
    console.log(language)
    for(const cont of content){
        const theme = cont.theme
        if(!themeSet.has(theme)){
            themes.push({
                name: theme,
                audioUrl: cont.themeAudio
            })
            themeSet.add(theme)
        }
    }
    return res.send(themes)
}))

router.get("/", tryCatchWrapper(async (req, res) => {
    if(req.query.language && req.query.theme && req.query.expName){
        const language = req.query.language
        const theme = decodeURIComponent(req.query.theme).toString()
        const type = req.query.expName
        return res.json(await Content.find({isProcessed:true,isPullModel:true,language:language,theme:theme,type:type}).sort({_id:-1}))
    }
    if (req.query.ids) {
        return res.json(await Content.getContentsByIds(req.query.ids))
    } 
    
    if (req.query.onlyTeacherApp) {
        return res.json(await Content.find({ isTeacherApp: true }).sort({_id: -1}).exec())
    }

    return res.json(await Content.getContent());
}))

async function regenerateAllTitleAudios(){
    const contents = await Content.find({isProcessed: true, isPullModel: true})
    for(const content of contents){
        console.log(`started working for content id=${content.id}`)
        const response = await fetch(
            'https://seedscontent.azurewebsites.net/api/acs',
            {
                method: 'POST',
                body: JSON.stringify({
                    type: "create-title-audio",
                    id: content.id,
                    localTitle: content.localTitle,
                    theme: content.theme,
                    localTheme: content.localTheme,
                    lang: content.language,
                    expType: content.type
                }),
                headers: { 'Content-Type': 'application/json' }
            }
        )
        const jsonResponse = await response.json()
        const titleAudio = jsonResponse.titleAudio
        const themeAudio = jsonResponse.themeAudio
        
        await Content.findOneAndUpdate(
            { id: content.id },                   
            { $set: { titleAudio, themeAudio } },
            { new: true }
        ).exec()

        console.log(`id = ${content.id}`)
        console.log(`titleAudio = ${titleAudio}`)
        console.log(`ThemeAudio = ${themeAudio}`)
    }
}

router.post("/regenerateAllTitleAudios", async (req,res) => {
    regenerateAllTitleAudios()
    return res.send("received. working on these...")
})

router.get("/sasToken", tryCatchWrapper(async (req, res) => {
    const containerName = "input-container"
    const sasToken = await blobService.getUploadSASToken(req.query.blobName, containerName)
    const container_client = blobService.getContainerClient(containerName)

    return res.json({
        sasToken:`${container_client.getBlockBlobClient(req.query.blobName).url}?${sasToken}`
    });
}))

router.get("/:contentId", tryCatchWrapper(async (req, res) => {
    return res.json(await Content.getContentById(req.params.contentId));
}))

router.get("/:contentId/processed", tryCatchWrapper(async (req, res) => {
    const content = await Content.getContentById(req.params.contentId);
    // if the content is already processed just return that content object
    if(content.isProcessed){
        return content
    }
    let titleAudio = content.titleAudio;
    let themeAudio = content.themeAudio;

    var response = undefined
    if (content.isPullModel) {
        response =  await fetch(
            'https://seedscontent.azurewebsites.net/api/acs',
            {
                method: 'POST',
                body: JSON.stringify({
                    type: "create-title-audio",
                    id: req.params.contentId,
                    localTitle: content.localTitle,
                    theme: content.theme,
                    localTheme: content.localTheme,
                    lang: content.language,
                    expType: content.type
                }),
                headers: { 'Content-Type': 'application/json' }
            }
        )
    }
    if(response){
        const jsonResponse = await response.json()
        titleAudio = jsonResponse.titleAudio
        themeAudio = jsonResponse.themeAudio
    }
    
    return res.json(await Content.findOneAndUpdate(
        { id: req.params.contentId },                   
        { $set: { isProcessed: true, titleAudio, themeAudio } },
        { new: true }
    ).exec())
}))

router.delete("/:contentId", tryCatchWrapper(async (req, res) => {
    // const response =  await fetch(
    //     'https://seedscontent.azurewebsites.net/api/acs',
    //     {
    //         method: 'POST',
    //         body: JSON.stringify({
    //             type:"delete-blob",
    //             id: req.params.contentId
    //         }),
    //         headers: { 'Content-Type': 'application/json' }
    //     }
    // )
    const result = await Content.updateOne({ id: req.params.contentId }, { $set: { isDeleted: true } });
    return res.json(result);
}))

router.post("/", tryCatchWrapper(async (req, res) => {
    let content = new Content(req.body);
    content.creation_time = Math.floor(Date.now() / 1000);
    await content.save();
    return res.json(content);
}))

router.patch("/",tryCatchWrapper(async (req,res) => {
    const isAudioUploaded = req.query.isAudioUploaded === "true"
    if(!req.body.isPullModel){
        const response =  await fetch(
            'https://seedscontent.azurewebsites.net/api/acs',
            {
                method: 'POST',
                body: JSON.stringify({
                    type:"delete-title-audio",
                    id: req.body.id
                }),
                headers: { 'Content-Type': 'application/json' }
            }
        )
    }
    var titleAudio = ""
    var themeAudio = ""
    var isProcessed = undefined
    if(isAudioUploaded){
        isProcessed = false
    }
    else{
        isProcessed = req.body.isProcessed
        if(req.body.isPullModel){
            const response =  await fetch(
                    'https://seedscontent.azurewebsites.net/api/acs',
                    {
                        method: 'POST',
                        body: JSON.stringify({
                            type: "create-title-audio",
                            id: req.body.id,
                            localTitle: req.body.localTitle,
                            theme: req.body.theme,
                            localTheme: req.body.localTheme,
                            lang: req.body.language,
                            expType: req.body.type
                        }),
                        headers: { 'Content-Type': 'application/json' }
                    }
                )
            const jsonResponse = await response.json()
            titleAudio = jsonResponse.titleAudio
            themeAudio = jsonResponse.themeAudio
        }
    }
    return res.json(await Content.findOneAndUpdate(
            {
                _id:req.body._id // we can use id:req.body.id here id means audioId, this also works.
            },
            {
                $set: {
                    title:req.body.title,
                    description: req.body.description,
                    type: req.body.type,
                    language: req.body.language,
                    isPullModel: req.body.isPullModel,
                    isTeacherApp: req.body.isTeacherApp,
                    isProcessed: isProcessed,
                    titleAudio: titleAudio,
                    localTitle: req.body.localTitle,
                    theme: req.body.theme,
                    localTheme: req.body.localTheme,
                    themeAudio: themeAudio
                }
            },
            {
                new: true
            }
        ))
}))

async function populateAllTitleAudiosForPullModel(){
    const contents = await Content.find({isPullModel: true})
    for(const content of contents){
        try{
            const response =  await fetch(
                'https://seedscontent.azurewebsites.net/api/acs',
                {
                    method: 'POST',
                    body: JSON.stringify({
                        type: "create-title-audio",
                        id: content.id,
                        localTitle: content.localTitle,
                        theme: content.theme,
                        localTheme: content.localTheme,
                        lang: content.language,
                        expType: content.type
                    }),
                    headers: { 'Content-Type': 'application/json' }
                }
            )
            const jsonResponse = await response.json()
            const titleAudio = jsonResponse.titleAudio
            const themeAudio = jsonResponse.themeAudio
        
            await Content.findOneAndUpdate(
                { id: content.id },                   
                { $set: { titleAudio, themeAudio } },
                { new: true }
            ).exec()
            console.log(`Processed for id = ${content.id} and title = ${content.title}`)
        }
        catch(err){
            console.log(`Error occured while handling content id = ${content.id} and Title = ${content.title}`)
            console.error(err)
        }

    }
}
router.post("/populate-title-audios",async (req,res) => {
    populateAllTitleAudiosForPullModel()
    return res.send("populating...")
})

async function deleteBlobFromAContainer(containerName,blobNamePrefix){
    const containerClient = blobService.getContainerClient(containerName);
    const options = {
      deleteSnapshots: 'include' // or 'only'
    }
    const blobList = containerClient.listBlobsFlat({prefix:blobNamePrefix})
    for await (const blob of blobList){
      await containerClient.deleteBlob(blob.name,options)
      console.log(`Deleted blob with name = ${blob.name}`)
    }
  }

async function deleteAudioBlobs(audioId){
    const blobNamePrefix = audioId

    var containerName = "output-container"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)

    containerName = "output-original"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)

    containerName = "experience-titles"
    await deleteBlobFromAContainer(containerName,blobNamePrefix)
}

async function deleteUnnecessaryStorage(){
    const docs = await Content.find({})
    const containerName = "output-container"
    const containerClient = blobService.getContainerClient(containerName)
    for(const doc of docs){
        var deleteDoc = false
        var deleteBlob = false
        if(!doc.isProcessed){
            deleteDoc = true
            deleteBlob = true
        }
        const blobs = containerClient.listBlobsFlat({prefix:doc.id})
        const iterator = blobs.next()
        const { done } = await iterator
        if(done){
            deleteDoc = true
        }
        if(deleteDoc){
            await Content.deleteOne({ id: doc.id })
            console.log(`Deleted doc with id = ${doc.id}`)
        }
        if(deleteBlob){
            await deleteAudioBlobs(doc.id)
            console.log(`Deleted blob with id = ${doc.id}`)
        }
    }
}
router.post("/delete-unnecessary-storage", async (req,res) => {
    await deleteUnnecessaryStorage()
    return res.send("Deleted Successfully")
})

// router.post("/change-type-from-snippets-to-snippet", async (req,res) => {
//     await Content.updateMany({type:"Snippets"},{$set: {type: "Snippet"}})
//     res.send("Done");
// })
module.exports = router;