# main.py

from fastapi import FastAPI
from routers import conference, webhooks

app = FastAPI(title="Conference Call System")

app.include_router(conference.router, prefix="/conference", tags=["Conference"])
app.include_router(webhooks.router, tags=["Webhooks"])

@app.get("/")
async def root():
    return {"message": "Conference Call System API"}
