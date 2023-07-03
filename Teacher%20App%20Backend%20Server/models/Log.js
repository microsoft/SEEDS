"use strict";
const mongoose = require("mongoose");

const LogSchema = new mongoose.Schema({
  id: { type: Number, requred: true, index: true },
  user: { type: String, required: true, index: true },
  logText : { type: String, required: true, index: true },
  time: { type: String, required: true, index: true },
  priority: { type: Number, required: true }
});

const Log =  (module.exports = mongoose.model("logs", LogSchema));

module.exports.getLogsByUserId = async id => {
  return Log.find(
    { user: id },
  ).exec()
}