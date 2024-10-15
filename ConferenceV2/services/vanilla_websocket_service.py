import asyncio
import traceback
import wave
from fastapi import WebSocket, WebSocketDisconnect
from fastapi.websockets import WebSocketState

class VanillaWebSocketService:
    def __init__(self, on_disconnect_callback: callable):
        self.on_disconnect_callback = on_disconnect_callback
        self.__play_event = asyncio.Event()
        self.__play_event.clear()  # Initially set to "paused mode", as websocket might not be connected
        self.__is_sending = False  # Indicates whether audio is currently being sent
        self.__websocket = None
    
    def set_websocket(self, websocket: WebSocket):
        self.__websocket = websocket
        if self.__check_connection():
            self.__play_event.set()
    
    async def close_websocket(self):
        if self.__websocket:
            await self.__websocket.close()

    async def send_audio(self, file_path: str):
        """ Send audio in chunks over WebSocket. """
        try:
            if not self.__websocket:
                raise ValueError("WebSocket connection is not established")

            for chunk in self.__read_wav_file_in_chunks(file_path, chunk_size=320):
                if not self.__is_sending:  # If stopped, exit the loop
                    break

                try:
                    await self.__send_chunk(chunk)
                except WebSocketDisconnect:
                    print("WebSocket is closed, triggering disconnect callback.")
                    self.__websocket = None # REMOVE THE WEBSOCKET OBJECT BEING USED
                    await self.pause() # PAUSE SENDING DATA
                    if self.on_disconnect_callback: # Call the disconnect callback if provided
                        print("CALLING RECONNECT CODE...")
                        await self.on_disconnect_callback()
                        # After callback, attempt to resend the failed chunk
                        await self.__send_chunk(chunk)

                except Exception as e:
                    print(f"Error sending audio chunk: {e}")
                    traceback.print_exc()
                    await self.stop()

                await asyncio.sleep(0.02)  # Wait for 20 milliseconds between chunks

            print("Audio sending completed or stopped")
            self.__is_sending = False

        except Exception as e:
            print(f"An error occurred in websocket service: {e}")
            traceback.print_exc()

    async def play(self, new_file_path: str = ""):
        """ Start sending audio through WebSocket to the connected client."""
        self.__play_event.set()  # Resume audio sending
        if not self.__is_sending and new_file_path:
            self.__is_sending = True
            asyncio.create_task(self.send_audio(new_file_path))  # Start sending audio

    async def pause(self):
        """ Pause the audio sending. """
        self.__play_event.clear()  # Pause audio sending

    async def stop(self):
        """ Stop the audio sending. """
        self.__is_sending = False  # Stop the audio sending
        self.__play_event.set()  # Resume event so that it can cleanly exit
    
    def __check_connection(self):
        return self.__websocket != None and self.__websocket.application_state == WebSocketState.CONNECTED
    
    def __read_wav_file_in_chunks(self, file_path: str, chunk_size: int = 320):
        """ Reads the WAV file in chunks. """
        with wave.open(file_path, 'rb') as wav_file:
            total_frames = wav_file.getnframes()
            frame_size = wav_file.getsampwidth()  # Sample width in bytes (e.g., 2 bytes for 16-bit audio)
            num_channels = wav_file.getnchannels()  # Number of audio channels
            total_byte_size = total_frames * frame_size * num_channels
            audio_data = wav_file.readframes(total_frames)

            for i in range(0, len(audio_data), chunk_size):
                yield audio_data[i:i + chunk_size]
    
    async def __send_chunk(self, chunk):
        """ Send chunk to websocket after checking connection """
        if chunk:
            await self.__play_event.wait() # Wait until the event is set (i.e., play mode)
            if not self.__check_connection():
                raise WebSocketDisconnect("WebSocket has been closed")
            await self.__websocket.send_bytes(chunk)
