# main.py

from pathlib import Path
from fastapi import FastAPI
from routers import conference, webhooks, websocket
from fastapi.middleware.cors import CORSMiddleware

# Read the version from version.txt
version_file = Path("version.txt")
if version_file.exists():
    app_version = version_file.read_text().strip()
else:
    app_version = "Unknown"

app = FastAPI(title=f"SEEDS Conference Call System")

# Store the original OpenAPI function
original_openapi = app.openapi

# Customize the OpenAPI docs to display the version
def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    openapi_schema = original_openapi()  # Call the original OpenAPI function
    openapi_schema["info"]["version"] = app_version  # Set version in the docs
    app.openapi_schema = openapi_schema
    return app.openapi_schema

# Override the default OpenAPI method with the custom one
app.openapi = custom_openapi

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Adjust as needed
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(conference.router, prefix="/conference", tags=["Conference"])
app.include_router(webhooks.router, prefix="/webhooks",  tags=["Webhooks"])
app.include_router(websocket.router, prefix="/websocket", tags=["Websocket for Comm API"])

# SAVE LOGS TO TXT FILE: uvicorn main:app 2>&1 | tee logs.txt
