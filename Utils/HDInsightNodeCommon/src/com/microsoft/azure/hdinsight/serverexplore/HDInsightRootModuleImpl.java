package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.ClusterNode;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class HDInsightRootModuleImpl extends HDInsightRootModule {

    private static final String HDInsight_SERVICE_MODULE_ID = HDInsightRootModuleImpl.class.getName();
    private static final String ICON_PATH = CommonConst.HDExplorerIconPath;
    private static final String BASE_MODULE_NAME = "HDInsight";

    public HDInsightRootModuleImpl(@NotNull Node parent) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    private List<IClusterDetail> clusterDetailList;

    @Override
    public HDInsightRootModule getNewNode(@NotNull Node node) {
        return new HDInsightRootModuleImpl(node);
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState) throws AzureCmdException {
        synchronized (this) { //todo???
            removeAllChildNodes();
            TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerHDInsightNodeExpand, null, null);

            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails(getProject());

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }
    }

    @Override
    public void refreshWithoutAsync() {
        synchronized (this) {
            removeAllChildNodes();
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(getProject());

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }

    }

}