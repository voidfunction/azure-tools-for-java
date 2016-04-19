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
    private static final String ICON_PATH = CommonConst.HDExplorerIconPath;
    private static final String BASE_MODULE_NAME = "HDInsight";

    private List<IClusterDetail> clusterDetailList;

    public HDInsightRootModule(Node parent) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

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