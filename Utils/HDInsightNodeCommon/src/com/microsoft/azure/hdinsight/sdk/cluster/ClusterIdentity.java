package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.SerializedName;

public class ClusterIdentity{
    @SerializedName("clusterIdentity.applicationId")
    private String applicationId;

    @SerializedName("clusterIdentity.certificate")
    private String certificate;

    @SerializedName("clusterIdentity.aadTenantId")
    private String aadTenantId;

    @SerializedName("clusterIdentity.resourceUri")
    private String resourceUri;

    @SerializedName("clusterIdentity.certificatePassword")
    private String certificatePassword;

    public String getClusterIdentityapplicationId(){
        return applicationId;
    }

    public String getClusterIdentitycertificate(){
        return certificate;
    }

    public String getClusterIdentityaadTenantId(){
        return aadTenantId;
    }

    public String getClusterIdentityresourceUri(){
        return resourceUri;
    }

    public String getClusterIdentitycertificatePassword(){
        return certificatePassword;
    }

}
