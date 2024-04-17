"use strict";
const express = require("express");
const { model } = require("mongoose");
const UserInfo = require("../models/UserInfo.js");
const path = require("path");
const { tryCatchWrapper } = require(path.join("..", "util.js"))

const router = express.Router()

const encryption_key = process.env.PHONE_NUMBER_ENCRYPTION_KEY

router.get("/participants", tryCatchWrapper(async (req, res) => {
    const result = await UserInfo.getAllUsers(encryption_key)
    return res.json(result)
}))

module.exports = router;
