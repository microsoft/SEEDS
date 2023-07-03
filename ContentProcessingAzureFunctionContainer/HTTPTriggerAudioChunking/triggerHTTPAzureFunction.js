const axios = require('axios');
const { pullModelDialogData } = require("./pull_model_menu_data")

function createOptions(url,data){
    let options = {
        method: 'POST',
        baseURL: "https://seedscontent.azurewebsites.net/api/",
        url: url,
        headers: {
        'Content-type': 'application/json',
        },
        data: data,
    };
return options
}

const url = "acs"


async function populateAllMessagesForPullModel(){
    const toLanguages = ["english","kannada","bengali"]
    const pullModelDialogDataLength = pullModelDialogData.length
    console.log(`Total Number of Messages = ${pullModelDialogDataLength}`)
    for(let i=0;i<pullModelDialogDataLength;i+=1){
        const dialog_data = pullModelDialogData[i]
        const data = {
            type:"translate-tts-multi-lang",
            text:dialog_data["text"],
            toLanguages:toLanguages,
            url:"/translate",
            baseFolder:dialog_data["baseFolder"],
            folder:dialog_data['folder']
        }
        const options = createOptions(url,data)
        try{
            await axios(options)
            console.log(`Message with Number ${i+1} is Done`)
        }
        catch(error){
            console.log(`------------Message with Number ${i+1} is Failed!!!----------------`)
            console.log(`Payload = ${JSON.stringify(data)}`)
            console.log(error)
        }
    }
}

// Important Note!!! : For language dialog, you have to populate audio only for that language but not all
populateAllMessagesForPullModel()