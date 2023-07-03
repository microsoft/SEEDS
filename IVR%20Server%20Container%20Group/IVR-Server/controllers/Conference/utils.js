"use strict";

const { saveErrorAndSendToAndroidClient } = require("./conferenceCall");

module.exports.tryCatchWrapperForReqResModel = f => { 
     return async function() { 
        const [req,res] = arguments 
         try {   
            return await f.apply(this, arguments)  
        } catch (error) {  
            // console.log(JSON.stringify(error, ["message", "arguments", "type", "name"]))  
            console.log(error.message)  
            return res.status(500).json({ message: error.message, stack: error.stack })
         }   
        }
    }

module.exports.tryCatchWrapperForAndroidPubSub = f => {
    return async function(req){
        try{
            return await f.apply(this,arguments)
        }
        catch(error){
            const confId = req.context.userId
            saveErrorAndSendToAndroidClient(confId,error)
            .then(console.log)
            .catch(console.log)
        }
    }
}
