package com.microsoft.azure.hdinsight.sdk.storage;

import java.util.Calendar;

public class BlobFile implements BlobItem{
    private boolean loading;
    private String name;
    private String uri;
    private String containerName;
    private String path;
    private String type;
    private String cacheControlHeader;
    private String contentEncoding;
    private String contentLanguage;
    private String contentType;
    private String contentMD5Header;
    private String eTag;
    private Calendar lastModified;
    private long size;

    public BlobFile(String name,
                    String uri,
                    String containerName,
                    String path,
                    String type,
                    String cacheControlHeader,
                    String contentEncoding,
                    String contentLanguage,
                    String contentType,
                    String contentMD5Header,
                    String eTag,
                    Calendar lastModified,
                    long size) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
        this.type = type;
        this.cacheControlHeader = cacheControlHeader;
        this.contentEncoding = contentEncoding;
        this.contentLanguage = contentLanguage;
        this.contentType = contentType;
        this.contentMD5Header = contentMD5Header;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.size = size;
    }

    public boolean isLoading() {
        return this.loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
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

    public void setPath(String path) {
        this.path = path;
    }

    public BlobItemType getItemType() {
        return BlobItemType.BlobFile;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCacheControlHeader() {
        return this.cacheControlHeader;
    }

    public void setCacheControlHeader(String cacheControlHeader) {
        this.cacheControlHeader = cacheControlHeader;
    }

    public String getContentEncoding() {
        return this.contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getContentLanguage() {
        return this.contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentMD5Header() {
        return this.contentMD5Header;
    }

    public void setContentMD5Header(String contentMD5Header) {
        this.contentMD5Header = contentMD5Header;
    }

    public String getETag() {
        return this.eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public Calendar getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(Calendar lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
