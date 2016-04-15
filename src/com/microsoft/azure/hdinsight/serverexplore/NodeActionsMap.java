package com.microsoft.azure.hdinsight.serverexplore;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.azure.hdinsight.serverexplore.action.ManageSubscriptionsAction;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.node.Node;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions = new HashMap<>();

    static {
        node2Actions.put(HDInsightRootModule.class, new ImmutableList.Builder().add(ManageSubscriptionsAction.class).add(AddNewClusterAction.class).build());
    }
}