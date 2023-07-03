const mongoose = require("mongoose");

const userSpentTimeForPullModelSchema = new mongoose.Schema({
    phoneNumber: {
        type: String,
        unique: true
    },
    timeInMilliSeconds: Number,
    date: String
}, { timestamps: true });

const userSpentTimeForPullModel = mongoose.model(process.env.USER_SPENT_TIME_FOR_PULLMODEL_COLLECTION, userSpentTimeForPullModelSchema);

module.exports = userSpentTimeForPullModel;
