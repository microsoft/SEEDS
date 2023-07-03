const express = require("express");
const app = express();
const cors = require('cors') 
const bp = require("body-parser");
const morgan = require("morgan");

const monoCallRouter = require("./routes/monoCall/monoCall");
const handCricketRouter = require("./routes/handCricket");
const conferenceCallRouter = require("./routes/Conference/conferenceCall");
const handler = require("./websockets/AndroidAppClient");


const showIp = (req,res,next) => {
    console.log("Client Address...")
    console.log(req.originalUrl)
    console.log(req.socket.remoteAddress);
    next()
  }
  
  // app.use(showIp)
  
  
  // app.post("/temp",(req,res) => {
  //   tryCatchWrapperForCustomizedFunction(doSomething)("mani","suresh")
  //   res.send("received from temp end point.")
  // })

app.get("/healthCheck",(req,res) => {
  res.send("alive").status(200)
})

app.use("/azurepubsubhook", handler.getMiddleware());

app.use("/vonage",morgan("tiny"),express.json(),communicationApi.router)

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