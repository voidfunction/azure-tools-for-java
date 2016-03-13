package com.microsoft.azureexplorer.actions;

import com.microsoft.azureexplorer.forms.CreateStorageAccountForm;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

@Name("Create Storage Account")
public class CreateStorageAccountAction extends NodeActionListener {

    private StorageModule storageModule;

    public CreateStorageAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // check if we have a valid subscription handy
        AzureManager apiManager = AzureManagerImpl.getManager();

        if (!apiManager.authenticated() && !apiManager.usingCertificate()) {
            DefaultLoader.getUIHelper().showException("Please configure an Azure subscription by right-clicking on the \"Azure\" " +
                    "node and selecting \"Manage subscriptions\".", null, "No Azure subscription found", false, true);
            return;
        }

        try {
            List<Subscription> subscriptions = apiManager.getSubscriptionList();

            if (subscriptions.isEmpty()) {
                DefaultLoader.getUIHelper().showException("No active Azure subscription was found. Please enable one more Azure " +
                                "subscriptions by right-clicking on the \"Azure\" " +
                                "node and selecting \"Manage subscriptions\".",
                        null, "No active Azure subscription found", false, true);
                return;
            }
        } catch (AzureCmdException e1) {
            DefaultLoader.getUIHelper().showException("An error occurred while creating the storage account.", e1, "Error creating storage account", false, true);
        }

        CreateStorageAccountForm createStorageAccountForm = new CreateStorageAccountForm(new Shell(), null);

        createStorageAccountForm.setOnCreate(new Runnable() {
            @Override
            public void run() {
                storageModule.load();
            }
        });
        createStorageAccountForm.open();
    }
}
