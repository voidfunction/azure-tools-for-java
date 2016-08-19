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
package com.microsoft.intellij.wizards.createarmvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.intellij.forms.CreateArmStorageAccountForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.VirtualMachine;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SettingsStep extends WizardStep<CreateVMWizardModel> {
    private static final String CREATE_NEW = "<< Create new >>";
    private final String NONE = "(None)";

    private final Node parent;
    private Project project;
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox storageComboBox;
    private JComboBox availabilityComboBox;
    private JComboBox networkComboBox;
    private JComboBox subnetComboBox;
    private JRadioButton createNewRadioButton;
    private JRadioButton useExistingRadioButton;
    private JTextField resourceGrpField;
    private JComboBox resourceGrpCombo;
    private JComboBox pipCombo;
    private JComboBox nsgCombo;

    private List<Network> virtualNetworks;

    private Map<String, ArmStorageAccount> storageAccounts;
    private List<PublicIpAddress> publicIpAddresses;
    private List<NetworkSecurityGroup> networkSecurityGroups;
    private List<AvailabilitySet> availabilitySets;

    public SettingsStep(final CreateVMWizardModel model, Project project, Node parent) {
        super("Settings", null, null);

        this.parent = parent;
        this.project = project;
        this.model = model;

        model.configStepList(createVmStepsList, 3);

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

        storageComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof StorageAccount) {
                    StorageAccount sa = (StorageAccount) o;
                    setText(String.format("%s (%s)", sa.getName(), sa.getLocation()));
                }
            }
        });

        storageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateNext();
            }
        });

        networkComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualNetwork) {
                    VirtualNetwork vn = (VirtualNetwork) o;
                    setText(String.format("%s (%s)", vn.getName(), vn.getLocation()));
                }
            }
        });

        subnetComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                model.setSubnet((String) subnetComboBox.getSelectedItem());
                validateNext();
            }
        });

        pipCombo.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof PublicIpAddress) {
                    PublicIpAddress pip = (PublicIpAddress) o;
                    setText(String.format("%s (%s)", pip.name(), pip.region()));
                }
            }
        });

        nsgCombo.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o instanceof NetworkSecurityGroup) {
                    setText(((NetworkSecurityGroup) o).name());
                }
            }
        });
    }

    private void fillResourceGroups() {
        try {
            resourceGrpCombo.setModel(
                    new DefaultComboBoxModel(AzureArmManagerImpl.getManager(project).getResourceGroups(model.getSubscription().getId()).toArray()));
        } catch (AzureCmdException ex) {
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), "Error loading resource groups", ex);
        }
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        model.getCurrentNavigationState().NEXT.setEnabled(false);

        fillResourceGroups();
        retrieveStorageAccounts();
        retrieveVirtualNetworks();
        retrievePublicIpAddresses();
        retrieveNetworkSecurityGroups();
        retrieveAvailabilitySets();

        return rootPanel;
    }

    private void retrieveVirtualNetworks() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading virtual networks...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                if (virtualNetworks == null) {
                    try {
                        virtualNetworks = AzureArmManagerImpl.getManager(project).getVirtualNetworks(model.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        virtualNetworks = null;
                        String msg = "An error occurred while attempting to retrieve the virtual networks list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                        return;
                    }
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        networkComboBox.setModel(getVirtualNetworkModel(model.getVirtualNetwork(), model.getSubnet()));
                    }
                });
            }
        });

        if (virtualNetworks == null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                      @Override
                                      public void run() {
                                          final DefaultComboBoxModel loadingVNModel = new DefaultComboBoxModel(new String[]{CREATE_NEW, "<Loading...>"}) {
                                              @Override
                                              public void setSelectedItem(Object o) {
                                                  if (CREATE_NEW.equals(o)) {
//                    showNewVirtualNetworkForm();
                                                  } else {
                                                      super.setSelectedItem(o);
                                                      model.setVirtualNetwork((Network) o);
                                                  }
                                              }
                                          };
                                          loadingVNModel.setSelectedItem(null);
                                          networkComboBox.setModel(loadingVNModel);

                                          subnetComboBox.removeAllItems();
                                          subnetComboBox.setEnabled(false);
                                      }
                                  }, ModalityState.any());
        }
    }

    private DefaultComboBoxModel getVirtualNetworkModel(Network selectedVN, final String selectedSN) {
        DefaultComboBoxModel refreshedVNModel = new DefaultComboBoxModel(filterVN().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                if (NONE.equals(o)) {
                    removeElement(o);
                    setSelectedItem(null);
                } else {
                    super.setSelectedItem(o);

                    if (o instanceof Network) {
                        model.setVirtualNetwork((Network) o);

                        if (getIndexOf(NONE) == -1) {
                            insertElementAt(NONE, 0);
                        }

                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                boolean validSubnet = false;

                                subnetComboBox.removeAllItems();

                                for (String subnet : ((Network) o).subnets().keySet()) {
                                    subnetComboBox.addItem(subnet);

                                    if (subnet.equals(selectedSN)) {
                                        validSubnet = true;
                                    }
                                }

                                if (validSubnet) {
                                    subnetComboBox.setSelectedItem(selectedSN);
                                } else {
                                    model.setSubnet(null);
                                    subnetComboBox.setSelectedItem(null);
                                }

                                subnetComboBox.setEnabled(true);
                            }
                        }, ModalityState.any());
                    } else {
                        model.setVirtualNetwork(null);

                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                subnetComboBox.removeAllItems();
                                subnetComboBox.setEnabled(false);
                            }
                        }, ModalityState.any());
                    }
                }
            }
        };

        if (selectedVN != null && virtualNetworks.contains(selectedVN)/* && (cascade || selectedCS != null)*/) {
            refreshedVNModel.setSelectedItem(selectedVN);
        } else {
            model.setVirtualNetwork(null);
            refreshedVNModel.setSelectedItem(null);
        }

        return refreshedVNModel;
    }

    private List<Network> filterVN() {
        List<Network> filteredNetworks = new ArrayList<>();

        for (Network network : virtualNetworks) {
            if (network.region().equals(model.getRegion())) {
                filteredNetworks.add(network);
            }
        }
        return filteredNetworks;
    }

    private void retrieveStorageAccounts() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading storage accounts...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                if (storageAccounts == null) {
                    try {
                        java.util.List<ArmStorageAccount> accounts = AzureArmManagerImpl.getManager(project).getStorageAccounts(model.getSubscription().getId());
                        storageAccounts = new TreeMap<String, ArmStorageAccount>();

                        for (ArmStorageAccount storageAccount : accounts) {
                            storageAccounts.put(storageAccount.getName(), storageAccount);
                        }
                    } catch (AzureCmdException e) {
                        storageAccounts = null;
                        String msg = "An error occurred while attempting to retrieve the storage accounts list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                    }
                }
                refreshStorageAccounts(null);
            }
        });

        if (storageAccounts == null) {
            final DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(new String[]{CREATE_NEW, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (CREATE_NEW.equals(o)) {
                        showNewStorageForm();
                    } else {
                        super.setSelectedItem(o);
                    }
                }
            };

            loadingSAModel.setSelectedItem(null);

            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    storageComboBox.setModel(loadingSAModel);
                }
            }, ModalityState.any());
        }
    }

    private void fillStorage() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                ArmStorageAccount selectedSA = model.getStorageAccount();
                if (selectedSA != null && !storageAccounts.containsKey(selectedSA.getName())) {
                    storageAccounts.put(selectedSA.getName(), selectedSA);
                }
                refreshStorageAccounts(selectedSA);
            }
        });
    }

    private void refreshStorageAccounts(final ArmStorageAccount selectedSA) {
        final DefaultComboBoxModel refreshedSAModel = getStorageAccountModel(selectedSA);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                storageComboBox.setModel(refreshedSAModel);
                model.getCurrentNavigationState().NEXT.setEnabled(selectedSA != null);
            }
        }, ModalityState.any());
    }

    private DefaultComboBoxModel getStorageAccountModel(ArmStorageAccount selectedSA) {
        Vector<ArmStorageAccount> accounts = filterSA();

        final DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
            @Override
            public void setSelectedItem(Object o) {
                if (CREATE_NEW.equals(o)) {
                    showNewStorageForm();
                } else {
                    super.setSelectedItem(o);
                    model.setStorageAccount((ArmStorageAccount) o);
                }
            }
        };

        refreshedSAModel.insertElementAt(CREATE_NEW, 0);

        if (accounts.contains(selectedSA)) {
            refreshedSAModel.setSelectedItem(selectedSA);
        } else {
            refreshedSAModel.setSelectedItem(null);
            model.setStorageAccount(null);
        }

        return refreshedSAModel;
    }

    private Vector<ArmStorageAccount> filterSA() {
        Vector<ArmStorageAccount> filteredStorageAccounts = new Vector<>();

        for (ArmStorageAccount storageAccount : storageAccounts.values()) {
            // VM and storage account need to be in the same region; only general purpose accounts support page blobs, so only they can be used to create vm
            if (storageAccount.getLocation().equals(model.getRegion().toString()) && storageAccount.getStorageAccount().kind() == Kind.STORAGE) {
                filteredStorageAccounts.add(storageAccount);
            }
        }
        return filteredStorageAccounts;
    }

    private void retrievePublicIpAddresses() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading public ip addresses...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                if (publicIpAddresses == null) {
                    try {
                        publicIpAddresses = AzureArmManagerImpl.getManager(project).getPublicIpAddresses(model.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        publicIpAddresses = null;
                        String msg = "An error occurred while attempting to retrieve public ip addresses list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                        return;
                    }
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipCombo.setModel(getPipAddressModel(model.getPublicIpAddress()));
                    }
                });
            }
        });

        if (publicIpAddresses == null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingPipModel = new DefaultComboBoxModel(new String[]{NONE, CREATE_NEW, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (CREATE_NEW.equals(o)) {
                                model.setWithNewPip(true);
//                    showNewPipForm();
                            } else if (NONE.equals(o)) {
                                model.setPublicIpAddress(null);
                                model.setWithNewPip(false);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingPipModel.setSelectedItem(null);
                    pipCombo.setModel(loadingPipModel);
                }
            }, ModalityState.any());
        }
    }

    private DefaultComboBoxModel getPipAddressModel(PublicIpAddress selectedPip) {
        DefaultComboBoxModel refreshedPipModel = new DefaultComboBoxModel(filterPip().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setPublicIpAddress(null);
                    model.setWithNewPip(false);
                } else if (CREATE_NEW.equals(o)) {
                    model.setWithNewPip(true);
                    model.setPublicIpAddress(null);
//                    showNewPipForm();
                } else if (o instanceof PublicIpAddress) {
                    model.setPublicIpAddress((PublicIpAddress) o);
                    model.setWithNewPip(false);
                }
            }
        };
        refreshedPipModel.insertElementAt(NONE, 0);
        refreshedPipModel.insertElementAt(CREATE_NEW, 1);

        if (selectedPip != null && publicIpAddresses.contains(selectedPip)) {
            refreshedPipModel.setSelectedItem(selectedPip);
        } else {
            model.setPublicIpAddress(null);
            refreshedPipModel.setSelectedItem(NONE);
        }

        return refreshedPipModel;
    }

    private Vector<PublicIpAddress> filterPip() {
        Vector<PublicIpAddress> filteredPips = new Vector<>();

        for (PublicIpAddress publicIpAddress : publicIpAddresses) {
            // VM and public ip address need to be in the same region
            if (publicIpAddress.region().equals(model.getRegion())) {
                filteredPips.add(publicIpAddress);
            }
        }
        return filteredPips;
    }

    private void retrieveNetworkSecurityGroups() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading network security groups...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                if (networkSecurityGroups == null) {
                    try {
                        networkSecurityGroups = AzureArmManagerImpl.getManager(project).getNetworkSecurityGroups(model.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        networkSecurityGroups = null;
                        String msg = "An error occurred while attempting to retrieve network security groups list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                        return;
                    }
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        nsgCombo.setModel(getNsgModel(model.getNetworkSecurityGroup()));
                    }
                });
            }
        });

        if (networkSecurityGroups == null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingNsgModel = new DefaultComboBoxModel(new String[]{NONE, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (NONE.equals(o)) {
                                model.setNetworkSecurityGroup(null);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingNsgModel.setSelectedItem(null);
                    nsgCombo.setModel(loadingNsgModel);
                }
            }, ModalityState.any());
        }
    }

    private DefaultComboBoxModel getNsgModel(NetworkSecurityGroup selectedNsg) {
        DefaultComboBoxModel refreshedNsgModel = new DefaultComboBoxModel(filterNsg().toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setNetworkSecurityGroup(null);
                } else if (o instanceof NetworkSecurityGroup) {
                    model.setNetworkSecurityGroup((NetworkSecurityGroup) o);
                } else {
                    model.setNetworkSecurityGroup(null);
                }
            }
        };
        refreshedNsgModel.insertElementAt(NONE, 0);

        if (selectedNsg != null && networkSecurityGroups.contains(selectedNsg)) {
            refreshedNsgModel.setSelectedItem(selectedNsg);
        } else {
            model.setNetworkSecurityGroup(null);
            refreshedNsgModel.setSelectedItem(NONE);
        }
        return refreshedNsgModel;
    }

    private Vector<NetworkSecurityGroup> filterNsg() {
        Vector<NetworkSecurityGroup> filteredNsgs = new Vector<>();

        for (NetworkSecurityGroup nsg : networkSecurityGroups) {
            // VM and network security group
            if (nsg.region().equals(model.getRegion())) {
                filteredNsgs.add(nsg);
            }
        }
        return filteredNsgs;
    }

    private void retrieveAvailabilitySets() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading availability sets...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                if (availabilitySets == null) {
                    try {
                        availabilitySets = AzureArmManagerImpl.getManager(project).getAvailabilitySets(model.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        availabilitySets = null;
                        String msg = "An error occurred while attempting to retrieve availability sets list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                        return;
                    }
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        availabilityComboBox.setModel(getAvailabilitySetsModel(model.getAvailabilitySet()));
                    }
                });
            }
        });

        if (availabilitySets == null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final DefaultComboBoxModel loadingPipModel = new DefaultComboBoxModel(new String[]{NONE, CREATE_NEW, "<Loading...>"}) {
                        @Override
                        public void setSelectedItem(Object o) {
                            super.setSelectedItem(o);
                            if (CREATE_NEW.equals(o)) {
                                model.setWithNewAvailabilitySet(true);
                                model.setAvailabilitySet(null);
                            } else if (NONE.equals(o)) {
                                model.setAvailabilitySet(null);
                                model.setWithNewAvailabilitySet(false);
                            } else {
//                                model.setVirtualNetwork((Network) o);
                            }
                        }
                    };
                    loadingPipModel.setSelectedItem(null);
                    pipCombo.setModel(loadingPipModel);
                }
            }, ModalityState.any());
        }
    }

    private DefaultComboBoxModel getAvailabilitySetsModel(AvailabilitySet selectedAvailabilitySet) {
        DefaultComboBoxModel refreshedAvailabilitySetModel = new DefaultComboBoxModel(availabilitySets.toArray()) {
            @Override
            public void setSelectedItem(final Object o) {
                super.setSelectedItem(o);
                if (NONE.equals(o)) {
                    model.setAvailabilitySet(null);
                    model.setWithNewAvailabilitySet(false);
                } else if (CREATE_NEW.equals(o)) {
                    model.setWithNewAvailabilitySet(true);
                    model.setAvailabilitySet(null);
                } else if (o instanceof AvailabilitySet) {
                    model.setAvailabilitySet(((AvailabilitySet) o));
                    model.setWithNewAvailabilitySet(false);
                }
            }
        };
        refreshedAvailabilitySetModel.insertElementAt(NONE, 0);
        refreshedAvailabilitySetModel.insertElementAt(CREATE_NEW, 1);

        if (selectedAvailabilitySet != null && availabilitySets.contains(selectedAvailabilitySet)) {
            refreshedAvailabilitySetModel.setSelectedItem(selectedAvailabilitySet);
        } else {
            model.setPublicIpAddress(null);
            refreshedAvailabilitySetModel.setSelectedItem(NONE);
        }

        return refreshedAvailabilitySetModel;
    }

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(project);
        form.fillFields(model.getSubscription(), model.getRegion());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ArmStorageAccount newStorageAccount = form.getStorageAccount();

                        if (newStorageAccount != null) {
                            model.setStorageAccount(newStorageAccount);
                            fillStorage();
                        }
                    }
                });
            }
        });

        form.show();
    }

    private void validateNext() {
        model.getCurrentNavigationState().NEXT.setEnabled(storageComboBox.getSelectedItem() instanceof StorageAccount &&
                (!subnetComboBox.isEnabled() || subnetComboBox.getSelectedItem() instanceof String));
    }

    @Override
    public boolean onFinish() {
        final boolean isNewResourceGroup = createNewRadioButton.isSelected();
        final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getSelectedItem().toString();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating virtual machine...", false) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    VirtualMachine virtualMachine = new VirtualMachine(
                            model.getName(),
                            resourceGroupName,
                            null, // field not used for ARM vm anyway
                            model.getSubnet(),
                            model.getSize().getName(),
                            VirtualMachine.Status.Unknown,
                            model.getSubscription().getId()
                    );

                    String certificate = model.getCertificate();
                    byte[] certData = new byte[0];

                    if (!certificate.isEmpty()) {
                        File certFile = new File(certificate);

                        if (certFile.exists()) {
                            FileInputStream certStream = null;

                            try {
                                certStream = new FileInputStream(certFile);
                                certData = new byte[(int) certFile.length()];

                                if (certStream.read(certData) != certData.length) {
                                    throw new Exception("Unable to process certificate: stream longer than informed size.");
                                }
                            } finally {
                                if (certStream != null) {
                                    try {
                                        certStream.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                        }
                    }

                    ArmStorageAccount storageAccount = model.getStorageAccount();

//                    for (StorageAccount account : AzureManagerImpl.getManager(project).getStorageAccounts(
//                            model.getSubscription().getId(), true)) {
//                        if (account.getName().equals(storageAccount.getName())) {
//                            storageAccount = account;
//                            break;
//                        }
//                    }
                    final com.microsoft.azure.management.compute.VirtualMachine vm = AzureArmManagerImpl.getManager(project)
                            .createVirtualMachine(model.getSubscription().getId(),
                                    virtualMachine,
                                    model.getVirtualMachineImage(),
                                    storageAccount,
                                    model.getVirtualNetwork(),
                                    model.getSubnet(),
                                    model.getPublicIpAddress(),
                                    model.isWithNewPip(),
                                    model.getAvailabilitySet(),
                                    model.isWithNewAvailabilitySet(),
                                    model.getUserName(),
                                    model.getPassword(),
                                    certData);

//                    virtualMachine = AzureManagerImpl.getManager(project).refreshVirtualMachineInformation(virtualMachine);

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                parent.addChildNode(new com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMNode(parent, model.getSubscription().getId(), vm));
                            } catch (AzureCmdException e) {
                                DefaultLoader.getUIHelper().showException("An error occurred while attempting to refresh the list of virtual machines.",
                                        e,
                                        "Azure Services Explorer - Error Refreshing VM List",
                                        false,
                                        true);
                            }
                        }
                    });
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to create the specified virtual machine." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                }
            }
        });
        return super.onFinish();
    }
}