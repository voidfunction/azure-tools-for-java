package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;

import java.util.List;

public class HDInsightRootModule extends AzureRefreshableNode {

    private static final String HDInsight_SERVICE_MODULE_ID = HDInsightRootModule.class.getName();
    private static final String ICON_PATH = CommonConst.HDInsightGrayIconPath;
    private static final String BASE_MODULE_NAME = "HDInsight";

//    private Project project;
    private EventHelper.EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();

    private List<IClusterDetail> clusterDetailList;

    public HDInsightRootModule(Node parent) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

//    @Override
//    protected void refreshItems() throws AzureCmdException {
//        synchronized (this) {
//            removeAllChildNodes();
//            TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerHDInsightNodeExpand, null, null);
//            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails((Project) getProject());
//
//            if (clusterDetailList != null) {
//                for (IClusterDetail clusterDetail : clusterDetailList) {
//                    addChildNode(new ClusterNode(this, clusterDetail, (Project) getProject()));
//                }
//            }
//        }
//    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState) throws AzureCmdException {
        synchronized (this) { //todo???
            removeAllChildNodes();
            TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerHDInsightNodeExpand, null, null);
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails();

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }
    }


//    @Override
//    public Object getProject() {
//        return project;
//    }

//    public void registerSubscriptionsChanged()
//            throws AzureCmdException {
//        synchronized (subscriptionsChangedSync) {
//            if (subscriptionsChanged == null) {
//                subscriptionsChanged = AzureManagerImpl.getManager().registerSubscriptionsChanged();
//            }
//
//            registeredSubscriptionsChanged = true;
//
//            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
//                @Override
//                public void run() {
//                    while (registeredSubscriptionsChanged) {
//                        try {
//                            subscriptionsChanged.waitEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (registeredSubscriptionsChanged) {
//                                        removeAllChildNodes();
//                                        load();
//                                    }
//                                }
//                            });
//                        } catch (AzureCmdException ignored) {
//                            break;
//                        }
//                    }
//                }
//            });
//        }
//    }

    public void refreshWithoutAsync() {
        synchronized (this) {
            removeAllChildNodes();
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync();

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }

    }

}