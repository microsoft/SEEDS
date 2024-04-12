"use strict";
const express = require("express");
const mongoose = require("mongoose");
const path = require("path");
const cors = require('cors') 
const bodyParser = require("body-parser");
const rateLimit = require('express-rate-limit');

const morgan = require(path.join(__dirname,"morganConfig.js"))
const dotenv = require("dotenv/config");
const verifyToken = require("./auth/verifyToken.js")
const callRouter = require("./routes/callRouter.js")
const teacherRouter = require("./routes/teacherRouter.js")
const contentRouter = require("./routes/contentRouter")
const classRoomRouter = require("./routes/classRouter.js");
const logRouter = require("./routes/logRouter.js")
const { constants } = require("zlib");


const app = express();

// Define the rate limiter options
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes - The time window for which requests are counted.
  max: 100, // 100 requests - The maximum number of requests per IP within the time window.
});

// Apply the rate limiter middleware to all requests
app.use(morgan('dev'))
// app.use(limiter);
app.use(cors())
app.use("/call", verifyToken, bodyParser.json(), callRouter);
app.use("/teacher", verifyToken, bodyParser.json(), teacherRouter);
app.use("/content", verifyToken, bodyParser.json(), contentRouter);
app.use("/class", verifyToken, bodyParser.json(), classRoomRouter);
app.use("/log", verifyToken, bodyParser.json(), logRouter)

mongoose.connect(process.env.DB_CONNECTION, () => {
    console.log("Connected to DB")
    const PORT = process.env.PORT || 4000
    app.listen(PORT, () => {
      console.log(`server running on port ${PORT}`)
    });
  }
);
