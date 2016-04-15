package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.hdinsight.sdk.common.*;
import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ClusterOperationImpl implements IClusterOperation {

    private final String VERSION = "2015-03-01-preview";

    /**
     * list hdinsight cluster
     *
     * @param subscription
     * @return cluster raw data info
     * @throws IOException
     */
    public List<ClusterRawInfo> listCluster(Subscription subscription) throws IOException, HDIException {
        String response = AzureAADRequestHelper.executeRequest(
                CommonConstant.hdinsightClusterUri,
                String.format("api/Clusters/GetAll?subscriptionIds=%s;&_=%d", subscription.getSubscriptionId(), new Date().getTime()),
                RestServiceManager.ContentType.Json,
                "GET",
                null,
                subscription.getAccessToken(),
                new RestServiceManagerBaseImpl() {
                });

        return new AuthenticationErrorHandler<List<ClusterRawInfo>>() {
            @Override
            public List<ClusterRawInfo> execute(String response) {
                Type listType = new TypeToken<List<ClusterRawInfo>>() {
                }.getType();
                List<ClusterRawInfo> clusterRawInfoList = new Gson().fromJson(response, listType);
                return clusterRawInfoList;
            }
        }.run(response);
    }

    /**
     * get cluster configuration including http username, password, storage and additional storage account
     *
     * @param subscription
     * @param clusterId
     * @return cluster configuration info
     * @throws IOException
     */
    public ClusterConfiguration getClusterConfiguration(Subscription subscription, String clusterId) throws IOException, HDIException {
        String response = AzureAADRequestHelper.executeRequest(
                CommonConstant.managementUri,
                String.format("%s/configurations?api-version=%s", clusterId.replaceAll("/+$", ""), VERSION),
                null,
                "GET",
                null,
                subscription.getAccessToken(),
                new RestServiceManagerBaseImpl() {
                });

        return new AuthenticationErrorHandler<ClusterConfiguration>() {
            @Override
            public ClusterConfiguration execute(String response) {
                Type listType = new TypeToken<ClusterConfiguration>() {
                }.getType();
                ClusterConfiguration clusterConfiguration = new Gson().fromJson(response, listType);

                if(clusterConfiguration == null || clusterConfiguration.getConfigurations() == null)
                {
                    return null;
                }

                return clusterConfiguration;
            }
        }.run(response);
    }
}
