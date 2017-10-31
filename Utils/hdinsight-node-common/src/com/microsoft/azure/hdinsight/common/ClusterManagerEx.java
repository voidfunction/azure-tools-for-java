/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.common;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.common.AggregatedException;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationErrorHandler;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClusterManagerEx {

    private static final String OSTYPE = "linux";

    private static ClusterManagerEx instance = null;

    private List<IClusterDetail> hdinsightAdditionalClusterDetails = new ArrayList<>();
    private List<IClusterDetail> emulatorClusterDetails = new ArrayList<>();

    private boolean isListClusterSuccess = false;
    private boolean isLIstAdditionalClusterSuccess = false;
    private boolean isListEmulatorClusterSuccess = false;
    private boolean isSelectedSubscriptionExist = false;

    private ClusterManagerEx() {
    }

    public static ClusterManagerEx getInstance() {
        if (instance == null) {
            synchronized (ClusterManagerEx.class) {
                if (instance == null) {
                    instance = new ClusterManagerEx();
                }
            }
        }

        return instance;
    }

    public String getClusterConnectionString(final @NotNull String clusterName) {
        if (isAzureChinaEnvironment()) {
            return String.format("https://%s.azurehdinsight.cn", clusterName);
        } else {
            return String.format("https://%s.azurehdinsight.net", clusterName);
        }
    }

    public String getBlobFullName(@NotNull final String storageName) {
        return storageName + getBlobSuffix();
    }

    public String getBlobSuffix() {
        if (isAzureChinaEnvironment()){
            return ".blob.core.chinacloudapi.cn";
        } else {
            return "blob.core.windows.net";
        }
    }

    public String getPortalUrl() {
        if (isAzureChinaEnvironment()) {
            return "https://portal.azure.cn/";
        } else {
            return "https://portal.azure.com/";
        }
    }

    public Environment getAzureEnvironment() {
        AzureManager azureManager = null;
        Environment env = Environment.GLOBAL;

        try {
            azureManager = AuthMethodManager.getInstance().getAzureManager();
        } catch (IOException e) {
            // ignore the exception
        }

        if (azureManager != null) {
            env = azureManager.getEnvironment();
        }
        return Environment.valueOf(env.getName());
    }

    public boolean isAzureChinaEnvironment() {
        return getAzureEnvironment() == Environment.CHINA;
    }

    //make sure calling it after getClusterDetails(project)
    public boolean isSelectedSubscriptionExist() {
        return isSelectedSubscriptionExist;
    }

    public boolean isListClusterSuccess() {
        return isListClusterSuccess;
    }

    public boolean isLIstAdditionalClusterSuccess() {
        return isLIstAdditionalClusterSuccess;
    }

    public boolean isListEmulatorClusterSuccess() { return isListEmulatorClusterSuccess; }

    public ImmutableList<IClusterDetail> getClusterDetailsWithoutAsync() {
        return getClusterDetailsWithoutAsync(false);
    }

    public ImmutableList<IClusterDetail> getClusterDetailsWithoutAsync(boolean isIgnoreErrorCluster) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        if (isIgnoreErrorCluster) {
            List<IClusterDetail> result = new ArrayList<>();
            for (IClusterDetail clusterDetail : cachedClusterDetails) {
                if (clusterDetail instanceof ClusterDetail && !clusterDetail.getState().equalsIgnoreCase("Running")) {
                    continue;
                }
                result.add(clusterDetail);
            }
            return ImmutableList.copyOf(result);
        } else {
            return cachedClusterDetails;
        }
    }

    public Optional<IClusterDetail> getClusterDetailByName(String clusterName) {
        return getClusterDetailsWithoutAsync(true)
                .stream()
                .filter(cluster -> cluster.getName().equals(clusterName))
                .findFirst()
                .flatMap(cluster -> {
                    try {
                        cluster.getConfigurationInfo();
                    } catch (Exception ignore) {
                    }

                    return cluster.isConfigInfoAvailable() ? Optional.of(cluster) : Optional.empty();
                });
    }

    public synchronized ImmutableList<IClusterDetail> getClusterDetails() {
        List<IClusterDetail> clusterDetails = new ArrayList<>();

        if(!isLIstAdditionalClusterSuccess) {
            hdinsightAdditionalClusterDetails = getAdditionalClusters();
        }

        if(!isListEmulatorClusterSuccess) {
            emulatorClusterDetails = getEmulatorClusters();
        }

        isListClusterSuccess = false;
        com.microsoft.azuretools.sdkmanage.AzureManager manager;
        try {
            manager = AuthMethodManager.getInstance().getAzureManager();
        } catch (Exception ex) {
            // not authenticated
            clusterDetails.addAll(hdinsightAdditionalClusterDetails);
            clusterDetails.addAll(emulatorClusterDetails);
            ClusterMetaDataService.getInstance().addCachedClusters(clusterDetails);
            return ClusterMetaDataService.getInstance().getCachedClusterDetails();
        }

        List<SubscriptionDetail> subscriptionList = null;
        try {
            subscriptionList = manager.getSubscriptionManager().getSubscriptionDetails();
            clusterDetails = ClusterManager.getInstance().getHDInsightClustersWithSpecificType(subscriptionList, OSTYPE);

            // TODO: so far we have not a good way to judge whether it is token expired as we have changed the way to list hdinsight clusters
            isListClusterSuccess = clusterDetails.size() != 0;
        } catch (AggregatedException aggregateException) {
            if (dealWithAggregatedException(aggregateException)) {
                DefaultLoader.getUIHelper().showError("Failed to get HDInsight Cluster, Please make sure there's no login problem first","List HDInsight Cluster Error");
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().showError("Failed to get HDInsight Clusters","List HDInsight Cluster Error");
        }

        isSelectedSubscriptionExist = subscriptionList != null && !subscriptionList.isEmpty();

        clusterDetails.addAll(hdinsightAdditionalClusterDetails);
        clusterDetails.addAll(emulatorClusterDetails);
        ClusterMetaDataService.getInstance().addCachedClusters(clusterDetails);
        return ClusterMetaDataService.getInstance().getCachedClusterDetails();
    }

    public synchronized  void addEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.add(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void addHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.add(hdInsightClusterDetail);
        ClusterMetaDataService.getInstance().addClusterToCache(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    public synchronized  void removeEmulatorCluster(EmulatorClusterDetail emulatorClusterDetail) {
        emulatorClusterDetails.remove(emulatorClusterDetail);
        ClusterMetaDataService.getInstance().removeClusterFromCache(emulatorClusterDetail);

        saveEmulatorClusters();
    }

    public synchronized void removeHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.remove(hdInsightClusterDetail);
        ClusterMetaDataService.getInstance().removeClusterFromCache(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    /*
        return 0: cluster can be added to additional cluster list
        return 1: cluster already exist in current cluster list
        return 2: cluster is valid to add to cluster list but storage account is not default
     */
    public int isHDInsightAdditionalStorageExist(String clusterName, String storageName) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            if (clusterDetail.getName().equals(clusterName)) {
                try {
                    if (clusterDetail.getStorageAccount().getName().equals(storageName)) {
                        return 1;
                    }
                } catch (HDIException e) {
                    return 0;
                }

                List<HDStorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                if (additionalStorageAccount != null) {
                    for (HDStorageAccount storageAccount : additionalStorageAccount) {
                        if (storageAccount.getName().equals(storageName)) {
                            return 2;
                        }
                    }
                }
            }
        }

        return 0;
    }

    public boolean isEmulatorClusterExist(String clusterName) {
        final ImmutableList<IClusterDetail> cachedClusterDetails =
                Optional.of(ClusterMetaDataService.getInstance().getCachedClusterDetails())
                        .filter(clusters -> !clusters.isEmpty())
                        .orElseGet(this::getClusterDetails);

        for( IClusterDetail clusterDetail : cachedClusterDetails) {
            if( clusterDetail.getName().equals(clusterName)) {
                return true;
            }
        }

        return false;
    }

    private void saveEmulatorClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(emulatorClusterDetails);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.EMULATOR_CLUSTERS, json);
    }

    private void saveAdditionalClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(hdinsightAdditionalClusterDetails);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS, json);
    }

    private List<IClusterDetail> getAdditionalClusters() {
        Gson gson = new Gson();
        String json = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
        List<IClusterDetail> hdiLocalClusters = new ArrayList<>();

        isLIstAdditionalClusterSuccess = false;
        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                hdiLocalClusters = gson.fromJson(json, new TypeToken<ArrayList<HDInsightAdditionalClusterDetail>>() {
                }.getType());
            } catch (JsonSyntaxException e) {

                isLIstAdditionalClusterSuccess = false;
                // clear local cache if we cannot get information from local json
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
                DefaultLoader.getUIHelper().showException("Failed to list additional HDInsight cluster", e, "List Additional HDInsight Cluster", false, true);
                return new ArrayList<>();
            }
        }

        isLIstAdditionalClusterSuccess = true;
        return hdiLocalClusters;
    }

    private List<IClusterDetail> getEmulatorClusters() {
        Gson gson = new Gson();
        String json = DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.EMULATOR_CLUSTERS);
        List<IClusterDetail> emulatorClusters = new ArrayList<>();

        isListEmulatorClusterSuccess = false;
        if(!StringHelper.isNullOrWhiteSpace(json)){
            try {
                emulatorClusters = gson.fromJson(json, new TypeToken<ArrayList<EmulatorClusterDetail>>(){
                }.getType());
            } catch (JsonSyntaxException e){

                isListEmulatorClusterSuccess = false;
                DefaultLoader.getIdeHelper().unsetApplicationProperty(CommonConst.EMULATOR_CLUSTERS);
                DefaultLoader.getUIHelper().showException("Failed to list emulator cluster", e, "List Emulator Cluster", false, true);
                return new ArrayList<>();
            }
        }

        isListEmulatorClusterSuccess = true;
        return emulatorClusters;
    }

    private boolean dealWithAggregatedException(AggregatedException aggregateException) {
        boolean isReAuth = false;
        for (Exception exception : aggregateException.getExceptionList()) {
            if (exception instanceof HDIException) {
                if (((HDIException) exception).getErrorCode() == AuthenticationErrorHandler.AUTH_ERROR_CODE) {
                    isReAuth = true;
                    break;
                }
            }
        }

        return isReAuth;
    }
}
