"use strict";
const express = require("express");
const path = require("path");
const Log = require(path.join("..", "models", "Log.js"))
const { tryCatchWrapper } = require(path.join("..", "util.js"))

const router = express.Router();

//req.body will be an array of log objects
router.post('/', tryCatchWrapper( async (req, res) => {
    await Log.insertMany(req.body)
    return res.sendStatus(200)
}))

router.get("/:userId", tryCatchWrapper(async (req, res) => {
    return res.json(await Log.getLogsByUserId(req.params.userId));
}))

module.exports = router;