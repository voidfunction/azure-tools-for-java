package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IStorageClient {
    List<BlobContainer> getBlobContainers(StorageAccount storageAccount)
            throws HDIException;

    BlobDirectory getRootDirectory(StorageAccount storageAccount, BlobContainer blobContainer)
            throws HDIException;

    List<BlobItem> getBlobItems(StorageAccount storageAccount, BlobDirectory blobDirectory)
            throws HDIException;

    BlobDirectory createBlobDirectory(StorageAccount storageAccount,
                                      BlobDirectory parentBlobDirectory,
                                      BlobDirectory blobDirectory)
            throws HDIException;

    BlobFile createBlobFile(StorageAccount storageAccount,
                            BlobDirectory parentBlobDirectory,
                            BlobFile blobFile)
            throws HDIException;

    void deleteBlobFile(StorageAccount storageAccount,
                        BlobFile blobFile)
            throws HDIException;

    void uploadBlobFileContent(StorageAccount storageAccount,
                               BlobContainer blobContainer,
                               String filePath,
                               InputStream content,
                               CallableSingleArg<Void, Long> processBlockEvent,
                               long maxBlockSize,
                               long length)
            throws HDIException;

    void downloadBlobFileContent(StorageAccount storageAccount,
                                 BlobFile blobFile,
                                 OutputStream content)
            throws HDIException;

}
