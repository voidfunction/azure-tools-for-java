package com.microsoft.azureexplorer.actions;

import com.microsoft.azureexplorer.forms.CreateBlobContainerForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

@Name("Create blob container")
public class CreateBlobContainer extends NodeActionListener {
    private BlobModule blobModule;

    public CreateBlobContainer(BlobModule blobModule) {
        this.blobModule = blobModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateBlobContainerForm form = new CreateBlobContainerForm(PluginUtil.getParentShell(), blobModule.getStorageAccount());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                blobModule.getParent().removeAllChildNodes();
                ((ClientStorageNode) blobModule.getParent()).load();
            }
        });
        form.open();
    }
}