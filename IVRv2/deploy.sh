APP_NAME="ivrv2 "  # Replace with your Azure App Service name
RESOURCE_GROUP="seeds"  # Replace with your Azure Resource Group name
ZIP_FILE="app.zip"

# Check if 'zip' is installed
if ! command -v zip &> /dev/null; then
    echo "'zip' command not found. Installing 'zip'..."
    sudo apt-get update
    sudo apt-get install -y zip
fi

# Remove any existing ZIP file
if [ -f $ZIP_FILE ]; then
    rm $ZIP_FILE
fi

# Create a ZIP file excluding unnecessary files and directories
zip -r $ZIP_FILE . -x "venv/*" -x "*.pyc" -x "__pycache__/*" -x "*.git/*" -x "*.DS_Store" -x "*.vscode/*" -x "*.env" -x "*.gitignore"
 
# Deploy the ZIP file to Azure App Service
az webapp deployment source config-zip --resource-group $RESOURCE_GROUP --name $APP_NAME --src $ZIP_FILE

# Clean up the ZIP file after deployment
rm $ZIP_FILE

echo "Deployment to $APP_NAME completed successfully!"
