package com.microsoft.azure.hdinsight.sdk.rest.spark.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StorageLevel {
    @JsonProperty("Use Disk")
    private boolean isUseDisk;

    @JsonProperty("Use Memory")
    private boolean isUseMemory;

    @JsonProperty("Deserialized")
    private boolean isDeserialized;

    @JsonProperty("Replication")
    private int replication;

    public boolean isUseDisk() {
        return isUseDisk;
    }

    public void setUseDisk(boolean useDisk) {
        isUseDisk = useDisk;
    }

    public boolean isUseMemory() {
        return isUseMemory;
    }

    public void setUseMemory(boolean useMemory) {
        isUseMemory = useMemory;
    }

    public boolean isDeserialized() {
        return isDeserialized;
    }

    public void setDeserialized(boolean deserialized) {
        isDeserialized = deserialized;
    }

    public int getReplication() {
        return replication;
    }

    public void setReplication(int replication) {
        this.replication = replication;
    }
}
