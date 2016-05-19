package com.microsoft.azure.hdinsight.sdk.cluster;


import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.Subscription;

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

    HDStorageAccount getStorageAccount() throws HDIException;

    List<HDStorageAccount> getAdditionalStorageAccounts();

    void getConfigurationInfo(Object project) throws IOException, HDIException, AzureCmdException;
}