const mongoose = require("mongoose");

const errorSchema = new mongoose.Schema({
    date:String,
    error:String
});

const clientRequestSchema = new mongoose.Schema({
    date:String,
    url: String,
    method:String,
    type: String,
    body: String,
});

const conferenceLogSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: true,
  },
  date:String,
  clientRequests: [clientRequestSchema],
  errors: [errorSchema],
}, { timestamps: true });

const conferenceLogModel = mongoose.model(process.env.CONFERENCE_LOG_COLLECTION_NAME, conferenceLogSchema);

module.exports = conferenceLogModel;
