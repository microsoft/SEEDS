# from azure.storage.blob import BlobServiceClient, BlobClient, ContainerClient, generate_blob_sas, BlobSasPermissions
# import datetime
# from urllib.parse import urlparse


# class SASGen:
#     def __init__(self, blob_conn_str: str):
#         self.blob_service_client = BlobServiceClient.from_connection_string(blob_conn_str)
    
#     def get_url_with_sas(self, url: str) -> str:
#         parsed_url = urlparse(url)
#         container_name = parsed_url.path.split('/')[1]
#         blob_path = '/'.join(parsed_url.path.split('/')[2:])
#         blob_client = self.blob_service_client.get_blob_client(container_name, blob_path)
#         sas_token = generate_blob_sas(
#                 account_name=self.blob_service_client.account_name,
#                 account_key=self.blob_service_client.credential.account_key,
#                 container_name=container_name,
#                 blob_name=blob_path,
#                 permission=BlobSasPermissions(read=True),
#                 expiry=datetime.datetime.utcnow() + datetime.timedelta(hours=1)
#             )
#         return blob_client.url + "?" + sas_token
from azure.identity import DefaultAzureCredential
from azure.storage.blob import BlobServiceClient, generate_blob_sas, BlobSasPermissions
from urllib.parse import urlparse, unquote
import datetime

class SASGen:
    def __init__(self):
        self.credential = DefaultAzureCredential()
        
    def get_url_with_sas(self, url: str) -> str:
        decoded_url = unquote(url)  # Decode the URL
        parsed_url = urlparse(decoded_url)
        container_name = parsed_url.path.split('/')[1]
        blob_path = '/'.join(parsed_url.path.split('/')[2:])
        
        blob_service_client = BlobServiceClient(account_url=f"{parsed_url.scheme}://{parsed_url.netloc}", 
                                                     credential=self.credential)
        
        blob_client = blob_service_client.get_blob_client(container_name, blob_path)
        # Get user delegation key
        key_start_time = datetime.datetime.utcnow()
        key_expiry_time = key_start_time + datetime.timedelta(hours=1)
        user_delegation_key = blob_service_client.get_user_delegation_key(key_start_time, key_expiry_time)

        # Generate SAS token using the user delegation key
        sas_token = generate_blob_sas(
            account_name=blob_service_client.account_name,
            container_name=container_name,
            blob_name=blob_path,
            permission=BlobSasPermissions(read=True),
            expiry=key_expiry_time,
            user_delegation_key=user_delegation_key,
        )
        return blob_client.url + "?" + sas_token

