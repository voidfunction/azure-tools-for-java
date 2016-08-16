/**
 * Copyright (c) Microsoft Corporation
 * <p>
 * All rights reserved.
 * <p>
 * MIT License
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azureexplorer.forms.createvm.arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import com.microsoft.azure.management.network.Network;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azureexplorer.forms.CreateArmStorageAccountForm;

public class SettingsStep extends WizardPage {
    private static final String CREATE_NEW = "<< Create new >>";

    private final String NONE = "(None)";

//    private final Node parent;
    private CreateVMWizard wizard;
    private Label resourceGrpLabel;
    private Button createNewRadioButton;
    private Button useExistingRadioButton;
    private Text resourceGrpField;
    private Combo resourceGrpCombo;
    // private JEditorPane imageDescriptionTextPane;
    private Label storageAccountLabel;
    private Combo storageComboBox;
    private Label networkLabel;
    private Combo networkComboBox;
    private Label subnetLabel;
    private Combo subnetComboBox;
    private Label pipLabel;
    private Combo pipCombo;
    private Label nsgLabel;
    private Combo nsgCombo;
    private Label availabilityLabel;
    private Combo availabilityCombo;

    private ComboViewer resourceGroupViewer;

    private List<Network> virtualNetworks;

    private Map<String, ArmStorageAccount> storageAccounts;
    private List<PublicIpAddress> publicIpAddresses;
    private List<NetworkSecurityGroup> networkSecurityGroups;

    public SettingsStep(CreateVMWizard wizard) {
        super("Create new Virtual Machine", "Associated resources", null);
        this.wizard = wizard;

        // model.configStepList(createVmStepsList, 3);

        // final ButtonGroup resourceGroup = new ButtonGroup();
        // resourceGroup.add(createNewRadioButton);
        // resourceGroup.add(useExistingRadioButton);
        // final ItemListener updateListener = new ItemListener() {
        // public void itemStateChanged(final ItemEvent e) {
        // final boolean isNewGroup = createNewRadioButton.isSelected();
        // resourceGrpField.setVisible(isNewGroup);
        // resourceGrpCombo.setVisible(!isNewGroup);
        // }
        // };
        // createNewRadioButton.addItemListener(updateListener);
        // createNewRadioButton.addItemListener(updateListener);
    }

    @Override
    public void createControl(Composite parent) {
        GridLayout gridLayout = new GridLayout(3, false);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        wizard.configStepList(container, 3);

        createSettingsPanel(container);
        //
        // imageDescription = wizard.createImageDescriptor(container);
        storageComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                validateNext();
            }
        });
        subnetComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                wizard.setSubnet((String) subnetComboBox.getText());
                validateNext();
            }
        });
        this.setControl(container);
    }

    private void createSettingsPanel(Composite container) {
        final Composite composite = new Composite(container, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        // gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 250;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        resourceGrpLabel = new Label(composite, SWT.LEFT);
        resourceGrpLabel.setText("Resource group:");
        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new RowLayout(SWT.HORIZONTAL));
        createNewRadioButton = new Button(group, SWT.RADIO);
        createNewRadioButton.setText("Create new");
        useExistingRadioButton = new Button(group, SWT.RADIO);
        useExistingRadioButton.setText("Use existing");

        SelectionListener updateListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                final boolean isNewGroup = createNewRadioButton.getSelection();
                resourceGrpField.setVisible(isNewGroup);
                resourceGrpCombo.setVisible(!isNewGroup);
            }
        };
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);

        resourceGrpField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);

        resourceGrpCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        resourceGroupViewer = new ComboViewer(resourceGrpCombo);
        resourceGroupViewer.setContentProvider(ArrayContentProvider.getInstance());

        storageAccountLabel = new Label(composite, SWT.LEFT);
        storageAccountLabel.setText("Storage account:");
        storageComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        storageComboBox.setLayoutData(gridData);

        networkLabel = new Label(composite, SWT.LEFT);
        networkLabel.setText("Virtual Network");
        networkComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        networkComboBox.setLayoutData(gridData);

        subnetLabel = new Label(composite, SWT.LEFT);
        subnetLabel.setText("Subnet:");
        subnetComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        subnetComboBox.setLayoutData(gridData);

        pipLabel = new Label(composite, SWT.LEFT);
        pipLabel.setText("Public IP address:");
        pipCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        pipCombo.setLayoutData(gridData);
        
        nsgLabel = new Label(composite, SWT.LEFT);
        nsgLabel.setText("Network security gtroup (firewall):");
        nsgCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nsgCombo.setLayoutData(gridData);

        availabilityLabel = new Label(composite, SWT.LEFT);
        availabilityLabel.setText("Availability set:");
        availabilityCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        availabilityCombo.setLayoutData(gridData);

    }

    @Override
    public String getTitle() {
        setPageComplete(false);

        // final VirtualMachineImage virtualMachineImage =
        // wizard.getVirtualMachineImage();
        // imageDescription.setText(wizard.getHtmlFromVMImage(virtualMachineImage));
        fillResourceGroups();
        retrieveStorageAccounts();
        retrieveVirtualNetworks();
        retrievePublicIpAddresses();

        return super.getTitle();
    }

    public void fillResourceGroups() {
        resourceGrpCombo.add("<Loading...>");

        DefaultLoader.getIdeHelper().runInBackground(null, "Loading resource groups...", false, true, "Loading resource groups...", new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ResourceGroup> resourceGroups = AzureArmManagerImpl.getManager(null).getResourceGroups(wizard.getSubscription().getId());

                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Vector<Object> vector = new Vector<Object>();
                            vector.addAll(resourceGroups);
                            resourceGroupViewer.setInput(vector);
                            if (resourceGroups.size() > 0) {
                                resourceGrpCombo.select(1);
                            }
                        }
                    });
                } catch (AzureCmdException e) {
                    PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
                            "An error occurred while loading the resource groups list.", e);
                }
            }
        });
    }

    private void retrieveVirtualNetworks() {
        DefaultLoader.getIdeHelper().runInBackground(null, "Loading virtual networks...", false, true, "Loading virtual networks...", new Runnable() {
            @Override
            public void run() {
                if (virtualNetworks == null) {
                    try {
                        virtualNetworks = AzureArmManagerImpl.getManager(null)
                                .getVirtualNetworks(wizard.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        virtualNetworks = null;
                        String msg = "An error occurred while attempting to retrieve the virtual networks list." + "\n" + e.getMessage();
                        PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                    }
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        networkComboBox.removeAll();
                        networkComboBox.add(CREATE_NEW);
                        for (Network network : filterVN()) {
                            networkComboBox.add(network.name());
                            networkComboBox.setData(network.name(), network);
                        }
                        networkComboBox.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                if (CREATE_NEW.equals(networkComboBox.getText())) {
//				                        showNewVirtualNetworkForm();
                                } else if ((Network) networkComboBox.getData(networkComboBox.getText()) != null) {
                                    Network network = (Network) networkComboBox.getData(networkComboBox.getText());
                                    wizard.setVirtualNetwork(network);


                                    subnetComboBox.removeAll();

                                    for (String subnet : network.subnets().keySet()) {
                                        subnetComboBox.add(subnet);
                                    }

                                }


                            }
                        });
                        if (virtualNetworks == null) {
                            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    networkComboBox.setItems(new String[]{CREATE_NEW, "<Loading...>"});
                                    subnetComboBox.removeAll();
                                    subnetComboBox.setEnabled(false);
                                }
                            });
                            networkComboBox.addSelectionListener(new SelectionAdapter() {
                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    if (CREATE_NEW.equals(networkComboBox.getText())) {
//                        showNewVirtualNetworkForm();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private List<Network> filterVN() {
        List<Network> filteredNetworks = new ArrayList<>();

        for (Network network : virtualNetworks) {
            if (network.region().equals(wizard.getRegion())) {
                filteredNetworks.add(network);
            }
        }
        return filteredNetworks;
    }

    private void retrieveStorageAccounts() {
//    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage accounts...", false) {
//        @Override
//        public void run(@NotNull ProgressIndicator progressIndicator) {
//            progressIndicator.setIndeterminate(true);
//            if (storageAccounts == null) {
//                try {
//                    java.util.List<ArmStorageAccount> accounts = AzureArmManagerImpl.getManager(project).getStorageAccounts(model.getSubscription().getId());
//                    storageAccounts = new TreeMap<String, ArmStorageAccount>();
//
//                    for (ArmStorageAccount storageAccount : accounts) {
//                        storageAccounts.put(storageAccount.getName(), storageAccount);
//                    }
//                } catch (AzureCmdException e) {
//                    storageAccounts = null;
//                    String msg = "An error occurred while attempting to retrieve the storage accounts list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
//                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
//                }
//            }
//            refreshStorageAccounts(null);
//        }
//    });
//
//    if (storageAccounts == null) {
//        final DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(new String[]{CREATE_NEW, "<Loading...>"}) {
//            @Override
//            public void setSelectedItem(Object o) {
//                if (CREATE_NEW.equals(o)) {
//                    showNewStorageForm();
//                } else {
//                    super.setSelectedItem(o);
//                }
//            }
//        };
//
//        loadingSAModel.setSelectedItem(null);
//
//        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
//            @Override
//            public void run() {
//                storageComboBox.setModel(loadingSAModel);
//            }
//        }, ModalityState.any());
//    }
    }

    private void fillStorage() {
//    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
//        @Override
//        public void run() {
//            ArmStorageAccount selectedSA = model.getStorageAccount();
//            if (selectedSA != null && !storageAccounts.containsKey(selectedSA.getName())) {
//                storageAccounts.put(selectedSA.getName(), selectedSA);
//            }
//            refreshStorageAccounts(selectedSA);
//        }
//    });
    }

    private void refreshStorageAccounts(final ArmStorageAccount selectedSA) {
//    final DefaultComboBoxModel refreshedSAModel = getStorageAccountModel(selectedSA);
//
//    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
//        @Override
//        public void run() {
//            storageComboBox.setModel(refreshedSAModel);
//            model.getCurrentNavigationState().NEXT.setEnabled(selectedSA != null);
//        }
//    }, ModalityState.any());
    }

//    private DefaultComboBoxModel getStorageAccountModel(ArmStorageAccount selectedSA) {
//    Vector<ArmStorageAccount> accounts = filterSA();
//
//    final DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
//        @Override
//        public void setSelectedItem(Object o) {
//            if (CREATE_NEW.equals(o)) {
//                showNewStorageForm();
//            } else {
//                super.setSelectedItem(o);
//                model.setStorageAccount((ArmStorageAccount) o);
//            }
//        }
//    };
//
//    refreshedSAModel.insertElementAt(CREATE_NEW, 0);
//
//    if (accounts.contains(selectedSA)) {
//        refreshedSAModel.setSelectedItem(selectedSA);
//    } else {
//        refreshedSAModel.setSelectedItem(null);
//        model.setStorageAccount(null);
//    }
//
//    return refreshedSAModel;
//    }

    private Vector<ArmStorageAccount> filterSA() {
        Vector<ArmStorageAccount> filteredStorageAccounts = new Vector<>();

        for (ArmStorageAccount storageAccount : storageAccounts.values()) {
            // VM and storage account need to be in the same region; only general purpose accounts support page blobs, so only they can be used to create vm
            if (storageAccount.getLocation().equals(wizard.getRegion().toString()) && storageAccount.getStorageAccount().kind() == Kind.STORAGE) {
                filteredStorageAccounts.add(storageAccount);
            }
        }
        return filteredStorageAccounts;
    }

    private void retrievePublicIpAddresses() {
//    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading public ip addresses...", false) {
//        @Override
//        public void run(@NotNull ProgressIndicator progressIndicator) {
//            progressIndicator.setIndeterminate(true);
//            if (publicIpAddresses == null) {
//                try {
//                    publicIpAddresses = AzureArmManagerImpl.getManager(project).getPublicIpAddresses(model.getSubscription().getId());
//                } catch (AzureCmdException e) {
//                    publicIpAddresses = null;
//                    String msg = "An error occurred while attempting to retrieve public ip addresses list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
//                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
//                }
//            }
//            ApplicationManager.getApplication().invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    pipCombo.setModel(getPipAddressModel(model.getPublicIpAddress()));
//                }
//            });
//        }
//    });
//
//    if (publicIpAddresses == null) {
//        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
//            @Override
//            public void run() {
//                final DefaultComboBoxModel loadingPipModel = new DefaultComboBoxModel(new String[]{NONE, CREATE_NEW, "<Loading...>"}) {
//                    @Override
//                    public void setSelectedItem(Object o) {
//                        super.setSelectedItem(o);
//                        if (CREATE_NEW.equals(o)) {
////                showNewPipForm();
//                        } else if (NONE.equals(o)) {
//                            model.setPublicIpAddress(null);
//                        } else {
////                            model.setVirtualNetwork((Network) o);
//                        }
//                    }
//                };
//                loadingPipModel.setSelectedItem(null);
//                pipCombo.setModel(loadingPipModel);
//            }
//        }, ModalityState.any());
//    }
    }

//    private DefaultComboBoxModel getPipAddressModel(PublicIpAddress selectedPip) {
//    DefaultComboBoxModel refreshedPipModel = new DefaultComboBoxModel(filterPip().toArray()) {
//        @Override
//        public void setSelectedItem(final Object o) {
//            super.setSelectedItem(o);
//            if (NONE.equals(o)) {
//                model.setPublicIpAddress(null);
//            } else if (CREATE_NEW.equals(o)) {
////                showNewPipForm();
//            } else if (o instanceof PublicIpAddress) {
//                model.setPublicIpAddress((PublicIpAddress) o);
//            } else {
//                model.setPublicIpAddress(null);
//            }
//        }
//    };
//    refreshedPipModel.insertElementAt(NONE, 0);
//    refreshedPipModel.insertElementAt(CREATE_NEW, 1);
//
//    if (selectedPip != null && publicIpAddresses.contains(selectedPip)) {
//        refreshedPipModel.setSelectedItem(selectedPip);
//    } else {
//        model.setPublicIpAddress(null);
//        refreshedPipModel.setSelectedItem(NONE);
//    }
//
//    return refreshedPipModel;
//    }

    private Vector<PublicIpAddress> filterPip() {
        Vector<PublicIpAddress> filteredPips = new Vector<>();

        for (PublicIpAddress publicIpAddress : publicIpAddresses) {
            // VM and public ip address need to be in the same region
            if (publicIpAddress.region().equals(wizard.getRegion())) {
                filteredPips.add(publicIpAddress);
            }
        }
        return filteredPips;
    }

    private void retrieveNetworkSecurityGroups() {
//    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading network security groups...", false) {
//        @Override
//        public void run(@NotNull ProgressIndicator progressIndicator) {
//            progressIndicator.setIndeterminate(true);
//            if (networkSecurityGroups == null) {
//                try {
//                    networkSecurityGroups = AzureArmManagerImpl.getManager(project).getNetworkSecurityGroups(model.getSubscription().getId());
//                } catch (AzureCmdException e) {
//                    networkSecurityGroups = null;
//                    String msg = "An error occurred while attempting to retrieve network security groups list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
//                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
//                }
//            }
//            ApplicationManager.getApplication().invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    nsgCombo.setModel(getNsgModel(model.getNetworkSecurityGroup()));
//                }
//            });
//        }
//    });
//
//    if (networkSecurityGroups == null) {
//        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
//            @Override
//            public void run() {
//                final DefaultComboBoxModel loadingNsgModel = new DefaultComboBoxModel(new String[]{NONE, "<Loading...>"}) {
//                    @Override
//                    public void setSelectedItem(Object o) {
//                        super.setSelectedItem(o);
//                        if (NONE.equals(o)) {
//                            model.setNetworkSecurityGroup(null);
//                        } else {
////                            model.setVirtualNetwork((Network) o);
//                        }
//                    }
//                };
//                loadingNsgModel.setSelectedItem(null);
//                nsgCombo.setModel(loadingNsgModel);
//            }
//        }, ModalityState.any());
//    }
    }

//    private DefaultComboBoxModel getNsgModel(NetworkSecurityGroup selectedNsg) {
//    DefaultComboBoxModel refreshedNsgModel = new DefaultComboBoxModel(filterNsg().toArray()) {
//        @Override
//        public void setSelectedItem(final Object o) {
//            super.setSelectedItem(o);
//            if (NONE.equals(o)) {
//                model.setNetworkSecurityGroup(null);
//            } else if (o instanceof NetworkSecurityGroup) {
//                model.setNetworkSecurityGroup((NetworkSecurityGroup) o);
//            } else {
//                model.setNetworkSecurityGroup(null);
//            }
//        }
//    };
//    refreshedNsgModel.insertElementAt(NONE, 0);
//
//    if (selectedNsg != null && networkSecurityGroups.contains(selectedNsg)) {
//        refreshedNsgModel.setSelectedItem(selectedNsg);
//    } else {
//        model.setNetworkSecurityGroup(null);
//        refreshedNsgModel.setSelectedItem(NONE);
//    }
//    return refreshedNsgModel;
//    }

    private Vector<NetworkSecurityGroup> filterNsg() {
        Vector<NetworkSecurityGroup> filteredNsgs = new Vector<>();

        for (NetworkSecurityGroup nsg : networkSecurityGroups) {
            // VM and network security group
            if (nsg.region().equals(wizard.getRegion())) {
                filteredNsgs.add(nsg);
            }
        }
        return filteredNsgs;
    }

    private void fillAvailabilitySets() {
//    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
//        @Override
//        public void run() {
//            availabilityComboBox.setModel(new DefaultComboBoxModel(new String[]{}));
//        }
//    }, ModalityState.any());
    }

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(PluginUtil.getParentShell(), wizard.getSubscription());

		form.setOnCreate(new Runnable() {
			@Override
			public void run() {
				ArmStorageAccount newStorageAccount = form.getStorageAccount();
				if (newStorageAccount != null) {
					wizard.setStorageAccount(newStorageAccount);
					fillStorage();
				}
			}
		});

        form.open();
    }

    private void validateNext() {
    	setPageComplete(storageComboBox.getData(storageComboBox.getText()) instanceof ArmStorageAccount &&
                (!subnetComboBox.isEnabled() || subnetComboBox.getText() != null));
    }
}
