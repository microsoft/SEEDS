"use strict";
const morgan = require('morgan')
require("dotenv/config");

morgan.token("signootreqid", (req, res) => {
  if(req.headers['signootreqid']) return req.headers['signootreqid']
  return ""
})
  

module.exports = morgan