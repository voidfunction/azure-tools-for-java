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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.Location;
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
    private JComboBox resourceGrpCombo;
    private JComboBox accoountKindCombo;

    private Runnable onCreate;
    private Subscription subscription;
    private ArmStorageAccount storageAccount;
    private Project project;

    private boolean isLoading = true;

    private final String PRICING_LINK = "http://go.microsoft.com/fwlink/?LinkID=400838";

    public CreateArmStorageAccountForm(Project project) {
        super(project, true);

        this.project = project;

        setModal(true);
        setTitle("Create Storage Account");

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
        createNewRadioButton.addItemListener(updateListener);

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

        accoountKindCombo.setModel(new DefaultComboBoxModel(Kind.values()));
        accoountKindCombo.setRenderer(new ListCellRendererWrapper<Kind>() {
            @Override
            public void customize(JList jList, Kind kind, int i, boolean b, boolean b1) {
                setText(kind == Kind.STORAGE ? "General purpose" : "Blob storage");
            }
        });

        replicationComboBox.setModel(new DefaultComboBoxModel(ReplicationTypes.values()));
        replicationComboBox.setRenderer(new ListCellRendererWrapper<ReplicationTypes>() {
            @Override
            public void customize(JList jList, ReplicationTypes replicationTypes, int i, boolean b, boolean b1) {
                setText(replicationTypes.getDescription());
            }
        });

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

        final String name = nameTextField.getText();
        final String region = regionComboBox.getSelectedItem().toString();
        final String replication = replicationComboBox.getSelectedItem().toString();
        final boolean isNewResourceGroup = createNewRadioButton.isSelected();
        final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();

        DefaultLoader.getIdeHelper().runInBackground(project, "Creating storage account", false, true, "Creating storage account...", new Runnable() {
            @Override
            public void run() {
                try {
                    storageAccount = new ArmStorageAccount(name, subscription.getId(), null);
                    storageAccount.setType(replication);
                    storageAccount.setLocation(region);
                    storageAccount.setNewResourceGroup(isNewResourceGroup);
                    storageAccount.setResourceGroupName(resourceGroupName);
                    storageAccount.setKind((Kind) accoountKindCombo.getSelectedItem());

                    storageAccount = AzureArmManagerImpl.getManager(project).createStorageAccount(storageAccount);
//                    AzureManagerImpl.getManager(project).refreshStorageAccountInformation(storageAccount);

                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreate != null) {
                                onCreate.run();
                            }
                        }
                    });
                } catch (AzureCmdException e) {
                    storageAccount = null;
                    String msg = "An error occurred while attempting to create the specified storage account." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });

        close(DialogWrapper.OK_EXIT_CODE, true);
    }


    public void fillFields(final Subscription subscription) {
        final CreateArmStorageAccountForm createStorageAccountForm = this;
        if (subscription == null) {
            try {
                subscriptionComboBox.setEnabled(true);

                java.util.List<Subscription> fullSubscriptionList = AzureManagerImpl.getManager(project).getFullSubscriptionList();
                subscriptionComboBox.setModel(new DefaultComboBoxModel(new Vector<Subscription>(fullSubscriptionList)));
                subscriptionComboBox.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent itemEvent) {
                        createStorageAccountForm.subscription = (Subscription) itemEvent.getItem();
                        loadRegions();
                        loadGroups();
                    }
                });

                if (fullSubscriptionList.size() > 0) {
                    createStorageAccountForm.subscription = fullSubscriptionList.get(0);
                    loadRegions();
                    loadGroups();
                }
            } catch (AzureCmdException e) {
                String msg = "An error occurred while attempting to get subscriptions." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
            }
        } else {
            this.subscription = subscription;
            subscriptionComboBox.addItem(subscription.getName());

            loadRegions();
        }
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public ArmStorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void loadRegions() {
        isLoading = true;

        regionComboBox.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading regions...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    java.util.List<Location> locations = AzureManagerImpl.getManager(project).getLocations(subscription.getId().toString());

                    final Vector<Object> vector = new Vector<Object>();
                    vector.add("Regions");
                    vector.addAll(locations);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;

                            validateEmptyFields();

                            regionComboBox.removeAllItems();
                            regionComboBox.setModel(new DefaultComboBoxModel(vector) {
                                public void setSelectedItem(Object o) {
                                    if (!(o instanceof String)) {
                                        super.setSelectedItem(o);
                                    }
                                }
                            });

                            regionComboBox.setSelectedIndex(1);
                        }
                    });
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to load the regions list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }

    public void loadGroups() {
        isLoading = true;

        resourceGrpCombo.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading resource groups...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    List<ResourceGroup> resourceGroups = AzureArmManagerImpl.getManager(project).getResourceGroups(subscription.getId());

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
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to load resource groups list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }
}
