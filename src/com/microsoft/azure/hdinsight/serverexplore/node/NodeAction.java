package com.microsoft.azure.hdinsight.serverexplore.node;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.List;

public class NodeAction {
    private String name;
    private boolean enabled = true;
    private List<NodeActionListener> listeners = new ArrayList<NodeActionListener>();
    private Node node; // the node with which this action is associated

    public NodeAction(Node node, String name) {
        this.node = node;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addListener(NodeActionListener listener) {
        listeners.add(listener);
    }

    public List<NodeActionListener> getListeners() {
        return listeners;
    }

    public void fireNodeActionEvent() {
        if (!listeners.isEmpty()) {
            final NodeActionEvent event = new NodeActionEvent(this);
            for (final NodeActionListener listener : listeners) {
                listener.beforeActionPerformed(event);
                Futures.addCallback(listener.actionPerformedAsync(event), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listener.afterActionPerformed(event);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        listener.afterActionPerformed(event);
                    }
                });
            }
        }
    }

    public Node getNode() {
        return node;
    }

    public boolean isEnabled() {
        // if the node to which this action is attached is in a
        // "loading" state then we disable the action regardless
        // of what "enabled" is
        return !node.isLoading() && enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

