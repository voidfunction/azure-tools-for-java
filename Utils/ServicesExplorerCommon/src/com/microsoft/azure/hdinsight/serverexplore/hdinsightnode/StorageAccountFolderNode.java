package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageNode;

import java.util.List;

public class StorageAccountFolderNode extends AzureRefreshableNode {
    private static final String STORAGE_ACCOUNT_FOLDER_MODULE_ID = StorageAccountFolderNode.class.getName();
    private static final String STORAGE_ACCOUNT_NAME = "Storage Accounts";
    private static final String ICON_PATH = CommonConst.StorageAccountFoldIConPath;

    private IClusterDetail clusterDetail;
    public StorageAccountFolderNode(Node parent, IClusterDetail clusterDetail) {
        super(STORAGE_ACCOUNT_FOLDER_MODULE_ID, STORAGE_ACCOUNT_NAME, parent, ICON_PATH);
        this.clusterDetail = clusterDetail;
        load();
    }

    @Override
    protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
            throws AzureCmdException {
        removeAllChildNodes();
        if (clusterDetail != null) {
            try {
                clusterDetail.getConfigurationInfo();
                addChildNode(new StorageAccountNode(this, clusterDetail.getStorageAccount(), true));
                List<HDStorageAccount> additionalStorageAccount = clusterDetail.getAdditionalStorageAccounts();
                if (additionalStorageAccount != null) {
                    for (HDStorageAccount account : additionalStorageAccount) {
                        addChildNode(new StorageNode(this, account, false));
                    }
                }
            } catch (Exception exception) {
                DefaultLoader.getUIHelper().showException(
                        "Failed to get HDInsight cluster configuration.", exception,
                        "HDInsight Explorer", false, true);
            }
        }
    }
}
