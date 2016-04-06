package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.serverexplore.AzureManagerImpl;
import com.microsoft.azure.hdinsight.serverexplore.node.EventHelper;
import com.microsoft.azure.hdinsight.serverexplore.node.Node;
import com.microsoft.azure.hdinsight.serverexplore.node.RefreshableNode;
import com.sun.istack.internal.NotNull;

public abstract class HDInsightRefreshNode extends RefreshableNode {
    public HDInsightRefreshNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public HDInsightRefreshNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void refreshItems()
            throws HDExploreException {
        EventHelper.runInterruptible(new EventHelper.EventHandler() {
            @Override
            public EventHelper.EventWaitHandle registerEvent()
                    throws HDExploreException {
                return AzureManagerImpl.getManager().registerSubscriptionsChanged();
            }

            @Override
            public void unregisterEvent(@NotNull EventHelper.EventWaitHandle waitHandle)
                    throws HDExploreException {
                AzureManagerImpl.getManager().unregisterSubscriptionsChanged(waitHandle);
            }

            @Override
            public void interruptibleAction(@NotNull EventHelper.EventStateHandle eventState)
                    throws HDExploreException {
                refresh(eventState);
            }

            @Override
            public void eventTriggeredAction()
                    throws HDExploreException {
            }
        });
    }

    protected abstract void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws HDExploreException;
}