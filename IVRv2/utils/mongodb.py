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
 
