package com.microsoft.azure.hdinsight.sdk.cluster;

import java.util.List;

public class ClusterProperties {
    private String clusterVersion;
    private String osType;
    private ClusterDefinition clusterDefinition;
    private ComputeProfile computeProfile;
    private String provisioningState;
    private String clusterState;
    private String createdDate;
    private QuotaInfo quotaInfo;
    private List<ConnectivityEndpoint> connectivityEndpoints;

    public String getClusterVersion(){
        return clusterVersion;
    }

    public String getOsType(){
        return osType;
    }

    public String getProvisioningState(){
        return provisioningState;
    }

    public String getClusterState(){
        return clusterState;
    }

    public ClusterDefinition getClusterDefinition() {
        return clusterDefinition;
    }

    public ComputeProfile getComputeProfile(){
        return computeProfile;
    }

    public String getCreatedDate(){
        return createdDate;
    }

    public QuotaInfo getQuotaInfo(){
        return quotaInfo;
    }

    public List<ConnectivityEndpoint> getConnectivityEndpoints(){
        return connectivityEndpoints;
    }
}
