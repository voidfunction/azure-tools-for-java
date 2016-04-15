package com.microsoft.azure.hdinsight.serverexplore.node;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;

import java.util.EventListener;

public abstract class NodeActionListener implements EventListener {
    protected static String name;

    public NodeActionListener() {
        // need a nullary constructor defined in order for
        // Class.newInstance to work on sub-classes
    }

    public NodeActionListener(Node node) {
    }

    protected void beforeActionPerformed(NodeActionEvent e) {
        // mark node as loading
        e.getAction().getNode().setLoading(true);
    }

    protected abstract void actionPerformed(NodeActionEvent e)
            throws HDExploreException;

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        try {
            actionPerformed(e);
            return Futures.immediateFuture(null);
        } catch (HDExploreException ex) {
            return Futures.immediateFailedFuture(ex);
        }
    }

    protected void afterActionPerformed(NodeActionEvent e) {
        // mark node as done loading
        e.getAction().getNode().setLoading(false);
    }
}
