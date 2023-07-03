"use strict";
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