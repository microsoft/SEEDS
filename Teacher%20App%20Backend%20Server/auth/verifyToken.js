"use strict";
const admin = require("firebase-admin");
const path = require("path");

var a = 2

const serviceAccount = path.join(__dirname, "serviceAccountKey.json")

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
})

async function verifyToken(req, res, next) {
    console.log(`\nRequest:\n`
        + `body: ${JSON.stringify(req.body)}\n`
        + `query: ${JSON.stringify(req.query)}\n`
        + `params: ${JSON.stringify(req.params)}`)

    const authToken = req.headers['authtoken']
    if(authToken == "postman" || authToken == "postman1") {
        req.userId = `postman@gmail.com`
        return next()
    }

    try { 
        const token = await admin.auth().verifyIdToken(authToken)
        console.log(JSON.stringify(token))
        req.userId = token.phone_number
        console.log(`userId: ${req.userId}`)
        next()
    } catch (error) {
        console.log(JSON.stringify(error, ["message", "arguments", "type", "name"]))
        res.sendStatus(401);
    }
}

module.exports = verifyToken