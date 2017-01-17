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
package com.microsoft.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuTier;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateArmStorageAccountForm extends DialogWrapper {
    private JPanel contentPane;
    private JComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox regionComboBox;
    private JComboBox replicationComboBox;
    private JLabel pricingLabel;
    private JLabel userInfoLabel;
    private JRadioButton createNewRadioButton;
    private JRadioButton useExistingRadioButton;
    private JTextField resourceGrpField;
    //private JComboBox resourceGrpCombo;
    private JComboBox accoountKindCombo;
    private JComboBox performanceComboBox;
    private JComboBox accessTeirComboBox;
    private JLabel accessTierLabel;
    private JComboBox encriptonComboBox;
    private JComboBox resourceGrpCombo;
    private JLabel encriptonLabel;

    private Runnable onCreate;
    private SubscriptionDetail subscription;
    private StorageAccount storageAccount;
    private Project project;

    private boolean isLoading = true;

    private static final String PRICING_LINK = "http://go.microsoft.com/fwlink/?LinkID=400838";

    public CreateArmStorageAccountForm(Project project) {
        super(project, true);

        this.project = project;

        setModal(true);
        setTitle("Create Storage Account");

        // this opton is not supported by SDK yet
        encriptonComboBox.setVisible(false);
        encriptonLabel.setVisible(false);

        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewRadioButton);
        resourceGroup.add(useExistingRadioButton);
        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isNewGroup = createNewRadioButton.isSelected();
                resourceGrpField.setVisible(isNewGroup);
                resourceGrpCombo.setVisible(!isNewGroup);
            }
        };
        createNewRadioButton.addItemListener(updateListener);
        resourceGrpCombo.setVisible(false);

        pricingLabel.addMouseListener(new LinkListener(PRICING_LINK));

        regionComboBox.setRenderer(new ListCellRendererWrapper<Object>() {

            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (!(o instanceof String) && o != null) {
                    setText("  " + o.toString());
                }
            }
        });
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        };

        nameTextField.getDocument().addDocumentListener(docListener);
        resourceGrpField.getDocument().addDocumentListener(docListener);

        ItemListener validateListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateEmptyFields();
            }
        };

        regionComboBox.addItemListener(validateListener);
        resourceGrpCombo.addItemListener(validateListener);


        if (AzureManagerImpl.getManager(project).authenticated()) {
            String upn = AzureManagerImpl.getManager(project).getUserInfo().getUniqueName();
            userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));
        } else {
            userInfoLabel.setText("");
        }

        accoountKindCombo.setRenderer(new ListCellRendererWrapper<Kind>() {
            @Override
            public void customize(JList jList, Kind kind, int i, boolean b, boolean b1) {
                setText(kind == Kind.STORAGE ? "General purpose" : "Blob storage");
            }
        });

        encriptonComboBox.setModel(new DefaultComboBoxModel(new Boolean[] {true, false}));
        encriptonComboBox.setRenderer(new ListCellRendererWrapper<Boolean>() {
            @Override
            public void customize(JList jList, Boolean aBoolean, int i, boolean b, boolean b1) {
                setText(aBoolean ? "Enabled" : "Disables");
            }
        });
        encriptonComboBox.setSelectedItem(Boolean.FALSE);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                nameTextField.getText().isEmpty() || regionComboBox.getSelectedObjects().length == 0
        || (createNewRadioButton.isSelected() && resourceGrpField.getText().trim().isEmpty())
        || (useExistingRadioButton.isSelected() && resourceGrpCombo.getSelectedObjects().length == 0));

        setOKActionEnabled(!isLoading && allFieldsCompleted);
    }


    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (nameTextField.getText().length() < 3
                || nameTextField.getText().length() > 24
                || !nameTextField.getText().matches("[a-z0-9]+")) {
            return new ValidationInfo("Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {

//        final String name = nameTextField.getText();
//        final String region = regionComboBox.getSelectedItem().toString();
//        final String replication = replicationComboBox.getSelectedItem().toString();
//        final boolean isNewResourceGroup = createNewRadioButton.isSelected();
//        final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
//        storageAccount = new ArmStorageAccount(name, subscription.getSubscriptionId(), null);
//        storageAccount.setType(replication);
//        storageAccount.setLocation(region);
//        storageAccount.setNewResourceGroup(isNewResourceGroup);
//        storageAccount.setResourceGroupName(resourceGroupName);
//        storageAccount.setKind((Kind) accoountKindCombo.getSelectedItem());
//        storageAccount.setAccessTier((AccessTier)accessTeirComboBox.getSelectedItem());
//        storageAccount.setEnableEncription((Boolean)encriptonComboBox.getSelectedItem());

        ProgressManager.getInstance().run(
            new Task.Modal(project, "Creating storage account", true) {
                @Override
                public void run(@com.microsoft.tooling.msservices.helpers.NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    boolean success = createStorageAccount();
                    if (success) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                close(DialogWrapper.OK_EXIT_CODE, true);
                            }
                        }, ModalityState.any());

                    }
                }
            }
        );
    }

    private boolean createStorageAccount() {
        try {
            boolean isNewResourceGroup = createNewRadioButton.isSelected();
            final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();
            storageAccount = AzureSDKManager.createStorageAccount(subscription.getSubscriptionId(), nameTextField.getText(), (Region) regionComboBox.getSelectedItem(),
                    isNewResourceGroup, resourceGroupName, (Kind) accoountKindCombo.getSelectedItem(), (AccessTier)accessTeirComboBox.getSelectedItem(),
                    (Boolean)encriptonComboBox.getSelectedItem(), replicationComboBox.getSelectedItem().toString());

            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (onCreate != null) {
                        onCreate.run();
                    }
                }
            });
            return true;
        } catch (Exception e) {
            storageAccount = null;
            String msg = "An error occurred while attempting to create the specified storage account in subscription " + subscription.getSubscriptionId() + ".<br>"
                    + String.format(message("webappExpMsg"), e.getCause());
            DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true);
            AzurePlugin.log(msg, e);
        }
        return false;
    }

    public void fillFields(final SubscriptionDetail subscription, Region region) {
        final CreateArmStorageAccountForm createStorageAccountForm = this;
        if (subscription == null) {
            loadRegions();
            accoountKindCombo.setModel(new DefaultComboBoxModel(Kind.values()));
            accoountKindCombo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        fillPerformanceComboBox();
                        fillReplicationTypes();
                        boolean isBlobKind = e.getItem().equals(Kind.BLOB_STORAGE);
                        accessTeirComboBox.setEnabled(isBlobKind);
                        accessTierLabel.setEnabled(isBlobKind);
                    }
                }
            });
            accessTeirComboBox.setModel(new DefaultComboBoxModel(AccessTier.values()));

            subscriptionComboBox.setEnabled(true);

            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
                List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
                final Vector<SubscriptionDetail> subscriptions = new Vector<SubscriptionDetail>(subscriptionDetails);
                subscriptionComboBox.setModel(new DefaultComboBoxModel(subscriptions));
                if (!subscriptions.isEmpty()) {
                    createStorageAccountForm.subscription = subscriptions.get(0);
//                    loadRegions();
                    loadGroups();
                }
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
            }

            subscriptionComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    createStorageAccountForm.subscription = (SubscriptionDetail) itemEvent.getItem();
