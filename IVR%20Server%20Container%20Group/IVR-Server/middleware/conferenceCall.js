const { logClientRequest } = require("../controllers/Conference/conferenceCall")

const validateConferenceCallRequest = async (req,res,next) => {
    const confId = req.body.confId;
    const indianTime = new Date().toLocaleString("en-Us", {timeZone: 'Asia/Kolkata'});
    await logClientRequest(confId,indianTime,req.originalUrl,req.method,"HTTP",JSON.stringify(req.body))
    const phoneNumbers = req.body.phoneNumbers
    const names = req.body.names

    if(
        typeof confId === "string" && 
        (confId.trim().length > 0) && 
        Array.isArray(phoneNumbers) && 
        Array.isArray(names) && 
        (phoneNumbers.length > 0) &&
        phoneNumbers.length === names.length
    ){
        next()
    }
    else{
        return res.status(400).json({message:'Invalid Payload'})
    }
}

const validateMobileNumbers = (req,res,next) => {
    const phoneNumbers = req.body.phoneNumbers
    for(const phoneNumber of phoneNumbers){
        if(typeof phoneNumber !== "string"){
            return res.status(400).json({message:"PhoneNumber Type must be string"})
        }
        else if(phoneNumber.startsWith("+")){
            return res.status(400).json({message:"PhoneNumber must not start with +"})
        }
        else if(!phoneNumber.startsWith("91")){
            return res.status(400).json({message:"PhoneNumber must start with 91"})
        }
        else if(phoneNumber.length !== 12){
            return res.status(400).json({message:"PhoneNumber length must be 12 including country code 91"})
        }
    }
    next()
}

const validateNames = (req,res,next) => {
    const names = req.body.names
    for(const name of names){
        if(typeof name !== 'string'){
            return res.status(400).json({message:"Name Type must be string"})
        }
        else if(name.trim().length === 0){
            return res.status(400).json({message:"Name must not be empty"})
        }
    }
    next()
}

const removeDuplicateMobileNumbers = (req,res,next) => {
    const phoneNumberSet = new Set([])
    const filteredPhoneNumbers = []
    const filteredNames = []
    const length = req.body.phoneNumbers.length

    for(let i=0;i<length;i++){
        const phoneNumber = req.body.phoneNumbers[i]
        if(!phoneNumberSet.has(phoneNumber)){
            phoneNumberSet.add(phoneNumber)
            filteredPhoneNumbers.push(phoneNumber)
            filteredNames.push(req.body.names[i])
        }
    }
    req.body.phoneNumbers = filteredPhoneNumbers
    req.body.names = filteredNames
    next()
}

const removeContactsExistInAnotherConference = (req,res,next) => {
    const phoneNumbers = []
    const names = []
    const length = req.body.phoneNumbers.length
    if(length > 100){
        return res.status(400).json({message:"Number of Contacts Limit Exceeded"})
    }
    else{
        for(let i=0;i<length;i++){
            const phoneNumber = req.body.phoneNumbers[i]
            if(!global.phoneNumberToConfId.hasOwnProperty(phoneNumber)){
                phoneNumbers.push(phoneNumber)
                names.push(req.body.names[i])
            }
        }
        if(phoneNumbers.length === 0){
            return res.status(400).json({message:"All contacts exist in Other Conferences"})
        }
        req.body.phoneNumbers = phoneNumbers
        req.body.names = names
        next()
    }
}

module.exports = {
    validateConferenceCallRequest,
    validateNames,
    validateMobileNumbers,
    removeDuplicateMobileNumbers,
    removeContactsExistInAnotherConference,
}
