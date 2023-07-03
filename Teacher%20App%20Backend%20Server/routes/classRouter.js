"use strict";
const express = require("express");
const path = require("path");
const ClassRoom = require("../models/Class.js");
const { tryCatchWrapper } = require(path.join("..", "util.js"))

const router = express.Router()

router.get("/", tryCatchWrapper( async (req, res) => {
    return res.json(await ClassRoom.getClassesByTeacherId(req.userId));
}));

router.get("/:classId", tryCatchWrapper(async (req, res) => {
    return res.json(await ClassRoom.getClassById(req.params.classId))
}))

router.post("/", tryCatchWrapper( async (req, res) => {
    req.body.teacher = req.userId;
    var classRoom;

    if (req.body._id) {
        classRoom = await ClassRoom.getClassById(req.body._id);
        if(classRoom.teacher != req.userId) return res.json(403);
        ["name", "students", "leaders", "contentIds"].forEach(prop => classRoom[prop] = req.body[prop])
    } else {
        classRoom = new ClassRoom(req.body);
    }
    await classRoom.save()
    return res.json(classRoom)
}))

router.delete("/:classId", tryCatchWrapper(async (req, res) => {
    await ClassRoom.deleteClassById(req.params.classId);
    return res.sendStatus(200);
}))

module.exports = router;