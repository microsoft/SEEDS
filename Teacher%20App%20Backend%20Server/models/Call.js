"use strict";
const mongoose = require("mongoose");

const CallSchema = new mongoose.Schema({
  id: { type: Number, required: true, index: true, unique: true },
  index: { type: Number, required: true }
});

var Call = (module.exports = mongoose.model("Call", CallSchema));
