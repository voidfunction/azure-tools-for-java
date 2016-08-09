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
import com.microsoft.azure.management.network.Network;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.CreateArmStorageAccountForm;
import com.microsoft.intellij.forms.CreateStorageAccountForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.VirtualMachine;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SettingsStep extends WizardStep<CreateVMWizardModel> {
    private static final String CREATE_VIRTUAL_NETWORK = "<< Create new virtual network >>";
    private final String NONE = "(None)";

    private final Node parent;
    private Project project;
    private CreateVMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JEditorPane imageDescriptionTextPane;
    private JComboBox storageComboBox;
    private JCheckBox availabilitySetCheckBox;
    private JComboBox availabilityComboBox;
    private JComboBox networkComboBox;
    private JComboBox subnetComboBox;
    private JRadioButton createNewRadioButton;
    private JRadioButton useExistingRadioButton;
    private JTextField resourceGrpField;
    private JComboBox resourceGrpCombo;

    private List<Network> virtualNetworks;
    private final Lock vnLock = new ReentrantLock();
    private final Condition vnInitialized = vnLock.newCondition();

    private Map<String, StorageAccount> storageAccounts;
    private final Lock saLock = new ReentrantLock();
    private final Condition saInitialized = saLock.newCondition();

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

        availabilitySetCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                availabilityComboBox.setEnabled(availabilitySetCheckBox.isSelected());
            }
        });

//        imageDescriptionTextPane.addHyperlinkListener(new HyperlinkListener() {
//            @Override
//            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
//                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//                    if (Desktop.isDesktopSupported()) {
//                        try {
//                            Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
//                        } catch (Exception e) {
//                            AzurePlugin.log(e.getStackTrace().toString());
//                        }
//                    }
//                }
//            }
//        });
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

//        final VirtualMachineImage virtualMachineImage = model.getVirtualMachineImage();
//        imageDescriptionTextPane.setText(model.getHtmlFromVMImage(virtualMachineImage));
//        imageDescriptionTextPane.setCaretPosition(0);

        fillResourceGroups();
        retrieveStorageAccounts();
        retrieveVirtualNetworks();

//        if (model.isFilterByCloudService()) {
//            fillCloudServices(null, true);
//        } else {
//        fillVirtualNetworks(true);
//        }

        return rootPanel;
    }

//    @Override
//    public WizardStep onPrevious(CreateVMWizardModel model) {
//        Vector<Endpoint> endpointData = ((EndpointTableModel) endpointsTable.getModel()).getData();
////        model.setEndpoints(endpointData.toArray(new Endpoint[endpointData.size()]));
//
//        return super.onPrevious(model);
//    }

//    private Collection<CloudService> filterCS(VirtualNetwork selectedVN) {
//        Collection<CloudService> services = /*selectedVN == null ? cloudServices.values() :*/ new Vector<CloudService>();
//
//        if (selectedVN != null) {
////            for (CloudService cloudService : cloudServices.values()) {
////                if ((isDeploymentEmpty(cloudService, PRODUCTION) && areSameRegion(cloudService, selectedVN)) ||
////                        areSameNetwork(cloudService, selectedVN)) {
////                    services.add(cloudService);
////                }
////            }
//        }
//
//        return services;
//    }

    private void retrieveVirtualNetworks() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading virtual networks...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

//                vnLock.lock();

//                try {
                if (virtualNetworks == null) {
                    try {
                        virtualNetworks = AzureArmManagerImpl.getManager(project).getVirtualNetworks(model.getSubscription().getId());

//                            networkComboBox.setModel(getVirtualNetworkModel(model.getVirtualNetwork(), model.getSubnet()));

//                            vnInitialized.signalAll();
                    } catch (AzureCmdException e) {
                        virtualNetworks = null;
                        String msg = "An error occurred while attempting to retrieve the virtual networks list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), msg, e);
                    }
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        networkComboBox.setModel(getVirtualNetworkModel(model.getVirtualNetwork(), model.getSubnet()));
                    }
                });
