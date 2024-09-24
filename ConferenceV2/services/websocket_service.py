import asyncio
import io
from fastapi import WebSocket
from pydub import AudioSegment

class WebSocketService:
    def __init__(self, websocket_server_ep: str):
        self.websocket_server_ep = websocket_server_ep
        self.websocket = None
        self.pause_event = asyncio.Event()
        self.pause_event.set()  # Initially set to "play mode"
        self.is_sending = False  # Indicates whether audio is currently being sent

    def __convert_audio_to_pcm_16000(self, file_path):
        # Convert MP3 to PCM format at 16000 samples per second
        audio = AudioSegment.from_mp3(file_path)
        audio = audio.set_frame_rate(16000).set_channels(1)  # Mono
        pcm_audio = io.BytesIO()
        audio.export(pcm_audio, format="raw")
        return pcm_audio.getvalue()

    def set_websocket(self, websocket: WebSocket):
        self.websocket = websocket

    async def send_audio(self, file_path: str):
        try:
            if not self.websocket:
                raise ValueError("WEBSOCKET NOT CONNECTED TO SEND AUDIO STREAM")
            
            audio_bytes = self.__convert_audio_to_pcm_16000(file_path)
            chunk_size = 320  # Sending 320 bytes at a time

            for i in range(0, len(audio_bytes), chunk_size):
                await self.pause_event.wait()  # Wait until the event is set (i.e., play mode)

                if not self.is_sending:  # If stopped, exit the loop
                    break

                chunk = audio_bytes[i:i + chunk_size]
                await self.websocket.send_bytes(chunk)  # Send binary data over the WebSocket
                await asyncio.sleep(0.02)  # Wait for 20 milliseconds between chunks

            print("Audio sending completed or stopped")

        except Exception as e:
            print(f"An error occurred: {e}")

    async def play(self, file_path: str):
        if not self.is_sending:
            self.is_sending = True
            self.pause_event.set()  # Resume audio sending
            asyncio.create_task(self.send_audio(file_path))  # Start sending audio

    async def pause(self):
        self.pause_event.clear()  # Pause audio sending

    async def stop(self):
        self.is_sending = False  # Stop the audio sending
        self.pause_event.set()  # Resume event so that it can cleanly exit
