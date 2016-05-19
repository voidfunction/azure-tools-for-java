package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public abstract class HDInsightRootModule extends AzureRefreshableNode {

    public HDInsightRootModule(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public HDInsightRootModule(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    public abstract HDInsightRootModule getNewNode(Node parent);

    public abstract void refreshWithoutAsync();
}