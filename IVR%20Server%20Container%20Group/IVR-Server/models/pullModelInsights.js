const mongoose = require("mongoose");

const audioContentDurationSchema = new mongoose.Schema({
    contentName: String,
    duration: Number
})

const userInsightsSchema = new mongoose.Schema({
    phoneNumber: {
        type: String,
        unique: true
    },
    callDuration: Number,
    audioContentDurations: [audioContentDurationSchema]
})

const pullModelInsightsSchema = new mongoose.Schema({
    date: {
        type: String,
        unique: true
    },
    userInsights: [userInsightsSchema]
    
}, { timestamps: true });

const pullModelInsights = mongoose.model(process.env.PULL_MODEL_INSIGHTS_COLLECTION, pullModelInsightsSchema);

module.exports = pullModelInsights;
