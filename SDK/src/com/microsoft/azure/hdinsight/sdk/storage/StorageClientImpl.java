package com.microsoft.azure.hdinsight.sdk.storage;

import com.google.common.base.Strings;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.core.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.*;

public class StorageClientImpl implements IStorageClient {
    // Singleton Instance
    private static StorageClientImpl instance = null;

    public static StorageClientImpl getInstance() {
        if(instance == null){
            synchronized (StorageClientImpl.class){
                if(instance == null){
                    instance = new StorageClientImpl();
                }
            }
        }

        return instance;
    }

    private StorageClientImpl(){}

    public List<BlobContainer> getBlobContainers(StorageAccount storageAccount)
            throws HDIException {

        try {
            List<BlobContainer> bcList = new ArrayList<>();
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            Iterable<CloudBlobContainer> temp = client.listContainers(null, ContainerListingDetails.ALL, null, null);
            for (CloudBlobContainer container : temp) {
                String uri = container.getUri() != null ? container.getUri().toString() : "";
                String eTag = "";
                Calendar lastModified = new GregorianCalendar();
                BlobContainerProperties properties = container.getProperties();

                if (properties != null) {
                    eTag = Strings.nullToEmpty(properties.getEtag());

                    if (properties.getLastModified() != null) {
                        lastModified.setTime(properties.getLastModified());
                    }
                }

                String publicReadAccessType = "";
                BlobContainerPermissions blobContainerPermissions = container.downloadPermissions();

                if (blobContainerPermissions != null && blobContainerPermissions.getPublicAccess() != null) {
                    publicReadAccessType = blobContainerPermissions.getPublicAccess().toString();
                }

                bcList.add(new BlobContainer(container.getName(),
                        uri,
                        eTag,
                        lastModified,
                        publicReadAccessType));
            }

            return bcList;
        } catch (Throwable t) {
            throw new HDIException("Error retrieving the Blob Container list", t);
        }
    }

    public BlobDirectory getRootDirectory(StorageAccount storageAccount, BlobContainer blobContainer)
            throws HDIException{
        try {
            CloudBlobClient t = getCloudBlobClient(storageAccount);
            CloudBlobContainer container = t.getContainerReference(blobContainer.getName());
            CloudBlobDirectory directory = container.getDirectoryReference("");
            String uri = directory.getUri() != null?directory.getUri().toString():"";
            return new BlobDirectory("", uri, blobContainer.getName(), "");
        } catch (Throwable T) {
            throw new HDIException("Error retrieving the root Blob Directory", T);
        }
    }

    public List<BlobItem> getBlobItems(StorageAccount storageAccount, BlobDirectory blobDirectory)
            throws HDIException {
        ArrayList biList = new ArrayList();
        try {
            CloudBlobClient t = getCloudBlobClient(storageAccount);
            String containerName = blobDirectory.getContainerName();
            String delimiter = t.getDirectoryDelimiter();
            CloudBlobContainer container = t.getContainerReference(containerName);
            CloudBlobDirectory directory = container.getDirectoryReference(blobDirectory.getPath());
            Iterator i$ = directory.listBlobs().iterator();

            while(i$.hasNext()) {
                ListBlobItem item = (ListBlobItem)i$.next();
                String uri = item.getUri() != null?item.getUri().toString():"";
                String name;
                String path;
                if(item instanceof CloudBlobDirectory) {
                    CloudBlobDirectory blob = (CloudBlobDirectory)item;
                    name = extractBlobItemName(blob.getPrefix(), delimiter);
                    path = Strings.nullToEmpty(blob.getPrefix());
                    biList.add(new BlobDirectory(name, uri, containerName, path));
                } else if(item instanceof CloudBlob) {
                    CloudBlob blob1 = (CloudBlob)item;
                    name = extractBlobItemName(blob1.getName(), delimiter);
                    path = Strings.nullToEmpty(blob1.getName());
                    String type = "";
                    String cacheControlHeader = "";
                    String contentEncoding = "";
                    String contentLanguage = "";
                    String contentType = "";
                    String contentMD5Header = "";
                    String eTag = "";
                    GregorianCalendar lastModified = new GregorianCalendar();
                    long size = 0L;
                    BlobProperties properties = blob1.getProperties();
                    if(properties != null) {
                        if(properties.getBlobType() != null) {
                            type = properties.getBlobType().toString();
                        }

                        cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
                        contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
                        contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
                        contentType = Strings.nullToEmpty(properties.getContentType());
                        contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
                        eTag = Strings.nullToEmpty(properties.getEtag());
                        if(properties.getLastModified() != null) {
                            lastModified.setTime(properties.getLastModified());
                        }

                        size = properties.getLength();
                    }

                    biList.add(new BlobFile(name, uri, containerName, path, type, cacheControlHeader, contentEncoding,
                            contentLanguage, contentType, contentMD5Header, eTag, lastModified, size));
                }
            }

            return biList;
        } catch (Throwable T) {
            throw new HDIException("Error retrieving the Blob Item list", T);
        }
    }

