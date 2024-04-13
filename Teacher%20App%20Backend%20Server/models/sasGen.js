const { BlobServiceClient, StorageSharedKeyCredential, generateBlobSASQueryParameters, BlobSASPermissions } = require("@azure/storage-blob");
const { URL } = require('url');

class SASGen {
    constructor(blobConnStr) {
        this.blobServiceClient = BlobServiceClient.fromConnectionString(blobConnStr);
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
        sasPermissions.read = true;  // Setting read permissions for the SAS

        const expiresOn = new Date(new Date().valueOf() + 3600 * 1000); // 1 hour from now
        const sasOptions = {
            containerName,
            blobName: blobPath,
            permissions: sasPermissions.toString(),
            expiresOn: expiresOn
        };

        const sasToken = generateBlobSASQueryParameters(sasOptions, this.blobServiceClient.credential).toString();
        return `${blobClient.url}?${sasToken}`;
    }
}

module.exports = SASGen;
