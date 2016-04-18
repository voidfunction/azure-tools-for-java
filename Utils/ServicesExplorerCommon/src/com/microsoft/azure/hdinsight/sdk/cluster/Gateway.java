package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.SerializedName;

public class Gateway {

    @SerializedName("restAuthCredential.isEnabled")
    private String isEnabled;

    @SerializedName("restAuthCredential.password")
    private String password;

    @SerializedName("restAuthCredential.username")
    private String username;

    public String getIsEnabled(){
        return isEnabled;
    }

    public String getPassword(){
        return password;
    }

    public String getUsername(){
        return username;
    }
}
