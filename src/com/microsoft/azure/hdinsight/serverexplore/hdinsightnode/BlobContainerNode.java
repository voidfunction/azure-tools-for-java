package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.storage.BlobContainer;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.node.Node;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionEvent;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BlobContainerNode extends Node {
    private static final String CONTAINER_MODULE_ID = BlobContainerNode.class.getName();
    private static final String ICON_PATH = CommonConst.BlobContainerIConPath;
    private static final String DEFAULT_CONTAINER_FLAG = "(default)";

    private StorageAccount storageAccount;
    private BlobContainer blobContainer;

    public BlobContainerNode(Node parent, StorageAccount storageAccount, BlobContainer blobContainer) {
        this(parent, storageAccount, blobContainer, false);
    }

    public BlobContainerNode(Node parent, StorageAccount storageAccount, BlobContainer blobContainer, boolean isDefaultContainer) {
        super(CONTAINER_MODULE_ID, isDefaultContainer ? blobContainer.getName() + DEFAULT_CONTAINER_FLAG : blobContainer.getName(), parent, ICON_PATH);
        this.storageAccount = storageAccount;
        this.blobContainer = blobContainer;
    }

    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getIdeHelper().refreshBlobs(getProject(), storageAccount, blobContainer);
        }
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerContainerOpen, null, null);
        final Object openedFile = DefaultLoader.getIdeHelper().getOpenedFile(getProject(), storageAccount, blobContainer);
        if (openedFile == null) {
            DefaultLoader.getIdeHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", CommonConst.BlobContainerIConPath);
        } else {
            DefaultLoader.getIdeHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Blob Container", ViewBlobContainer.class
                );
    }
}
