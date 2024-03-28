from motor.motor_asyncio import AsyncIOMotorClient
import uuid

class MongoDB:
    def __init__(self, conn_str: str, db_name: str, collection_name: str):
        client = AsyncIOMotorClient(conn_str)
        db = client[db_name]
        self.collection = db[collection_name]
    
    async def insert(self, doc: dict):
        if "_id" not in doc:
            doc["_id"] = str(uuid.uuid1())
        result = await self.collection.insert_one(doc)
        print(f"Single document inserted with _id: {result.inserted_id}")
    
    async def find_by_id(self, id: str) -> dict | None:
        doc = await self.collection.find_one({"_id": id})
        return doc

    async def find(self, query: dict) -> dict | None:
        doc = await self.collection.find_one(query)
        return doc
    
    async def update_document(self, id: str, new_doc: dict):
        if "_id" not in new_doc:
            new_doc["_id"] = id
        result = await self.collection.replace_one({"_id": id}, new_doc, upsert=True)
        if result.modified_count > 0:
            print(f"Document with _id: {id} replaced.")
        elif result.upserted_id is not None:
            print(f"New document inserted with _id: {result.upserted_id}.")
        else:
            print("No document was replaced.")