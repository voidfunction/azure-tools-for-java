package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.serverexplore.AzureManagerImpl;
import com.microsoft.azure.hdinsight.serverexplore.node.EventHelper;
import com.microsoft.azure.hdinsight.serverexplore.node.Node;
import com.microsoft.azure.hdinsight.serverexplore.node.RefreshableNode;

import java.util.List;

public class HDInsightRootModule extends RefreshableNode {
    private static final String HDInsight_SERVICE_MODULE_ID = HDInsightRootModule.class.getName();
    private static final String ICON_PATH = CommonConst.HDInsightGrayIconPath;
    private static final String BASE_MODULE_NAME = "HDInsight";

    private Project project;
    private EventHelper.EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();

    private List<IClusterDetail> clusterDetailList;

    public HDInsightRootModule(Node parent, String iconPath, Object data) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, iconPath);
    }

    public HDInsightRootModule(Project project) {
        this(null, ICON_PATH, null);
        this.project = project;
    }


    @Override
    protected void refreshItems() throws HDExploreException {
        synchronized (this) {
            removeAllChildNodes();
            TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerHDInsightNodeExpand, null, null);
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails(this.project);

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail, project));
                }
            }
        }
    }

    @Override
    public Object getProject() {
        return project;
    }

    public void registerSubscriptionsChanged()
            throws HDExploreException {
        synchronized (subscriptionsChangedSync) {
            if (subscriptionsChanged == null) {
                subscriptionsChanged = AzureManagerImpl.getManager().registerSubscriptionsChanged();
            }

            registeredSubscriptionsChanged = true;

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    while (registeredSubscriptionsChanged) {
                        try {
                            subscriptionsChanged.waitEvent(new Runnable() {
                                @Override
                                public void run() {
                                    if (registeredSubscriptionsChanged) {
                                        removeAllChildNodes();
                                        load();
                                    }
                                }
                            });
                        } catch (HDExploreException ignored) {
                            break;
                        }
                    }
                }
            });
        }
    }

    public void refreshWithoutAsync() {
        synchronized (this) {
            removeAllChildNodes();
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(this.project);

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail, this.project));
                }
            }
        }

    }

}