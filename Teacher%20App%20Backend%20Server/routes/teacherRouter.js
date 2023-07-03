"use strict";
const express = require("express");
const path = require("path");
const Teacher = require("../models/Teacher.js");
const { tryCatchWrapper } = require(path.join("..", "util.js"))

const router = express.Router()

router.get("/register", tryCatchWrapper(async (req, res) => {
    var teacher = await Teacher.getTeacherById(req.userId);
    if(!teacher) {
        teacher = new Teacher( {email: req.userId, students: [] } )
        await teacher.save()
    }
    return res.json(teacher)
}))

router.get("/students", tryCatchWrapper (async (req, res) => {
    var teacher = await Teacher.getTeacherById(req.userId);
    if(!teacher)  return res.sendStatus(404);
    return res.json(teacher.students)
}))

router.post("/students", tryCatchWrapper(async (req, res) => {
    var teacher = await Teacher.setStudentsByTeacherId(req.userId, req.body.students);
    return res.json(teacher.students);
}))

module.exports = router
