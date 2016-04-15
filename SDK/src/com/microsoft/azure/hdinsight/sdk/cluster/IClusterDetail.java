package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;

import java.io.IOException;
import java.util.List;

public interface IClusterDetail {

    boolean isConfigInfoAvailable();

    String getName();

    String getState();

    String getLocation();

    String getConnectionUrl();

    String getCreateDate();

    ClusterType getType();

    String getVersion();

    Subscription getSubscription();

    int getDataNodes();

    String getHttpUserName() throws HDIException;

    String getHttpPassword() throws HDIException;

    String getOSType();

    String getResourceGroup();

    StorageAccount getStorageAccount() throws HDIException;

    List<StorageAccount> getAdditionalStorageAccounts();

    void getConfigurationInfo() throws IOException, HDIException;
}