   public BlobDirectory createBlobDirectory(StorageAccount storageAccount,
                                      BlobDirectory parentBlobDirectory,
                                      BlobDirectory blobDirectory)
            throws HDIException{
       try {
           CloudBlobClient t = getCloudBlobClient(storageAccount);
           String containerName = parentBlobDirectory.getContainerName();
           CloudBlobContainer container = t.getContainerReference(containerName);
           CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());
           CloudBlobDirectory directory = parentDirectory.getDirectoryReference(blobDirectory.getName());
           String uri = directory.getUri() != null?directory.getUri().toString():"";
           String path = Strings.nullToEmpty(directory.getPrefix());
           blobDirectory.setUri(uri);
           blobDirectory.setContainerName(containerName);
           blobDirectory.setPath(path);
           return blobDirectory;
       } catch (Throwable T) {
           throw new HDIException("Error creating the Blob Directory", T);
       }

    }

   public void uploadBlobFileContent(StorageAccount storageAccount,
                               BlobContainer blobContainer,
                               String filePath,
                               InputStream content,
                               CallableSingleArg<Void, Long> processBlockEvent,
                               long maxBlockSize,
                               long length)
            throws HDIException{
       try {
           CloudBlobClient t = getCloudBlobClient(storageAccount);
           String containerName = blobContainer.getName();
           CloudBlobContainer container = t.getContainerReference(containerName);
           CloudBlockBlob blob = container.getBlockBlobReference(filePath);
           long uploadedBytes = 0L;

           ArrayList blockEntries;
           long blockSize;
           for(blockEntries = new ArrayList(); uploadedBytes < length; uploadedBytes += blockSize) {
               String blockId = com.microsoft.azure.storage.core.Base64.encode(UUID.randomUUID().toString().getBytes());
               BlockEntry entry = new BlockEntry(blockId, BlockSearchMode.UNCOMMITTED);
               blockSize = maxBlockSize;
               if(length - uploadedBytes <= maxBlockSize) {
                   blockSize = length - uploadedBytes;
               }

               if(processBlockEvent != null) {
                   processBlockEvent.call(Long.valueOf(uploadedBytes));
               }

               entry.setSize(blockSize);
               blockEntries.add(entry);
               blob.uploadBlock(entry.getId(), content, blockSize);
           }

           blob.commitBlockList(blockEntries);
       } catch (Throwable T) {
           throw new HDIException("Error uploading the Blob File content", T);
       }
   }

   public void downloadBlobFileContent(StorageAccount storageAccount,
                                 BlobFile blobFile,
                                 OutputStream content)
            throws HDIException{
       try {
           CloudBlobClient t = getCloudBlobClient(storageAccount);
           String containerName = blobFile.getContainerName();
           CloudBlobContainer container = t.getContainerReference(containerName);
           CloudBlob blob = getCloudBlob(container, blobFile);
           blob.download(content);
       } catch (Throwable T) {
           throw new HDIException("Error downloading the Blob File content", T);
       }
   }

    public BlobFile createBlobFile(StorageAccount storageAccount,
                            BlobDirectory parentBlobDirectory,
                            BlobFile blobFile)
            throws HDIException{
        try {
            CloudBlobClient t = getCloudBlobClient(storageAccount);
            String containerName = parentBlobDirectory.getContainerName();
            CloudBlobContainer container = t.getContainerReference(containerName);
            CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());
            CloudBlob blob = getCloudBlob(parentDirectory, blobFile);
            blob.upload(new ByteArrayInputStream(new byte[0]), 0L);
            return reloadBlob(blob, containerName, blobFile);
        } catch (Throwable T) {
            throw new HDIException("Error creating the Blob File", T);
        }
    }

   public void deleteBlobFile(StorageAccount storageAccount,
                        BlobFile blobFile)
            throws HDIException{
       try {
           CloudBlobClient t = getCloudBlobClient(storageAccount);
           String containerName = blobFile.getContainerName();
           CloudBlobContainer container = t.getContainerReference(containerName);
           CloudBlob blob = getCloudBlob(container, blobFile);
           blob.deleteIfExists();
       } catch (Throwable T) {
           throw new HDIException("Error deleting the Blob File", T);
       }
    }

    private CloudBlobClient getCloudBlobClient(StorageAccount storageAccount)
            throws Exception {
         CloudStorageAccount csa = CloudStorageAccount.parse(storageAccount.getConnection());
         return csa.createCloudBlobClient();
    }

    private static String extractBlobItemName(String path, String delimiter) {
        if (path == null) {
            return "";
        } else if (delimiter != null && !delimiter.isEmpty()) {
            String[] parts = path.split(delimiter);
            return parts.length == 0 ? "" : parts[parts.length - 1];
        } else {
            return path;
        }
    }

    private static CloudBlob getCloudBlob(CloudBlobDirectory parentDirectory, BlobFile blobFile) throws URISyntaxException, StorageException {
        Object blob;
        if(blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = parentDirectory.getBlockBlobReference(blobFile.getName());
        } else {
            blob = parentDirectory.getPageBlobReference(blobFile.getName());
        }

        return (CloudBlob)blob;
    }

    private static CloudBlob getCloudBlob(CloudBlobContainer container, BlobFile blobFile) throws URISyntaxException, StorageException {
        Object blob;
        if(blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = container.getBlockBlobReference(blobFile.getPath());
        } else {
            blob = container.getPageBlobReference(blobFile.getPath());
        }

        return (CloudBlob)blob;
    }

    private static BlobFile reloadBlob(CloudBlob blob,String containerName, BlobFile blobFile) throws StorageException, URISyntaxException {
        blob.downloadAttributes();
        String uri = blob.getUri() != null?blob.getUri().toString():"";
        String path = Strings.nullToEmpty(blob.getName());
        String type = "";
        String cacheControlHeader = "";
        String contentEncoding = "";
        String contentLanguage = "";
        String contentType = "";
        String contentMD5Header = "";
        String eTag = "";
        GregorianCalendar lastModified = new GregorianCalendar();
        long size = 0L;
        BlobProperties properties = blob.getProperties();
        if(properties != null) {
            if(properties.getBlobType() != null) {
                type = properties.getBlobType().toString();
            }

            cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
            contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
            contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
            contentType = Strings.nullToEmpty(properties.getContentType());
            contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
            eTag = Strings.nullToEmpty(properties.getEtag());
            if(properties.getLastModified() != null) {
                lastModified.setTime(properties.getLastModified());
            }

            size = properties.getLength();
        }

        blobFile.setUri(uri);
        blobFile.setPath(path);
        blobFile.setContainerName(containerName);
        blobFile.setType(type);
        blobFile.setCacheControlHeader(cacheControlHeader);
        blobFile.setContentEncoding(contentEncoding);
        blobFile.setContentLanguage(contentLanguage);
        blobFile.setContentType(contentType);
        blobFile.setContentMD5Header(contentMD5Header);
        blobFile.setETag(eTag);
        blobFile.setLastModified(lastModified);
        blobFile.setSize(size);
        return blobFile;
    }
}