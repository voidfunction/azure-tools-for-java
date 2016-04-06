package com.microsoft.azure.hdinsight.serverexplore.node;

import java.util.EventObject;

public class NodeActionEvent extends EventObject {
    public NodeActionEvent(NodeAction action) {
        super(action);
    }

    public NodeAction getAction() {
        return (NodeAction) getSource();
    }
}
