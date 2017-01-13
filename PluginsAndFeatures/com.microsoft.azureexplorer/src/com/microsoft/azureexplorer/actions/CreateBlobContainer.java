package com.microsoft.azureexplorer.actions;

import com.microsoft.azureexplorer.forms.CreateBlobContainerForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storagearm.StorageNode;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;

@Name("Create blob container")
public class CreateBlobContainer extends NodeActionListener {
    private RefreshableNode parent;

    public CreateBlobContainer(BlobModule parent) {
        this.parent = parent;
    }

    public CreateBlobContainer(StorageNode parent) {
        this.parent = parent;
    }
    
    @Override
    public void actionPerformed(NodeActionEvent e) {
    	String connectionString;
    	if (parent instanceof BlobModule) {
    		connectionString = (((BlobModule) parent).getStorageAccount()).getConnectionString();
    	} else {
    		connectionString = StorageClientSDKManager.getConnectionString(((StorageNode) parent).getStorageAccount());
    	}
        CreateBlobContainerForm form = new CreateBlobContainerForm(PluginUtil.getParentShell(), connectionString);

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                parent.removeAllChildNodes();
                parent.load();
            }
        });
        form.open();
    }
}