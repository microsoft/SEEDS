const mongoose = require("mongoose");

const unhandledExceptionSchema = new mongoose.Schema({
    error: String,
    origin: String
}, { timestamps: true });

const unhandledExceptionModel = mongoose.model(process.env.UNHANDLED_EXCEPTION_COLLECTION, unhandledExceptionSchema);

module.exports = unhandledExceptionModel;
  