# config.py

from pydantic import BaseSettings


class Settings(BaseSettings):
    VONAGE_API_KEY: str
    VONAGE_API_SECRET: str
    COSMOS_ENDPOINT: str
    COSMOS_KEY: str
    COSMOS_DATABASE: str
    COSMOS_CONTAINER: str

    class Config:
        env_file = ".env"


def get_settings():
    return Settings()
