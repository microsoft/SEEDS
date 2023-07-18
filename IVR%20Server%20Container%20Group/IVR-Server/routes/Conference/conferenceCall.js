var express = require("express");
var router = express.Router();
const { v4: uuidv4 } = require("uuid");
const { WebPubSubServiceClient } = require("@azure/web-pubsub");
const {
  createConferenceObject,
  logClientRequest,
  setTeacherNumberInConference,
  setTeacherNameInConference,
} = require("../../controllers/Conference/conferenceCall");
const Conference = require("../../models/conference");
const { tryCatchWrapperForReqResModel } = require("../../controllers/Conference/utils");
const { validateConferenceCallRequest, validateMobileNumbers, validateNames, removeContactsExistInAnotherConference, removeDuplicateMobileNumbers } = require("../../middleware/conferenceCall");

const serviceClient = new WebPubSubServiceClient(
  process.env.PUBSUB_CONNECTION_STRING,
  process.env.AZURE_PUBSUB_HUB_NAME
);


router.post("/",
  validateConferenceCallRequest,
  validateMobileNumbers,
  validateNames,
  removeDuplicateMobileNumbers,
  removeContactsExistInAnotherConference,
  tryCatchWrapperForReqResModel(async (req, res) => {
  const confId = req.body.confId;
  // const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  // await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(req.body))
  const numbers = req.body.phoneNumbers;
  const names = req.body.names?req.body.names:[]
  setTeacherNumberInConference(confId,numbers[0])
  setTeacherNameInConference(confId,names[0])
  await createConferenceObject(confId, numbers, names);
  global.communicationApi.makeConference(confId,numbers)
  res.send("success.");
}));

router.get("/accessToken", tryCatchWrapperForReqResModel(async (req, res) => {
  const confId = uuidv4();
  const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP","")
  const token = await serviceClient.getClientAccessToken({ userId: confId });
  res.json({ confId: confId, accessToken: token.url });
}));

router.get("/:confId/status", tryCatchWrapperForReqResModel(async (req, res) => {
  const confId = req.params.confId;
  const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
  await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP","")
  const conference = await Conference.findById(confId);
  res.json(conference);
}));

module.exports = router;
