package com.microsoft.azure.hdinsight.sdk.storage;

import java.util.Calendar;

public class BlobContainer {
    private String name;
    private String uri;
    private String eTag;
    private Calendar lastModified;
    private String publicReadAccessType;

    public BlobContainer( String name,
                          String uri,
                          String eTag,
                          Calendar lastModified,
                          String publicReadAccessType) {
        this.name = name;
        this.uri = uri;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.publicReadAccessType = publicReadAccessType;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(Calendar lastModified) {
        this.lastModified = lastModified;
    }

    public String getPublicReadAccessType() {
        return publicReadAccessType;
    }

    public void setPublicReadAccessType(String publicReadAccessType) {
        this.publicReadAccessType = publicReadAccessType;
    }
}
