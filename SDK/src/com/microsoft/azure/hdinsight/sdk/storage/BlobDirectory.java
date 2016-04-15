package com.microsoft.azure.hdinsight.sdk.storage;

public class BlobDirectory implements BlobItem{
    private String name;
    private String uri;
    private String containerName;
    private String path;

    public BlobDirectory(String name, String uri, String containerName, String path) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
    }

    public String getName() {
        return this.name;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath( String path) {
        this.path = path;
    }

    public BlobItemType getItemType() {
        return BlobItemType.BlobDirectory;
    }
}
