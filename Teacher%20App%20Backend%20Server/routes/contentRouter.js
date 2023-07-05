"use strict";
const express = require("express");
const path = require("path");
const Content = require("../models/Content.js");
const { tryCatchWrapper } = require(path.join("..", "util.js"))
const fetch = (...args) => import('node-fetch').then(({default: fetch}) => fetch(...args));
const { 
    generateBlobSASQueryParameters, 
    BlobSASPermissions, 
    StorageSharedKeyCredential,
    BlobServiceClient,
} = require('@azure/storage-blob');

const constants = {
    accountName: process.env.AZURE_STORAGE_ACCOUNT_NAME,
    accountKey: process.env.AZURE_STORAGE_ACCOUNT_KEY
};
const sharedKeyCredential = new StorageSharedKeyCredential(
    constants.accountName,
    constants.accountKey
);

const blobServiceClient = BlobServiceClient.fromConnectionString(process.env.AZURE_STORAGE_CONNECTION_STRING);
const containerName = "input-container"
const containerClient = blobServiceClient.getContainerClient(containerName)

const router = express.Router();

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
    const sasOptions = {
        containerName: containerClient.containerName,
        blobName: req.query.blobName,
        startsOn: new Date(),
        expiresOn: new Date(new Date().valueOf() + 3600 * 1000), //change this time duration later
        permissions: BlobSASPermissions.parse("rw"),
    };
    const sasToken = generateBlobSASQueryParameters(sasOptions, sharedKeyCredential).toString();

    return res.json({
        sasToken:`${containerClient.getBlockBlobClient(req.query.blobName).url}?${sasToken}`
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
    const response =  await fetch(
        'https://seedscontent.azurewebsites.net/api/acs',
        {
            method: 'POST',
            body: JSON.stringify({
                type:"delete-blob",
                id: req.params.contentId
            }),
            headers: { 'Content-Type': 'application/json' }
        }
    )
    return res.json(await Content.deleteOne({ id: req.params.contentId }));
}))

router.post("/", tryCatchWrapper(async (req, res) => {
    const content = new Content(req.body);
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
    const containerClient = blobServiceClient.getContainerClient(containerName);
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
    const containerClient = blobServiceClient.getContainerClient(containerName)
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