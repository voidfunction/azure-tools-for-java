package com.microsoft.azure.hdinsight.metadata;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ClusterMetaDataService{
    private static ClusterMetaDataService instance = new ClusterMetaDataService();
    private Map<Project, List<IClusterDetail>> cachedClustersMap = new HashMap<>();

    private ClusterMetaDataService() {
    }

    public static ClusterMetaDataService getInstance() {
        return instance;
    }

    public List<IClusterDetail> getCachedClusterDetails (@NotNull Project project) {
        if(cachedClustersMap.containsKey(project)) {
            return cachedClustersMap.get(project);
        } else {
            return new ArrayList<>();
        }
    }

    public void addCachedClusters(@NotNull Project project, @NotNull List<IClusterDetail> clusterDetails) {
        cachedClustersMap.put(project, clusterDetails);
    }

    public boolean isCachedClusterExist(@NotNull Project project, @NotNull IClusterDetail clusterDetail) {
        List<IClusterDetail> clusterDetails = cachedClustersMap.get(project);
        if(clusterDetails == null) {
            return false;
        }

        for (IClusterDetail iClusterDetail : clusterDetails) {
            iClusterDetail.getName().equalsIgnoreCase(clusterDetail.getName());
            return true;
        }
        return false;
    }
}
