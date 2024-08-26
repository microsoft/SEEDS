import os
from azure.identity import DefaultAzureCredential
from azure.cosmos import CosmosClient as cosmos_client
from azure.cosmos import PartitionKey, exceptions
from dotenv import load_dotenv
load_dotenv()

class MongoDB:
    def __init__(self, container_name, db_name = "ivr"):
        credential = DefaultAzureCredential()
        client = cosmos_client(os.environ.get("COSMOS_DB_EP", ""), credential)

        db = client.get_database_client(db_name)
        self.container = db.get_container_client(container_name)
    
    async def find_by_id(self, id_string: str) -> dict | None:
        query = f'SELECT * FROM c WHERE c.id="{id_string}"'
        response = self.container.query_items(query, enable_cross_partition_query=True)
        result = [item for item in response]
        return result[0] if len(result) > 0 else None
    
    async def find_one_by_query(self, query: dict):
        attrs_list = [f'c.{k}="{v}"' for k,v in query.items()]
        where_clause = ' AND '.join(attrs_list)
        response = self.container.query_items(f"SELECT * FROM c WHERE {where_clause}", enable_cross_partition_query=True)
        result = [item for item in response]
        return result[0] if len(result) > 0 else None
    
    async def find_all(self) -> list:
        query = f"SELECT * FROM c"
        response = self.container.query_items(query, enable_cross_partition_query=True)
        result = [item for item in response]
        return result
    
    async def query_items(self, query:str):
        response = self.container.query_items(query, enable_cross_partition_query=True)
        result = [item for item in response]

    async def insert(self, doc: dict):
        return self.container.upsert_item(doc)

    async def update_document(self, id: str, new_doc: dict):
        return await self.insert(new_doc)
    
    async def delete(self, id: str):
        return self.container.delete_item(item=id, partition_key=id)
    
    async def find_top_one(self, attr: str):
        query = f"""
            SELECT TOP 1 * 
            FROM c 
            ORDER BY c.{attr} DESC
        """
        items = list(self.container.query_items(
            query=query,
            enable_cross_partition_query=True
        ))
        return items[0] if items else None
    
    def get_container(self):
        return self.container
