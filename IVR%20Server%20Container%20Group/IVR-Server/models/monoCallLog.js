const mongoose = require("mongoose");

const interactionSchema = new mongoose.Schema({
    pressedKey:String,
    means:String,
    timeStamp:String
});

const monoCallLogSchema = new mongoose.Schema({
  userPhoneNumber:String,
  createdDate:String,
  interactions: [interactionSchema],
  endDate:String
}, { timestamps: true });

const monoCallLogModel = mongoose.model(process.env.MONOCALL_LOG_COLLECTION_NAME, monoCallLogSchema);

module.exports = monoCallLogModel;
