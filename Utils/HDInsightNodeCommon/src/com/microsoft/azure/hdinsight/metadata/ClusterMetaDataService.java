package com.microsoft.azure.hdinsight.metadata;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.helpers.NotNull;

import java.util.ArrayList;
import java.util.List;


public class ClusterMetaDataService {
    private static ClusterMetaDataService instance = new ClusterMetaDataService();
    private List<IClusterDetail> cachedClusters = new ArrayList<>();
//    private Map<Project, List<IClusterDetail>> cachedClustersMap = new HashMap<>();

    private ClusterMetaDataService() {
    }

    public static ClusterMetaDataService getInstance() {
        return instance;
    }

    public List<IClusterDetail> getCachedClusterDetails () {
        return cachedClusters;
    }

    public void addCachedClusters(@NotNull List<IClusterDetail> clusterDetails) {
        cachedClusters = clusterDetails;
    }

    public boolean isCachedClusterExist(@NotNull IClusterDetail clusterDetail) {
        for (IClusterDetail iClusterDetail : cachedClusters) {
            if (iClusterDetail.getName().equalsIgnoreCase(clusterDetail.getName())) {
                return true;
            }
        }
        return false;
    }
}
