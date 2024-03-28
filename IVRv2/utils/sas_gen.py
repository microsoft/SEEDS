from azure.storage.blob import BlobServiceClient, BlobClient, ContainerClient, generate_blob_sas, BlobSasPermissions
import datetime
from urllib.parse import urlparse


class SASGen:
    def __init__(self, blob_conn_str: str):
        self.blob_service_client = BlobServiceClient.from_connection_string(blob_conn_str)
    
    def get_url_with_sas(self, url: str) -> str:
        parsed_url = urlparse(url)
        container_name = parsed_url.path.split('/')[1]
        blob_path = '/'.join(parsed_url.path.split('/')[2:])
        blob_client = self.blob_service_client.get_blob_client(container_name, blob_path)
        sas_token = generate_blob_sas(
                account_name=self.blob_service_client.account_name,
                account_key=self.blob_service_client.credential.account_key,
                container_name=container_name,
                blob_name=blob_path,
                permission=BlobSasPermissions(read=True),
                expiry=datetime.datetime.utcnow() + datetime.timedelta(hours=1)
            )
        return blob_client.url + "?" + sas_token