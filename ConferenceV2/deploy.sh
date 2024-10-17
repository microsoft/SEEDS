#!/bin/bash

# Variables (update these according to your requirements)
WEBAPP_NAME="confv2"
RESOURCE_GROUP="seeds"
RUNTIME="PYTHON|3.10"
ZIP_FILE_NAME="$WEBAPP_NAME.zip"

# Ensure Azure CLI is logged in
echo "Checking Azure CLI login status..."
az account show > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Not logged in. Please log in to Azure CLI."
    az login --use-device-code
else
    echo "Already logged in."
fi

# Ignore .env but include private.key
echo "Preparing files for deployment..."
# Create a zip file while excluding files listed in .gitignore, and include private.key explicitly
zip -r $ZIP_FILE_NAME . -x "*.git*" -x "*venv*" -x "*.pyc" -x "*__pycache__*" -x ".env" -x "*.pytest_cache*"

# Print the directory structure of the deployment package
echo "Files to be deployed:"
unzip -l "$ZIP_FILE_NAME"

# Deploy the app using az webapp deploy
echo "Deploying the web app..."
az webapp deploy --resource-group $RESOURCE_GROUP --name $WEBAPP_NAME --src-path "$ZIP_FILE_NAME"

# Clean up the zip file after successful deployment
echo "Cleaning up the zip file..."
rm "$ZIP_FILE_NAME"

echo "Deployment completed!"
