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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.common.AggregatedException;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationErrorHandler;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ClusterManagerEx {

    private static final String OSTYPE = "linux";

    private static ClusterManagerEx instance = null;

    private List<IClusterDetail> cachedClusterDetails = new ArrayList<>();
    private List<IClusterDetail> hdinsightAdditionalClusterDetails = new ArrayList<>();

    private boolean isListClusterSuccess = false;
    private boolean isLIstAdditionalClusterSuccess = false;
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

    public List<IClusterDetail> getClusterDetailsWithoutAsync(Object project) {
        return getClusterDetailsWithoutAsync(false, project);
    }

    public List<IClusterDetail> getClusterDetailsWithoutAsync(boolean isIgnoreErrorCluster, Object projectObject) {
        cachedClusterDetails = ClusterMetaDataService.getInstance().getCachedClusterDetails();
        if(cachedClusterDetails.size() == 0) {
            cachedClusterDetails = getClusterDetails(projectObject);
        }

        if (isIgnoreErrorCluster == true) {
            List<IClusterDetail> result = new ArrayList<>();
            for (IClusterDetail clusterDetail : cachedClusterDetails) {
                if (clusterDetail instanceof ClusterDetail && !clusterDetail.getState().equalsIgnoreCase("Running")) {
                    continue;
                }
                result.add(clusterDetail);
            }
            return result;
        } else {
            return cachedClusterDetails;
        }
    }

    public synchronized List<IClusterDetail> getClusterDetails(Object project) {
        cachedClusterDetails.clear();

        if(!isLIstAdditionalClusterSuccess) {
            hdinsightAdditionalClusterDetails = getAdditionalClusters(project);
        }

        isListClusterSuccess = false;
        if (!AzureManagerImpl.getManager(project).authenticated()) {
            cachedClusterDetails.addAll(hdinsightAdditionalClusterDetails);
            return cachedClusterDetails;
        }

        List<Subscription> subscriptionList = AzureManagerImpl.getManager(project).getSubscriptionList();

        try {
            cachedClusterDetails = ClusterManager.getInstance().getHDInsightCausersWithSpecificType(subscriptionList, ClusterType.spark, OSTYPE, project);
            // TODO: so far we have not a good way to judge whether it is token expired as we have changed the way to list hdinsight clusters
            if (cachedClusterDetails.size() == 0) {
                //DefaultLoader.getUIHelper().showError("Falied to get HDInsight Cluster, Please make sure there's no login problem first","List HDInsight Cluster Error");
                isListClusterSuccess = false;
            } else {
                isListClusterSuccess = true;
            }
        } catch (AggregatedException aggregateException) {
            if (dealWithAggregatedException(aggregateException)) {
                DefaultLoader.getUIHelper().showError("Falied to get HDInsight Cluster, Please make sure there's no login problem first","List HDInsight Cluster Error");
//                if (isAuthSuccess()) {
//                    subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
//                    try {
//                        cachedClusterDetails.addAll(ClusterManager.getInstance().getHDInsightCausersWithSpecificType(subscriptionList, ClusterType.spark, OSTYPE));
//                        isListClusterSuccess = true;
//                    } catch (Exception exception) {
//                        DefaultLoader.getUIHelper().showError("Failed to list HDInsight cluster", "List HDInsight Cluster Error");
//                    }
//                }
            }
        }

        if (subscriptionList.isEmpty()) {
            isSelectedSubscriptionExist = false;
        } else {
            isSelectedSubscriptionExist = true;
        }

        cachedClusterDetails.addAll(hdinsightAdditionalClusterDetails);
        ClusterMetaDataService.getInstance().addCachedClusters(cachedClusterDetails);
        return cachedClusterDetails;
    }

    public synchronized void addHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.add(hdInsightClusterDetail);
        cachedClusterDetails.add(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    public synchronized void removeHDInsightAdditionalCluster(HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.remove(hdInsightClusterDetail);
        cachedClusterDetails.remove(hdInsightClusterDetail);

        saveAdditionalClusters();
    }

    public boolean isHDInsightAdditionalStorageExist(String clusterName, String storageName) {

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            if (clusterDetail.getName().equals(clusterName)) {
                try {
                    if (clusterDetail.getStorageAccount().getName().equals(storageName)) {
                        return true;
                    }
                } catch (HDIException e) {
                    return false;
                }

                List<HDStorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                if (additionalStorageAccount != null) {
                    for (ClientStorageAccount storageAccount : additionalStorageAccount) {
                        if (storageAccount.getName().equals(storageName)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void saveAdditionalClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(hdinsightAdditionalClusterDetails);
        DefaultLoader.getIdeHelper().setApplicationProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS, json);
    }

    private List<IClusterDetail> getAdditionalClusters(Object projectObject) {
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

    private boolean isAuthSuccess() {

        boolean isSuccess = false;
        try {
            AzureManager apiManager = AzureManagerImpl.getManager();
            apiManager.authenticate();
            isSuccess = true;
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException(
                    "An error occurred while attempting to sign in to your account.", e1,
                    "Error Signing In", false, true);
        } finally {
            return isSuccess;
        }
    }
}
