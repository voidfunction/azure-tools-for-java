package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class ClusterNode extends AzureRefreshableNode {
    private static final String CLUSTER_MODULE_ID = ClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    private IClusterDetail clusterDetail;

    public ClusterNode(Node parent, IClusterDetail clusterDetail) {
        super(CLUSTER_MODULE_ID, getClusterNameWitStatus(clusterDetail), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.loadActions();
        this.load();
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Open Spark History UI", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String sparkHistoryUrl = String.format("https://%s.azurehdinsight.net/sparkhistory", clusterDetail.getName());
                openUrlLink(sparkHistoryUrl);
            }
        });

        addAction("Open Cluster Management Portal(Ambari)", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                String ambariUrl = String.format(CommonConstant.default_cluster_endpoint, clusterDetail.getName());
                openUrlLink(ambariUrl);
            }
        });

        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Jupyter Notebook", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String jupyterUrl = String.format("https://%s.azurehdinsight.net/jupyter/tree", clusterDetail.getName());
                    openUrlLink(jupyterUrl);
                }
            });

            addAction("Open Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String resourceGroupName = clusterDetail.getResourceGroup();
                    if (resourceGroupName != null) {
                        String webPortHttpLink = String.format("https://portal.azure.com/#resource/subscriptions/%s/resourcegroups/%s/providers/Microsoft.HDInsight/clusters/%s",
                                clusterDetail.getSubscription().getId(),
                                resourceGroupName,
                                clusterDetail.getName());
                        openUrlLink(webPortHttpLink);
                    } else {
                        DefaultLoader.getUIHelper().showError("Failed to get resource group name.", "HDInsight Explorer");
                    }
                }
            });
        }

        if (clusterDetail instanceof HDInsightAdditionalClusterDetail) {
            addAction("Delete", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    int exitCode = JOptionPane.showConfirmDialog(null, "Do you really want to delete the HDInsight cluster?", "Delete HDInsight Cluster", JOptionPane.OK_CANCEL_OPTION);
                    if(exitCode == JOptionPane.OK_OPTION) {
                        ClusterManagerEx.getInstance().removeHDInsightAdditionalCluster((HDInsightAdditionalClusterDetail)clusterDetail);
                    }
                }
            });
        }
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            {
        removeAllChildNodes();
        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerSparkNodeExpand, null, null);
        RefreshableNode storageAccountNode = new StorageAccountFolderNode(this, clusterDetail);
        addChildNode(storageAccountNode);
    }

    private static String getClusterNameWitStatus(IClusterDetail clusterDetail) {
        String state = clusterDetail.getState();
        if(!StringHelper.isNullOrWhiteSpace(state) && !state.equalsIgnoreCase("Running")) {
            return String.format("%s (State:%s)", clusterDetail.getName(), state);
        }
        return clusterDetail.getName();
    }

    private void openUrlLink(String linkUrl) {
        if (clusterDetail != null && !StringHelper.isNullOrWhiteSpace(clusterDetail.getName())) {
            try {
                Desktop.getDesktop().browse(new URI(linkUrl));
            } catch (Exception exception) {
                DefaultLoader.getUIHelper().showError(exception.getMessage(), "HDInsight Explorer");
            }
        }
    }
}
