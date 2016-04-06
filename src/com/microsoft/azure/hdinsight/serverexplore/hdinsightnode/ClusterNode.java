package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.node.*;
import com.sun.istack.internal.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class ClusterNode extends HDInsightRefreshNode {
    private static final String CLUSTER_MODULE_ID = ClusterNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    private IClusterDetail clusterDetail;
    private Project project;

    public ClusterNode(Node parent, IClusterDetail clusterDetail, Project project) {
        super(CLUSTER_MODULE_ID, getClusterNameWitStatus(clusterDetail), parent, ICON_PATH, true);
        this.clusterDetail = clusterDetail;
        this.project = project;
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
                                clusterDetail.getSubscription().getSubscriptionId(),
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
                        ClusterManagerEx.getInstance().removeHDInsightAdditionalCluster(project, (HDInsightAdditionalClusterDetail)clusterDetail);
                    }
                }
            });
        }
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws HDExploreException {
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
