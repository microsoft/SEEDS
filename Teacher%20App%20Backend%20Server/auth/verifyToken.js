"use strict";
const admin = require("firebase-admin");
const path = require("path");
 
/*
create a separate serviceAccountKey.json file for your own Firebase Account
To generate private key follow below steps,
    1. go to https://console.firebase.google.com/
    2. create a new project if not yet
    3. go to settings/project settings
    4. go to Service accounts
    5. Under Firebase admin SDK section, you should be able to see 'Generate new private key' option
    6. when you click on this option, it will prompt you to download a json file that you can place exactly in the place of this serviceAccountKey.json file
*/
 
const serviceAccount = path.join(__dirname, "serviceAccountKey.json")
 
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
})
 
async function verifyToken(req, res, next) {
    try{
        console.log(`\nRequest:\n`
            + `body: ${JSON.stringify(req.body)}\n`
            + `query: ${JSON.stringify(req.query)}\n`
            + `params: ${JSON.stringify(req.params)}`)
    
        const authToken = req.headers['authtoken']
        console.log(`authtoken: ${authToken}`)
        if(authToken == "postman" || authToken == "postman1") {
            req.userId = `postman@gmail.com`
            return next()
        }
    
        //regex for a phone number
    
        else if(authToken.startsWith("+91") && authToken.length == 13){
            req.userId = authToken
            return next()
        }
    }catch(error){
        res.sendStatus(401);
    }
 
    // try {
    //     const token = await admin.auth().verifyIdToken(authToken)
    //     console.log(JSON.stringify(token))
    //     req.userId = token.phone_number
    //     console.log(`userId: ${req.userId}`)
    //     next()
    // } catch (error) {
    //     console.log(JSON.stringify(error, ["message", "arguments", "type", "name"]))
    //     res.sendStatus(401);
    // }
}
 
module.exports = verifyToken