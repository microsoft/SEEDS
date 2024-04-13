"use strict";
const mongoose = require("mongoose");

const ContentSchema = new mongoose.Schema({
  _id: { type: String, default: () => require('uuid').v4() }, // Set default UUID generation
  title: { type: String, required: true, index: true, unique: true },
  description: { type: String, default: "" },
  id: { type: String, required: true, index: true, unique: true },
  type: { type: String, required: true},
  language: { type: String, required: true},
  isPullModel: {type: Boolean, default:false },
  isTeacherApp: {type: Boolean, default:false },
  isProcessed: {type: Boolean, default:false },
  isDeleted: {type: Boolean, default:false },
  titleAudio: { type: String, default: "" },
  localTitle: { type: String, default: ""},
  theme: { type: String, default: ""},
  localTheme: { type: String, default: ""},
  themeAudio: { type: String, default: ""},
  creation_time: { type: Number, default: -1 }
}, { collection: 'contentsV2' });

var Content = (module.exports = mongoose.model("Content", ContentSchema));

module.exports.getContent = () => {
    return Content.find().sort({_id: -1}).exec()
}

module.exports.getContentsByIds = ids => {
    return Content.find({ id: { $in: ids } }).exec()
}

module.exports.getContentById = id => {
  return Content.findOne({ id }).exec()
}