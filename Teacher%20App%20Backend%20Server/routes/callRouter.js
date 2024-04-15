"use strict";
const express = require("express");
const { model } = require("mongoose");
const Call = require("../models/Call.js");
const CallLog = require("../models/CallLog.js");
const FsmContext = require("../models/FsmContext.js");
const path = require("path");
const { tryCatchWrapper } = require(path.join("..", "util.js"))
const axios = require('axios').default

const router = express.Router()

router.get("/accessToken", tryCatchWrapper(async (req, res) => {
    console.log(process.env.IVR_SERVER_URL)
    console.log("HERE")
    const response = await axios.get(`${process.env.IVR_SERVER_URL}conference_call/accessToken`)
    console.log(response.data)
    return res.json(response.data)
}))

router.post('/start', tryCatchWrapper(async (req, res) => {
    console.log("START CALL BODY", req.body)
    console.log("IVR_SERVER_URL", process.env.IVR_SERVER_URL)
    const response = await axios.post(`${process.env.IVR_SERVER_URL}conference_call`, req.body)
    console.log("START CALL RESPONSE", response.data)
    return res.json(response.data)
}))

router.get("/:confId/status", tryCatchWrapper(async (req, res) => {
    const response = await axios.get(`${process.env.IVR_SERVER_URL}conference_call/${req.params.confId}/status`)
    return res.json(response.data)
}))

router.post("/logCall", tryCatchWrapper(async(req, res) => {
    const callLog = CallLog(req.body);
    await callLog.save()
    return res.json(callLog);
}))

router.post("/fsmContext", tryCatchWrapper(async(req, res) => {
    const fsmContext = FsmContext(req.body);
    await fsmContext.save()
    return res.json(fsmContext);
}))

router.get("/fsmContext/:contextId", tryCatchWrapper(async (req, res) => {
    return res.json(
        await FsmContext.getContextById(req.params.contextId)
    )
}))

router.get("/logCall/:callId", tryCatchWrapper(async (req, res) => {
    return res.json(
        await CallLog.getCallLogById(req.params.callId)
    );
}))

module.exports = router;
