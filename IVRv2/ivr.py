import vonage
from dotenv import load_dotenv
import os
from utils.sas_gen import SASGen
# from vonage.voice import Ncco

load_dotenv()

application_id = os.getenv("VONAGE_APPLICATION_ID")
api_secret = os.getenv("VONAGE_API_SECRET")
api_key = os.getenv("VONAGE_API_KEY")

sas_gen = SASGen(os.getenv("BLOB_STORE_CONN_STR"))
audio_url = sas_gen.get_url_with_sas("https://seedsblob.blob.core.windows.net/output-original/04573140-93e9-4edc-9efc-e9b21d3052f8.mp3")

client = vonage.Client(application_id=application_id, private_key=os.getenv("VONAGE_PRIVATE_KEY_PATH"))

# talk = Ncco.Talk(text='Hello from Vonage!', bargeIn=True, loop=5, premium=True)
# ncco = Ncco.build_ncco(record, connect, talk)
    

response = client.voice.create_call({
  'to': [{'type': 'phone', 'number': '919606612444'}],
  'from': {'type': 'phone', 'number': os.getenv("VONAGE_NUMBER")},
  'ncco': [
         {
            'action': 'talk', 
            'text': 'This is a text to speech call from Nexmo'
         },
         {
            'action': "stream",
            'streamUrl': ['https://contentmenu.blob.core.windows.net/menu/WelcomeToSeedsNinad.mp3'],
            # 'streamUrl': [audio_url],
            'loop': 1,
            'bargeIn': 'False',
        },
        {
            'action': 'input',
            'eventUrl': [os.getenv("NGROK_URL")+ '/input'],
            'type': ['dtmf'],
            'dtmf': {
                'maxDigits': 6,
                'submitOnHash': 'True',
                'timeOut': 1000
            }
        }
  ]
})





# client.voice.send_audio(response['uuid'],stream_url=[stream_url])
# client.voice.send_audio(response['uuid'],stream_url=[stream_url])
# client.voice.send_speech(response['uuid'], text='Hello from vonage')
# client.voice.stop_speech(response['uuid'])
# client.voice.send_dtmf(response['uuid'], digits='1234')


# record = Ncco.Record(eventUrl=['https://example.com'])
# talk = Ncco.Talk(text='Hello from Vonage!', bargeIn=True, loop=5, premium=True)


# ncco = Ncco.build_ncco(record, connect, talk)

# response = client.voice.create_call({
#     'to': [{'type': 'phone', 'number': TO_NUMBER}],
#     'from': {'type': 'phone', 'number': VONAGE_NUMBER},
#     'ncco': ncco
# })

# pprint(response)


# client.voice.update_call(response['uuid'], action='hangup')


# client = vonage.Client(key=api_key, secret=api_secret)
