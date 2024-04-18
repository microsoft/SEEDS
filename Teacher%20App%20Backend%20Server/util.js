"use strict";
const LogEntry = require('./models/LogEntry'); // Importing the LogEntry model

module.exports.tryCatchWrapper = f => {
    return async function(req, res) {
        try {
            return await f.apply(this, arguments)
        } catch (error) {
            console.log(JSON.stringify(error, ["message", "arguments", "type", "name"]))
            console.log(error.stack)
            return res.status(400).json({ message: error.message, stack: error.stack })
        }
    }
}

module.exports.tryCatchWrapperLog = f => {
    return async function(req, res, next) {
        try {
            // We temporarily override the res.json method to capture the response body
            const originalJson = res.json.bind(res);
            res.json = (body) => {
                res.json = originalJson; // Restore original res.json function
                res.json(body); // Continue with sending the response

                // Create and save the log entry
                const logEntry = new LogEntry({
                    path: req.originalUrl,
                    method: req.method,
                    requestBody: req.body,
                    responseBody: body,
                    statusCode: res.statusCode,
                    timestamp: new Date()
                });
                logEntry.save().catch(err => console.error('Log could not be saved', err));
            };

            // Execute the original function and let the modified res.json handle the response
            await f(req, res, next);
        } catch (error) {
            console.error('Error:', error);
            // Log the error details
            const logEntry = new LogEntry({
                path: req.originalUrl,
                method: req.method,
                requestBody: req.body,
                responseBody: {
                    message: error.message,
                    stack: error.stack
                },
                statusCode: 400, // Assuming it always results in a 400 error
                timestamp: new Date()
            });
            logEntry.save().catch(err => console.error('Log could not be saved', err));

            // Send error response
            res.status(400).json({ message: error.message, stack: error.stack });
        }
    }
}

// module.exports = tryCatchWrapper;
