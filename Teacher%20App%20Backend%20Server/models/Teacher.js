"use strict";
const mongoose = require("mongoose");

const TeacherSchema = new mongoose.Schema({
  email: { type: String, required: true, index: true, unique: true },
  students: [ String ]
});

var Teacher = (module.exports = mongoose.model("Teacher", TeacherSchema));

module.exports.getTeacherById = email => {
    return Teacher.findOne (
        { email }
    ).exec()
}

module.exports.setStudentsByTeacherId = (email, students) => {
    return Teacher.findOneAndUpdate (
        { email },
        { $set: { students } },
        { new : true }
    ).exec()
}
