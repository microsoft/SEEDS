const { BlockBlobClient } = require("@azure/storage-blob");

//reference: https://learn.microsoft.com/en-us/azure/storage/blobs/storage-blob-account-delegation-sas-create-javascript?tabs=blob-client
const sasUrl = "https://seedsblob.blob.core.windows.net/input-container/testblob.js?sv=2021-10-04&st=2023-02-08T08%3A42%3A26Z&se=2023-02-08T09%3A42%3A26Z&sr=b&sp=rw&sig=L1%2FDlMbAtjxiTOr4eL3FsWV81MLT2KtkRXHJ%2FarORCs%3D"

const client = new BlockBlobClient(sasUrl)

setTimeout(async () => await client.uploadFile('./util.js'))
