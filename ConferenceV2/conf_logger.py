import logging
import os
from pathlib import Path
import sys
from dotenv import load_dotenv
from opencensus.ext.azure.log_exporter import AzureLogHandler
from datetime import datetime

load_dotenv()

class ConferenceLogger:
    def __init__(self, version="1"):
        # Create a logger instance
        self.logger = logging.getLogger("ConferenceLogger")
        self.logger.setLevel(logging.DEBUG)  # Capture all log levels
        
        # Version number to be included in logs
        self.version = version
        
        environment = os.getenv('ENVIRONMENT', 'production')
        
        # Check if the app is running locally or in production
        if environment == 'production':
            # In production, send logs to Azure App Insights
            app_insights_conn_str = os.getenv('APPLICATIONINSIGHTS_CONNECTION_STRING')
            self.add_app_insights_handler(app_insights_conn_str)
        else:
            # Locally, print logs to both stdout and stderr
            self.add_console_handler()

    def add_app_insights_handler(self, connection_string):
        # Azure App Insights handler
        azure_handler = AzureLogHandler(connection_string=connection_string)
        azure_handler.setLevel(logging.DEBUG)  # Set log level for production
        self.logger.addHandler(azure_handler)

    def add_console_handler(self):
        # Handler to log to stdout (normal log output)
        stdout_handler = logging.StreamHandler(sys.stdout)
        stdout_handler.setLevel(logging.DEBUG)
        
        # Handler to log to stderr (for errors)
        stderr_handler = logging.StreamHandler(sys.stderr)
        stderr_handler.setLevel(logging.ERROR)

        self.logger.addHandler(stdout_handler)
        self.logger.addHandler(stderr_handler)

    def _format_message(self, *args):
            # Prepend timestamp and version number to each log
            timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            message = ' '.join(map(str, args))  # Concatenate all arguments into a single string
            return f"[{timestamp}] [Version: {self.version}] {message}"

    # Log level methods
    def debug(self, *args):
        self.logger.debug(self._format_message(*args))

    def info(self, *args):
        self.logger.info(self._format_message(*args))

    def warning(self, *args):
        self.logger.warning(self._format_message(*args))

    def error(self, *args):
        self.logger.error(self._format_message(*args))

    def critical(self, *args):
        self.logger.critical(self._format_message(*args))


version_file = Path("version.txt")
if version_file.exists():
    app_version = version_file.read_text().strip()
else:
    app_version = "Unknown"
    
logger_instance = ConferenceLogger(version=app_version)
