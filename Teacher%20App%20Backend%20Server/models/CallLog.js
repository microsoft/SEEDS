"use strict";
const mongoose = require("mongoose");

const CallLogSchema = new mongoose.Schema({
  type: { type: String, required: true }, 
  time: { type: String, required: true },
  fsmContextId: { type: String, required: true, index: true },
  data: {type: mongoose.Schema.Types.Mixed},
  isCompleted: { type: Boolean, required: true }
});

var CallLog = (module.exports = mongoose.model("CallLog", CallLogSchema));

module.exports.getCallLogById = fsmContextId => {
  return CallLog.findOne({ fsmContextId }).exec();
}
