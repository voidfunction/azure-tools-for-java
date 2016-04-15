package com.microsoft.azure.hdinsight.sdk.storage;

public class StorageAccount {
    private final String Default_Protocol = "https";
    private String storageName;
    private String storageKey;
    private String protocol;
    private String fullStorageBlobName;

    private boolean isDefaultStorageAccount;
    private String defaultContainer;

    public StorageAccount(String name, String key, boolean isDefault, String defaultContainer){
        this.fullStorageBlobName = name;
        this.storageName = name.replace(".blob.core.windows.net", "");
        this.storageKey = key;
        this.isDefaultStorageAccount = isDefault;
        this.defaultContainer = defaultContainer;
        this.protocol = Default_Protocol;
    }

    public String getStorageName(){
        return storageName;
    }

    public String getStorageKey(){
        return storageKey;
    }

    public String getProtocol(){
        return protocol;
    }

    public String getFullStoragBlobName(){
        return fullStorageBlobName;
    }

    public boolean isDefaultStorageAccount(){
        return isDefaultStorageAccount;
    }

    public String getDefaultContainer(){
        return defaultContainer;
    }

    public String getConnection(){
        return String.format("DefaultEndpointsProtocol=%s;AccountName=%s;AccountKey=%s",
                new Object[]{this.getProtocol(), this.getStorageName(), this.getStorageKey()});
    }

}