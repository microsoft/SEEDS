const { BlobServiceClient, generateBlobSASQueryParameters, BlobSASPermissions } = require("@azure/storage-blob");
const { DefaultAzureCredential } = require("@azure/identity");
const { URL } = require('url');

class BlobService {
    constructor() {
        const credential = new DefaultAzureCredential();
        this.blobServiceClient = new BlobServiceClient(
            `https://seedsblob.blob.core.windows.net`, 
            credential
        );
    }

    getContainerClient(containerName){
        return this.blobServiceClient.getContainerClient(containerName)
    }

    getBlobServiceClient(){
        return this.blobServiceClient
    }

    async getUploadSASToken(blobname, containerName){
        const expiresOn = new Date(new Date().valueOf() + 3600 * 1000); // 1 hour from now
        const sasOptions = {
            containerName: containerName,
            blobName: blobname,
            startsOn: new Date(),
            expiresOn: expiresOn, //change this time duration later
            permissions: BlobSASPermissions.parse("rw"),
        };
        
        const userDelegationKey = await this.blobServiceClient.getUserDelegationKey(new Date(), expiresOn);
        const sasToken = generateBlobSASQueryParameters(sasOptions, userDelegationKey, this.blobServiceClient.accountName).toString();
        return sasToken
    }


    async getURLWithSAS(url) {
        const decodedUrl = decodeURIComponent(url);
        const parsedUrl = new URL(decodedUrl);
        const pathSegments = parsedUrl.pathname.split('/').filter(part => part.length > 0);
        const containerName = pathSegments[0];
        const blobPath = pathSegments.slice(1).join('/');

        // Get a blob client to interact with the blob
        const containerClient = this.blobServiceClient.getContainerClient(containerName);
        const blobClient = containerClient.getBlobClient(blobPath);

        const sasPermissions = new BlobSASPermissions();
        sasPermissions.read = true; // Setting read permissions for the SAS

        const expiresOn = new Date(new Date().valueOf() + 3600 * 1000); // 1 hour from now

        // Fetch a user delegation key
        const userDelegationKey = await this.blobServiceClient.getUserDelegationKey(new Date(), expiresOn);

        const sasOptions = {
            containerName,
            blobName: blobPath,
            permissions: sasPermissions.toString(),
            expiresOn: expiresOn,
            startsOn: new Date(new Date().valueOf() - 300 * 1000), // Optional: start time is 5 minutes before now
            userDelegationKey
        };

        const sasToken = generateBlobSASQueryParameters(sasOptions, userDelegationKey, this.blobServiceClient.accountName).toString();
        return `${blobClient.url}?${sasToken}`;
    }
}

module.exports = BlobService;
