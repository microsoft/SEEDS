import asyncio
import traceback
from fastapi import WebSocket, WebSocketDisconnect
from fastapi.websockets import WebSocketState
from azure.storage.blob.aio import BlobClient
from azure.identity.aio import DefaultAzureCredential
from urllib.parse import urlparse

class VanillaWebSocketService:
    def __init__(self, on_disconnect_callback: callable):
        self.on_disconnect_callback = on_disconnect_callback
        self.__play_event = asyncio.Event()
        self.__play_event.clear()  # Initially set to "paused mode"
        self.__is_sending = False  # Indicates whether audio is currently being sent
        self.__websocket = None
        self.__blob_client = None
    
    def set_websocket(self, websocket: WebSocket):
        self.__websocket = websocket
        if self.__check_connection():
            self.__play_event.set()
    
    async def close_websocket(self):
        if self.__websocket:
            await self.__websocket.close()

    async def play(self, blob_url: str = ""):
        """ Start sending audio through WebSocket to the connected client."""
        self.__play_event.set()  # Resume audio sending
        if not self.__is_sending and blob_url:
            self.__is_sending = True
            asyncio.create_task(self.__send_audio(blob_url))  # Start sending audio

    async def pause(self):
        """ Pause the audio sending. """
        self.__play_event.clear()  # Pause audio sending

    async def stop(self):
        """ Stop the audio sending. """
        self.__is_sending = False  # Stop the audio sending
        self.__play_event.set()  # Resume event so that it can cleanly exit
    
    async def __send_audio(self, blob_url: str):
        """ Send audio in chunks over WebSocket, fetching from Azure Blob Storage. """
        try:
            if not self.__websocket:
                print("WebSocket connection is not established")
                await self.on_disconnect_callback()

            # Initialize the BlobClient for the given blob URL
            await self.__initialize_blob_client(blob_url)

            # Read and send the blob data in chunks
            async for chunk in self.__read_blob_in_chunks(chunk_size=320):
                if not self.__is_sending:  # If stopped, exit the loop
                    break

                try:
                    await self.__send_chunk(chunk)
                except WebSocketDisconnect:
                    print("WebSocket is closed, triggering disconnect callback.")
                    self.__websocket = None  # Remove the WebSocket object being used
                    await self.pause()  # Pause sending data
                    if self.on_disconnect_callback:  # Call the disconnect callback if provided
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
        finally:
            if self.__blob_client:
                await self.__blob_client.close()
    
    def __check_connection(self):
        return self.__websocket is not None and self.__websocket.application_state == WebSocketState.CONNECTED

    async def __initialize_blob_client(self, blob_url: str):
        """ Initialize the BlobClient for fetching audio data. """
        # Parse the blob URL
        parsed_url = urlparse(blob_url)
        storage_account_url = f"https://{parsed_url.hostname}"
        path_parts = parsed_url.path.lstrip('/').split('/')
        
        # Extract the container name and blob name
        container_name = path_parts[0]
        blob_name = '/'.join(path_parts[1:])
        
        # Use DefaultAzureCredential to authenticate
        credential = DefaultAzureCredential()
        self.__blob_client = BlobClient(
            account_url=storage_account_url,
            container_name=container_name,
            blob_name=blob_name,
            credential=credential
        )

    async def __read_blob_in_chunks(self, chunk_size: int = 320):
        """ Reads the blob data in chunks asynchronously from Azure Blob Storage. """
        if not self.__blob_client:
            raise ValueError("BlobClient is not initialized")

        # Download the blob and read it in chunks
        download_stream = await self.__blob_client.download_blob()

        while True:
            await self.__play_event.wait()  # Wait until the event is set (play mode)
            chunk = await download_stream.read(chunk_size)
            if not chunk:  # End of the stream
                break
            yield chunk

    async def __send_chunk(self, chunk):
        """ Send chunk to websocket after checking connection """
        if chunk:
            await self.__play_event.wait()  # Wait until the event is set (play mode)
            if not self.__check_connection():
                raise WebSocketDisconnect("WebSocket has been closed")
            await self.__websocket.send_bytes(chunk)
