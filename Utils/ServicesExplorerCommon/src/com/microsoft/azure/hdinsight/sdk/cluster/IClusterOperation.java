package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.Subscription;

import java.io.IOException;
import java.util.List;

public interface IClusterOperation {

    /**
     * list hdinsight cluster
     * @param subscription
     * @return cluster raw data info
     * @throws IOException
     */
    List<ClusterRawInfo> listCluster(Subscription subscription) throws IOException, HDIException, AzureCmdException;

    /**
     * get cluster configuration including http username, password, storage and additional storage account
     * @param subscription
     * @param clusterId
     * @return cluster configuration info
     * @throws IOException
     */
    ClusterConfiguration getClusterConfiguration(Subscription subscription, String clusterId) throws IOException, HDIException, AzureCmdException;
}
