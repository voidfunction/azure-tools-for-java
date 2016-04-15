package com.microsoft.azure.hdinsight.sdk.storage;

public interface BlobItem {
    String getName();

    String getUri();

    String getContainerName();

    String getPath();

    BlobItem.BlobItemType getItemType();

    public enum BlobItemType {
        BlobFile,
        BlobDirectory
    }
}