package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.Expose;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.model.Subscription;

import java.io.IOException;
import java.util.List;

public class HDInsightAdditionalClusterDetail implements IClusterDetail {

    private String clusterName;
    private String userName;
    private String passWord;

    @Expose
    private HDStorageAccount defaultStorageAccount;

    public HDInsightAdditionalClusterDetail(String clusterName, String userName, String passWord, HDStorageAccount storageAccount) {
        this.clusterName = clusterName;
        this.userName = userName;
        this.passWord = passWord;
        defaultStorageAccount = storageAccount;
    }

    @Override
    public boolean isConfigInfoAvailable() {
        return false;
    }

    @Override
    public String getName() {
        return clusterName;
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getConnectionUrl() {
        return String.format("https://%s.azurehdinsight.net", getName());
    }

    @Override
    public String getCreateDate() {
        return null;
    }

    @Override
    public ClusterType getType() {
        return null;
    }

    @Override
    public String getResourceGroup(){
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public Subscription getSubscription() {
        return null;
    }

    @Override
    public int getDataNodes() {
        return 0;
    }

    @Override
    public String getHttpUserName() throws HDIException {
        return userName;
    }

    @Override
    public String getHttpPassword() throws HDIException {
        return passWord;
    }

    @Override
    public String getOSType() {
        return null;
    }

    @Override
    public HDStorageAccount getStorageAccount() throws HDIException {
        return defaultStorageAccount;
    }

    @Override
    public List<HDStorageAccount> getAdditionalStorageAccounts() {
        return null;
    }

    @Override
    public void getConfigurationInfo(Object project) throws IOException, HDIException {

    }
}
