package com.microsoft.azure.hdinsight.sdk.cluster;

public class Role {
    private String name;
    private int targetInstanceCount;
    private HardwareProfile hardwareProfile;

    public String getName(){
        return name;
    }

    public int getTargetInstanceCount(){
        return targetInstanceCount;
    }

    public HardwareProfile getHardwareProfile() {
        return hardwareProfile;
    }
}