//                        loadRegions();
                    loadGroups();
                }
            });
        } else { // if you create SA while creating VM
            this.subscription = subscription;
            subscriptionComboBox.addItem(subscription.getSubscriptionName());
            accoountKindCombo.addItem(Kind.STORAGE); // only General purpose accounts supported for VMs
            accoountKindCombo.setEnabled(false);
            regionComboBox.addItem(region);
            regionComboBox.setEnabled(false);
            loadGroups();
        }
        //performanceComboBox.setModel(new DefaultComboBoxModel(SkuTier.values()));
        fillPerformanceComboBox();
        performanceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillReplicationTypes();
                }
            }
        });

        replicationComboBox.setRenderer(new ListCellRendererWrapper<ReplicationTypes>() {
            @Override
            public void customize(JList jList, ReplicationTypes replicationTypes, int i, boolean b, boolean b1) {
                if (replicationTypes != null) {
                    setText(replicationTypes.getDescription());
                }
            }
        });
        fillReplicationTypes();
    }

    private void fillPerformanceComboBox() {
        if (accoountKindCombo.getSelectedItem().equals(Kind.BLOB_STORAGE)) {
            performanceComboBox.setModel(new DefaultComboBoxModel(new SkuTier[] {SkuTier.STANDARD}));
        } else {
            performanceComboBox.setModel(new DefaultComboBoxModel(SkuTier.values()));
        }
    }

    private void fillReplicationTypes() {
        if (performanceComboBox.getSelectedItem().equals(SkuTier.STANDARD)) {
            // Create storage account from Azure Explorer
            if (regionComboBox.isEnabled()) {
                if (accoountKindCombo.getSelectedItem().equals(Kind.BLOB_STORAGE)) {
                    replicationComboBox.setModel(
                            new DefaultComboBoxModel(new ReplicationTypes[] {
                                    ReplicationTypes.Standard_LRS,
                                    ReplicationTypes.Standard_GRS,
                                    ReplicationTypes.Standard_RAGRS}));

                } else {
                    replicationComboBox.setModel(
                            new DefaultComboBoxModel(new ReplicationTypes[] {
                                    ReplicationTypes.Standard_ZRS,
                                    ReplicationTypes.Standard_LRS,
                                    ReplicationTypes.Standard_GRS,
                                    ReplicationTypes.Standard_RAGRS}));
                }

            } else {
                // Create storage account from VM creation
                replicationComboBox.setModel(
                        new DefaultComboBoxModel(new ReplicationTypes[] {ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}));
            }
        } else {
            replicationComboBox.setModel(new DefaultComboBoxModel(new ReplicationTypes[] {ReplicationTypes.Premium_LRS}));
        }
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void loadRegions() {
        // todo: load regions from subscription
        regionComboBox.setModel(new DefaultComboBoxModel(Region.values()));
//        isLoading = true;
//
//        regionComboBox.addItem("<Loading...>");
//
//        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading regions...", false) {
//            @Override
//            public void run(@NotNull ProgressIndicator progressIndicator) {
//                progressIndicator.setIndeterminate(true);
//
//                try {
//                    java.util.List<Location> locations = AzureArmManagerImpl.getManager(project).getLocations(subscription.getId().toString());
//
//                    final Vector<Object> vector = new Vector<Object>();
//                    vector.addAll(locations);
//                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            isLoading = false;
//
//                            validateEmptyFields();
//
//                            regionComboBox.removeAllItems();
//                            regionComboBox.setModel(new DefaultComboBoxModel(vector) {
//                                public void setSelectedItem(Object o) {
//                                    if (!(o instanceof String)) {
//                                        super.setSelectedItem(o);
//                                    }
//                                }
//                            });
//
//                            regionComboBox.setSelectedIndex(1);
//                        }
//                    });
//                } catch (AzureCmdException e) {
//                    String msg = "An error occurred while attempting to load the regions list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
//                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
//                }
//            }
//        });
    }

    public void loadGroups() {
        isLoading = true;

        resourceGrpCombo.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading resource groups...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                    Azure azure = azureManager.getAzure(subscription.getSubscriptionId());
                    List<ResourceGroup> resourceGroups = azure.resourceGroups().list();

                    final Vector<Object> vector = new Vector<Object>();
                    vector.addAll(resourceGroups);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;

                            validateEmptyFields();

                            resourceGrpCombo.removeAllItems();
                            resourceGrpCombo.setModel(new DefaultComboBoxModel(vector) {
                                public void setSelectedItem(Object o) {
                                    if (!(o instanceof String)) {
                                        super.setSelectedItem(o);
                                    }
                                }
                            });
                            if (vector.size() > 0) {
                                resourceGrpCombo.setSelectedIndex(0);
                            }
                        }
                    });
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to load resource groups list." + "<br>" + String.format(message("webappExpMsg"), e.getMessage());
                    DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true);
                    AzurePlugin.log(msg, e);
                }
            }
        });
    }
}
