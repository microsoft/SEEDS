const mongoose = require("mongoose");

const warningSchema = new mongoose.Schema({
    name: String,
    message: String,
    stack: String
}, { timestamps: true });

const warningModel = mongoose.model(process.env.WARNING_COLLECTION, warningSchema);

module.exports = warningModel;
  