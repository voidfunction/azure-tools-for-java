package com.microsoft.azure.hdinsight.sdk.cluster;

public class ConnectivityEndpoint {
    private String name;
    private String protocol;
    private String location;
    private int port;

    public String getName(){
        return name;
    }

    public String getProtocol(){
        return protocol;
    }

    public String getLocation(){
        return location;
    }

    public int getPort(){
        return port;
    }
}
