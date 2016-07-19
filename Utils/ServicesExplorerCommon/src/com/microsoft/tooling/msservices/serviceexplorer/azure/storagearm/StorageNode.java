/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer.azure.storagearm;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.Map;

public class StorageNode extends AzureRefreshableNode {
    private static final String STORAGE_ACCOUNT_ICON_PATH = "storageaccount.png";

    private final StorageAccount storageAccount;
    private String subscriptionId;

    public StorageNode(Node parent, String subscriptionId, StorageAccount storageAccount) {
        super(storageAccount.name(), storageAccount.name(), parent, STORAGE_ACCOUNT_ICON_PATH,  true);

        this.subscriptionId = subscriptionId;
        this.storageAccount = storageAccount;

        loadActions();
    }

    public class DeleteStorageAccountAction extends AzureNodeActionPromptListener {
        public DeleteStorageAccountAction() {
            super(StorageNode.this,
                    String.format("This operation will delete storage account %s.\nAre you sure you want to continue?", storageAccount.name()),
                    "Deleting Storage Account");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e, @NotNull EventHelper.EventStateHandle stateHandle)
                throws AzureCmdException {
            try {
                AzureArmManagerImpl.getManager(getProject()).deleteStorageAccount(subscriptionId, storageAccount);
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(StorageNode.this);
                    }
                });
            } catch (AzureCmdException ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete storage account.", ex,
                        "MS Services - Error Deleting Storage Account", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();

        fillChildren(eventState);
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Delete", new DeleteStorageAccountAction());
        return super.initActions();
    }

    protected void fillChildren(@NotNull EventHelper.EventStateHandle eventState) {
//        BlobModule blobsNode = new BlobModule(this, clientStorageAccount);
//        blobsNode.load();
//
//        if (eventState.isEventTriggered()) {
//            return;
//        }
//
//        addChildNode(blobsNode);
//
//        QueueModule queueNode = new QueueModule(this, clientStorageAccount);
//        queueNode.load();
//
//        if (eventState.isEventTriggered()) {
//            return;
//        }
//
//        addChildNode(queueNode);
//
//        TableModule tableNode = new TableModule(this, clientStorageAccount);
//        tableNode.load();
//
//        if (eventState.isEventTriggered()) {
//            return;
//        }
//
//        addChildNode(tableNode);
    }
}