const mongoose = require("mongoose");

const monoCallUserSchema = new mongoose.Schema(
  { _id: String },
  { strict: false }
);

const monoCallUserModel = mongoose.model(
  process.env.MONO_CALL_USER_COLLECTION_NAME,
  monoCallUserSchema
);

module.exports = monoCallUserModel;
