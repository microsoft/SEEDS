var express = require('express');
const { GetPullModelInsights } = require('../../controllers/monoCall/monoCall');
const { tryCatchWrapper1, createMonoCallLog } = require("../../controllers/monoCall/utils");
var router = express.Router();

router.post("/",tryCatchWrapper1(async (req, res) => {
  const number = req.body.sender;
  console.log(`Call Request received with Number ${number}`);
  if(!global.monoCallInfo.hasOwnProperty(number)){
    await createMonoCallLog(number)
    global.communicationApi.createMonoCall(number)
  }
  else{
    return res.send("Call already in progress.");
  }
  return res.send("success.");
}));


router.post("/insights",tryCatchWrapper1(GetPullModelInsights));

// router.post("/test",async (req,res) => {
//   const pullModelUserSettings = require("../../models/pullModelUserSettings")
//   const phoneNumber = req.body.phoneNumber
//   const language = req.body.language
//   const speechRateIndex = req.body.speechRateIndex

//   const result = await pullModelUserSettings.findOneAndUpdate(
//     {
//       phoneNumber: phoneNumber,
//       "languageLevelSettings.language": language
//     },
//     {
//       $set: {
//         "languageLevelSettings.$.speechRateIndex": speechRateIndex
//       }
//     },
//     {
//       new: true
//     }
//   )
//   return res.json(result)
// })

// resource: https://stackoverflow.com/questions/25260818/rest-with-express-js-nested-router
// router.use('/quiz',quizRouter)

module.exports = router;