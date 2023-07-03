"use strict";
const mongoose = require("mongoose");

const ClassRoomSchema = new mongoose.Schema({
  name: { type: String, required: true }, 
  teacher: { type: String, required: true },
  students: [ String ],
  leaders: [ String ],
  contentIds: [ String ]
});

var ClassRoom = (module.exports = mongoose.model("Class", ClassRoomSchema));

module.exports.getClassById = _id => {
  return ClassRoom.findOne({ _id: mongoose.Types.ObjectId(_id) }).exec();
}

module.exports.getClassesByTeacherId = teacher => {
  return ClassRoom.find( {teacher} ).exec()
}

module.exports.deleteClassById = id => {
  return ClassRoom.deleteOne({_id: mongoose.Types.ObjectId(id)}).exec()
}