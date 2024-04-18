// models/LogEntry.js
const mongoose = require('mongoose');

const logEntrySchema = new mongoose.Schema({
  path: String,
  method: String,
  requestBody: mongoose.Schema.Types.Mixed,
  responseBody: mongoose.Schema.Types.Mixed,
  statusCode: Number,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('LogEntry', logEntrySchema);
