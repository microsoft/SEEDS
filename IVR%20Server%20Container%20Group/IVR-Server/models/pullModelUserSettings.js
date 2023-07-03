const mongoose = require("mongoose");

const languageLevelSettingsSchema = new mongoose.Schema({
    language:{
        type: String,
        unique: true
    },
    speechRateIndex: Number
})

const pullModelUserSettingsSchema = new mongoose.Schema({
    phoneNumber: {
        type: String,
        unique: true
    },
    languageLevelSettings: [languageLevelSettingsSchema]
}, { timestamps: true });

const pullModelUserSettings = mongoose.model(process.env.PULL_MODEL_USER_SETTINGS_COLLECTION, pullModelUserSettingsSchema);

module.exports = pullModelUserSettings;
