"use strict";
const mongoose = require("mongoose");

const FsmContextSchema = new mongoose.Schema({
  fsmContextId: { type: String, required: true, index: true },
  phoneNumbers: [ String ]
});

var FsmContext = (module.exports = mongoose.model("FsmContext", FsmContextSchema));

module.exports.getContextById = fsmContextId => {
  return FsmContext.findOne({ fsmContextId }).exec();
}
