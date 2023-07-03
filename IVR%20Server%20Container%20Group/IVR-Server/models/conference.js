const mongoose = require("mongoose");

const participantSchema = new mongoose.Schema({
  phoneNumber: {
    type: String,
    required: true,
  },
  name: String,
  status: String,
  isMuted: Boolean,
  raiseHand: Boolean,
  uuid: String,
});

const audioSchema = new mongoose.Schema({
  id:String,
  state:String
})

const conferenceSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: true,
  },
  participants: [participantSchema],
  audio:audioSchema,
  leaderPhoneNumber:String,
  isEnded: Boolean
}, { timestamps: true });
const conferenceModel = mongoose.model(process.env.CONFERENCE_COLLECTION_NAME, conferenceSchema);

module.exports = conferenceModel;
