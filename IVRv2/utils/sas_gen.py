import time
from azure.identity import DefaultAzureCredential
from azure.storage.blob import BlobServiceClient, generate_blob_sas, BlobSasPermissions
from urllib.parse import urlparse, unquote
import datetime

class SASGen:
    def __init__(self):
        self.credential = DefaultAzureCredential()
        self.blob_service_client = None
        self.user_delegation_key = None
        self.key_expiry_time = None

    def get_blob_service_client(self, url):
        if not self.blob_service_client:
            parsed_url = urlparse(url)
            self.blob_service_client = BlobServiceClient(
                account_url=f"{parsed_url.scheme}://{parsed_url.netloc}",
                credential=self.credential)
        return self.blob_service_client

    def get_user_delegation_key(self, blob_service_client):
        current_time = datetime.datetime.utcnow()
        if not self.user_delegation_key or current_time >= self.key_expiry_time:
            self.key_expiry_time = current_time + datetime.timedelta(hours=1)
            self.user_delegation_key = blob_service_client.get_user_delegation_key(current_time, self.key_expiry_time)
        return self.user_delegation_key

    def get_url_with_sas(self, url: str) -> str:
        print('GENERATING SAS FOR URL: ', url)
        start = time.time()
        decoded_url = unquote(url)
        parsed_url = urlparse(decoded_url)
        container_name = parsed_url.path.split('/')[1]
        blob_path = '/'.join(parsed_url.path.split('/')[2:])
        
        blob_service_client = self.get_blob_service_client(url)
        blob_client = blob_service_client.get_blob_client(container_name, blob_path)
        
        user_delegation_key = self.get_user_delegation_key(blob_service_client)
        
        # Generate SAS token using the user delegation key
        sas_token = generate_blob_sas(
            account_name=blob_service_client.account_name,
            container_name=container_name,
            blob_name=blob_path,
            permission=BlobSasPermissions(read=True),
            expiry=self.key_expiry_time,
            user_delegation_key=user_delegation_key,
        )
        print("TIME TAKEN: ", time.time() - start)
        return blob_client.url + "?" + sas_token