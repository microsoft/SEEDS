var express = require('express');
var router = express.Router();
const { v4: uuidv4 } = require("uuid");
const { mapPhoneNumbersToGameId, createNewHandCricketGameObject } = require('../controllers/handCricket');

router.get("/", (req, res) => {
    const numbers = [req.query.user1.trim(),req.query.user2.trim()];
    const gameId = uuidv4()
    mapPhoneNumbersToGameId(gameId,numbers)
    createNewHandCricketGameObject(gameId,numbers)
    global.communicationApi.createHandCricketGame(gameId,numbers)
    res.send("Game is going to start.");
});


module.exports = router