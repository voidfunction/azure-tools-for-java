package com.microsoft.azure.hdinsight.common;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.hdinsight.sdk.common.AggregatedException;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationErrorHandler;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;
import com.microsoft.azure.hdinsight.serverexplore.AzureManager;
import com.microsoft.azure.hdinsight.serverexplore.AzureManagerImpl;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import org.jetbrains.annotations.NotNull;

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

    public List<IClusterDetail> getClusterDetailsWithoutAsync(Project project) {
        return getClusterDetailsWithoutAsync(project, false);
    }

    public List<IClusterDetail> getClusterDetailsWithoutAsync(@NotNull Project project, boolean isIgnoreErrorCluster) {
        cachedClusterDetails = ClusterMetaDataService.getInstance().getCachedClusterDetails(project);
        if(cachedClusterDetails.size() == 0) {
            cachedClusterDetails = getClusterDetails(project);
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

    public synchronized List<IClusterDetail> getClusterDetails(Project project) {
        cachedClusterDetails.clear();

        if(!isLIstAdditionalClusterSuccess) {
            hdinsightAdditionalClusterDetails = getAdditionalClusters();
        }

        isListClusterSuccess = false;
        if (!AzureManagerImpl.getManager().authenticated()) {
            if (!isAuthSuccess()) {
                cachedClusterDetails.addAll(hdinsightAdditionalClusterDetails);
                return cachedClusterDetails;
            }
        }

        List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();

        try {
            cachedClusterDetails = ClusterManager.getInstance().getHDInsightCausersWithSpecificType(subscriptionList, ClusterType.spark, OSTYPE);
            // TODO: so far we have not a good way to judge whether it is token expired as we have changed the way to list hdinsight clusters
            if (cachedClusterDetails.size() == 0) {
                if (isAuthSuccess()) {
                    subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
                    try {
                        cachedClusterDetails.addAll(ClusterManager.getInstance().getHDInsightCausersWithSpecificType(subscriptionList, ClusterType.spark, OSTYPE));
                        isListClusterSuccess = true;
                    } catch (Exception exception) {
                        DefaultLoader.getUIHelper().showError("Failed to list HDInsight cluster", "List HDInsight Cluster Error");
                    }
                }
            } else {
                isListClusterSuccess = true;
            }
        } catch (AggregatedException aggregateException) {
            if (dealWithAggregatedException(aggregateException)) {
                if (isAuthSuccess()) {
                    subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
                    try {
                        cachedClusterDetails.addAll(ClusterManager.getInstance().getHDInsightCausersWithSpecificType(subscriptionList, ClusterType.spark, OSTYPE));
                        isListClusterSuccess = true;
                    } catch (Exception exception) {
                        DefaultLoader.getUIHelper().showError("Failed to list HDInsight cluster", "List HDInsight Cluster Error");
                    }
                }
            }
        }

        if (subscriptionList.isEmpty()) {
            isSelectedSubscriptionExist = false;
        } else {
            isSelectedSubscriptionExist = true;
        }

        cachedClusterDetails.addAll(hdinsightAdditionalClusterDetails);
        ClusterMetaDataService.getInstance().addCachedClusters(project, cachedClusterDetails);
        return cachedClusterDetails;
    }

    public synchronized void addHDInsightAdditionalCluster(Project project, HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.add(hdInsightClusterDetail);
        cachedClusterDetails.add(hdInsightClusterDetail);

        PluginUtil.getServerExplorerRootModule(project).refreshWithoutAsync();
        saveAdditionalClusters();
    }

    public synchronized void removeHDInsightAdditionalCluster(Project project, HDInsightAdditionalClusterDetail hdInsightClusterDetail) {

        hdinsightAdditionalClusterDetails.remove(hdInsightClusterDetail);
        cachedClusterDetails.remove(hdInsightClusterDetail);

        PluginUtil.getServerExplorerRootModule(project).refreshWithoutAsync();
        saveAdditionalClusters();
    }

    public boolean isHDInsightAdditionalStorageExist(String clusterName, String storageName) {

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            if (clusterDetail.getName().equals(clusterName)) {
                try {
                    if (clusterDetail.getStorageAccount().getStorageName().equals(storageName)) {
                        return true;
                    }
                } catch (HDIException e) {
                    return false;
                }

                List<StorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                for (StorageAccount storageAccount : additionalStorageAccount) {
                    if (storageAccount.getStorageName().equals(storageName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void saveAdditionalClusters() {
        Gson gson = new Gson();
        String json = gson.toJson(hdinsightAdditionalClusterDetails);
        DefaultLoader.getIdeHelper().setProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS, json);
    }

    private List<IClusterDetail> getAdditionalClusters() {
        Gson gson = new Gson();
        String json = DefaultLoader.getIdeHelper().getProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
        List<IClusterDetail> hdiLocalClusters = new ArrayList<>();

        isLIstAdditionalClusterSuccess = false;
        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                hdiLocalClusters = gson.fromJson(json, new TypeToken<ArrayList<HDInsightAdditionalClusterDetail>>() {
                }.getType());
            } catch (JsonSyntaxException e) {

                isLIstAdditionalClusterSuccess = false;
                // clear local cache if we cannot get information from local json
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.HDINSIGHT_ADDITIONAL_CLUSTERS);
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
        } catch (HDExploreException e1) {
            DefaultLoader.getUIHelper().showException(
                    "An error occurred while attempting to sign in to your account.", e1,
                    "Error Signing In", false, true);
        } finally {
            return isSuccess;
        }
    }
}
