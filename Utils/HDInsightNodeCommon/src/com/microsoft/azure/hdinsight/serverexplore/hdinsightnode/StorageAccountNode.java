package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
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
        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerStorageAccountExpand, null, null);
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


