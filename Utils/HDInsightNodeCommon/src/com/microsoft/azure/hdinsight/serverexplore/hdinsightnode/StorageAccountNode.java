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
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManagerImpl;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class StorageAccountNode extends AzureRefreshableNode {
    private static final String STORAGE_ACCOUNT_MODULE_ID = StorageAccountNode.class.getName();
    private static final String ICON_PATH = CommonConst.StorageAccountIConPath;
    private static final String DEFAULT_STORAGE_FLAG = "(default)";

    private HDStorageAccount storageAccount;

    public StorageAccountNode(Node parent, HDStorageAccount storageAccount, boolean isDefaultStorageAccount) {
        super(STORAGE_ACCOUNT_MODULE_ID, isDefaultStorageAccount ? storageAccount.getName() + DEFAULT_STORAGE_FLAG : storageAccount.getName(), parent, ICON_PATH);
        this.storageAccount = storageAccount;
        load();
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();
        //TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerStorageAccountExpand, null, null);
        try {
            String defaultContainer = storageAccount.getDefaultContainer();
            List<BlobContainer> containerList = StorageClientSDKManagerImpl.getManager().getBlobContainers(storageAccount);
            for (BlobContainer blobContainer : containerList) {
                addChildNode(new BlobContainerNode(this, storageAccount, blobContainer, !StringHelper.isNullOrWhiteSpace(defaultContainer) && defaultContainer.equals(blobContainer.getName())));
            }
        } catch (AzureCmdException ex) {
            throw new AzureCmdException(ex.getMessage(), ex);
        }
    }
}


