package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StringHelper;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.sdk.storage.BlobContainer;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageClientImpl;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.serverexplore.node.*;
import com.sun.istack.internal.NotNull;

import java.util.List;

public class StorageAccountNode extends HDInsightRefreshNode {
    private static final String STORAGE_ACCOUNT_MODULE_ID = StorageAccountNode.class.getName();
    private static final String ICON_PATH = CommonConst.StorageAccountIConPath;
    private static final String DEFAULT_STORAGE_FLAG = "(default)";

    private StorageAccount storageAccount;

    public StorageAccountNode(Node parent, StorageAccount storageAccount, boolean isDefaultStorageAccount) {
        super(STORAGE_ACCOUNT_MODULE_ID, isDefaultStorageAccount ? storageAccount.getStorageName() + DEFAULT_STORAGE_FLAG : storageAccount.getStorageName(), parent, ICON_PATH);
        this.storageAccount = storageAccount;
        load();
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws HDExploreException {
        removeAllChildNodes();
        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerStorageAccountExpand, null, null);
        try {
            String defaultContainer = storageAccount.getDefaultContainer();
            List<BlobContainer> containerList = StorageClientImpl.getInstance().getBlobContainers(storageAccount);
            for (BlobContainer blobContainer : containerList) {
                addChildNode(new BlobContainerNode(this, storageAccount, blobContainer, !StringHelper.isNullOrWhiteSpace(defaultContainer) && defaultContainer.equals(blobContainer.getName())));
            }
        } catch (HDIException hdiException) {
            throw new HDExploreException(hdiException.getMessage(), hdiException);
        }
    }
}