//                } finally {
//                    vnLock.unlock();
//                }
            }
        });

        if (virtualNetworks == null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                                      @Override
                                      public void run() {
                                          final DefaultComboBoxModel loadingVNModel = new DefaultComboBoxModel(new String[]{CREATE_VIRTUAL_NETWORK, "<Loading...>"}) {
                                              @Override
                                              public void setSelectedItem(Object o) {
                                                  if (CREATE_VIRTUAL_NETWORK.equals(o)) {
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

//    private void fillVirtualNetworks(final boolean cascade) {
//        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
//            @Override
//            public void run() {
//                VirtualNetwork selectedVN = model.getVirtualNetwork();
//                String selectedSN = model.getSubnet();
//
//                vnLock.lock();
//
//                try {
//                    while (virtualNetworks == null) {
//                        vnInitialized.await();
//                    }
//                } catch (InterruptedException e) {
//                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), "An error occurred while attempting load the virtual networks list.", e);
//                } finally {
//                    vnLock.unlock();
//                }
//
//                refreshVirtualNetworks(selectedVN, selectedSN, cascade);
//            }
//        });
//    }

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
                        java.util.List<StorageAccount> accounts = AzureArmManagerImpl.getManager(project).getStorageAccounts(model.getSubscription().getId());
                        storageAccounts = new TreeMap<String, StorageAccount>();

                        for (StorageAccount storageAccount : accounts) {
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
            final String createSA = "<< Create new storage account >>";

            final DefaultComboBoxModel loadingSAModel = new DefaultComboBoxModel(new String[]{createSA, "<Loading...>"}) {
                @Override
                public void setSelectedItem(Object o) {
                    if (createSA.equals(o)) {
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
                StorageAccount selectedSA = model.getStorageAccount();

                saLock.lock();

                try {
                    while (storageAccounts == null) {
                        saInitialized.await();
                    }

                    if (selectedSA != null && !storageAccounts.containsKey(selectedSA.getName())) {
                        storageAccounts.put(selectedSA.getName(), selectedSA);
                    }
                } catch (InterruptedException e) {
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), "An error occurred while attempting to load the storage accounts list.", e);
                } finally {
                    saLock.unlock();
                }

                refreshStorageAccounts(/*selectedCS, */selectedSA);
            }
        });
    }

    private void refreshStorageAccounts(final StorageAccount selectedSA) {
        final DefaultComboBoxModel refreshedSAModel = getStorageAccountModel(/*selectedCS, */selectedSA);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                storageComboBox.setModel(refreshedSAModel);
                model.getCurrentNavigationState().NEXT.setEnabled(/*selectedCS != null &&*/
                        selectedSA != null/* &&
                        selectedSA.getLocation().equals(selectedCS.getLocation())*/);
            }
        }, ModalityState.any());
    }

    private DefaultComboBoxModel getStorageAccountModel(/*final CloudService selectedCS, */StorageAccount selectedSA) {
        Vector<StorageAccount> accounts = filterSA(/*selectedCS*/);

        final String createSA = "<< Create new storage account >>";

        final DefaultComboBoxModel refreshedSAModel = new DefaultComboBoxModel(accounts) {
            @Override
            public void setSelectedItem(Object o) {
                if (createSA.equals(o)) {
                    showNewStorageForm();
                } else {
                    super.setSelectedItem(o);
                    model.setStorageAccount((StorageAccount) o);
                }
            }
        };

        refreshedSAModel.insertElementAt(createSA, 0);

        if (accounts.contains(selectedSA)) {
            refreshedSAModel.setSelectedItem(selectedSA);
        } else {
            refreshedSAModel.setSelectedItem(null);
            model.setStorageAccount(null);
        }

        return refreshedSAModel;
    }

    private Vector<StorageAccount> filterSA(/*CloudService selectedCS*/) {
        Vector<StorageAccount> accounts = new Vector<StorageAccount>();

//        if (selectedCS != null) {
//            for (StorageAccount storageAccount : storageAccounts.values()) {
//                if ((!storageAccount.getLocation().isEmpty() &&
//                        storageAccount.getLocation().equals(selectedCS.getLocation())) ||
//                        (!storageAccount.getAffinityGroup().isEmpty() &&
//                                storageAccount.getAffinityGroup().equals(selectedCS.getAffinityGroup()))) {
//                    accounts.add(storageAccount);
//                }
//            }
//        }

        return accounts;
    }

    private void fillAvailabilitySets() {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                availabilityComboBox.setModel(new DefaultComboBoxModel(new String[]{}));
            }
        }, ModalityState.any());
    }

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(project);
        form.fillFields(model.getSubscription());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        StorageAccount newStorageAccount = form.getStorageAccount();

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
                            model.getAvailabilitySet(),
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

                    StorageAccount storageAccount = model.getStorageAccount();

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
                                    model.getUserName(),
                                    model.getPassword(),
                                    certData);

//                    virtualMachine = AzureManagerImpl.getManager(project).refreshVirtualMachineInformation(virtualMachine);

//                    final VirtualMachine vm = virtualMachine;

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