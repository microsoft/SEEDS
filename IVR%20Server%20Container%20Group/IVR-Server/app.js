const express = require("express");
const app = express();
const cors = require('cors') 
const bp = require("body-parser");
const morgan = require("morgan");
const rateLimit = require('express-rate-limit');

const monoCallRouter = require("./routes/monoCall/monoCall");
const handCricketRouter = require("./routes/handCricket");
const conferenceCallRouter = require("./routes/Conference/conferenceCall");
const handler = require("./websockets/AndroidAppClient");

// This function will just show the Client Ip Address. we can modify it to allow only few Ip Addresses to this IVR server
const showIp = (req,res,next) => {
    console.log("Client Address...")
    console.log(req.originalUrl)
    console.log(req.socket.remoteAddress);
    next()
  }
  
  // app.use(showIp)
  
app.get("/healthCheck",(req,res) => {
  res.send("alive").status(200)
})

// Define the rate limiter options
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes - The time window for which requests are counted.
  max: 2000, // 100 requests - The maximum number of requests per IP within the time window.
});

app.use(limiter);
app.use("/azurepubsubhook", handler.getMiddleware());

app.use("/vonage",morgan("tiny"),express.json(),global.communicationApi.router)

app.use(
  "/conference_call",
  morgan("tiny"),
  express.json(),
  conferenceCallRouter
);

app.use(
  "/mono_call",
  cors(),
  morgan("tiny"),
  bp.urlencoded({ extended: true }),
  express.json(),
  monoCallRouter
);

app.use("/handCricket",
  morgan("tiny"),
  express.json(),
  handCricketRouter
)

module.exports = app