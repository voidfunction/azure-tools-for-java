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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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

import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.network.Network;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azureexplorer.forms.CreateArmStorageAccountForm;

public class SettingsStep extends WizardPage {
    private static final String CREATE_NEW = "<< Create new >>";
    private static final String LOADING = "<Loading...>";

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
    private List<AvailabilitySet> availabilitySets;

    public SettingsStep(CreateVMWizard wizard) {
        super("Create new Virtual Machine", "Associated resources", null);
        this.wizard = wizard;
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
        resourceGrpField.addFocusListener(new FocusAdapter() {
        	public void focusLost(FocusEvent e) {
        		wizard.setResourceGroupName(resourceGrpField.getText());
        	}
		});
        resourceGrpCombo.addSelectionListener(new SelectionAdapter() {
        	@Override
            public void widgetSelected(SelectionEvent event) {
        		wizard.setResourceGroupName(resourceGrpCombo.getText());
        	}
		});
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
            	handleResourceGroup();
            }
        };
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);
        createNewRadioButton.setSelection(true);
        
        resourceGrpField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);

        resourceGrpCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        resourceGroupViewer = new ComboViewer(resourceGrpCombo);
        resourceGroupViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        handleResourceGroup();

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
        nsgLabel.setText("Network security group (firewall):");
        nsgCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nsgCombo.setLayoutData(gridData);

        availabilityLabel = new Label(composite, SWT.LEFT);
        availabilityLabel.setText("Availability set:");
        availabilityCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        availabilityCombo.setLayoutData(gridData);

    }
    
	private void handleResourceGroup() {
		final boolean isNewGroup = createNewRadioButton.getSelection();
		resourceGrpField.setVisible(isNewGroup);
		resourceGrpCombo.setVisible(!isNewGroup);
		wizard.setNewResourceGroup(isNewGroup);
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
        retrieveNetworkSecurityGroups();
        retrieveAvailabilitySets();

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
//                            if (resourceGroups.size() > 0) {
//                                resourceGrpCombo.select(0);
//                            }
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
		DefaultLoader.getIdeHelper().runInBackground(null, "Loading virtual networks...", false, true,
				"Loading virtual networks...", new Runnable() {
					@Override
					public void run() {
						if (virtualNetworks == null) {
							try {
								virtualNetworks = AzureArmManagerImpl.getManager(null).getVirtualNetworks(wizard.getSubscription().getId());
							} catch (AzureCmdException e) {
								virtualNetworks = null;
								String msg = "An error occurred while attempting to retrieve the virtual networks list." + "\n" + e.getMessage();
								PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
								return;
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
							}
						});
					}
				});
		networkComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (CREATE_NEW.equals(networkComboBox.getText())) {
					// showNewVirtualNetworkForm();
				} else if ((Network) networkComboBox.getData(networkComboBox.getText()) != null) {
					Network network = (Network) networkComboBox.getData(networkComboBox.getText());
					wizard.setVirtualNetwork(network);
					subnetComboBox.removeAll();

					for (String subnet : network.subnets().keySet()) {
						subnetComboBox.add(subnet);
					}
					subnetComboBox.setEnabled(true);
					if (network.subnets().size() > 0) {
						subnetComboBox.select(0);
						wizard.setSubnet(subnetComboBox.getText());
					}
				}
			}
		});
		if (virtualNetworks == null) {
			DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
				@Override
				public void run() {
					networkComboBox.setItems(new String[] { CREATE_NEW, LOADING });
					subnetComboBox.removeAll();
					subnetComboBox.setEnabled(false);
				}
			});
		}
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
		DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {

				if (storageAccounts == null) {
					try {
						java.util.List<ArmStorageAccount> accounts = AzureArmManagerImpl.getManager(null).getStorageAccounts(wizard.getSubscription().getId());
						storageAccounts = new TreeMap<String, ArmStorageAccount>();

						for (ArmStorageAccount storageAccount : accounts) {
							storageAccounts.put(storageAccount.getName(), storageAccount);
						}
					} catch (AzureCmdException e) {
						storageAccounts = null;
						String msg = "An error occurred while attempting to retrieve the storage accounts list." + "\n" + e.getMessage();
						PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
						return;
					}
				}
				fillStorage();
			}
		});
		storageComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (CREATE_NEW.equals(storageComboBox.getText())) {
					 showNewStorageForm();
				} else if (storageComboBox.getData(storageComboBox.getText()) != null) {
					ArmStorageAccount storageAccount = (ArmStorageAccount) storageComboBox.getData(storageComboBox.getText());
					wizard.setStorageAccount(storageAccount);
				}

			}
		});
		if (storageAccounts == null) {
            DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    storageComboBox.setItems(new String[]{CREATE_NEW, LOADING});
                }
            });            
        }
    }


    private void fillStorage() {
    	DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
				storageComboBox.removeAll();
				storageComboBox.add(CREATE_NEW);
				for (ArmStorageAccount storageAccount : filterSA()) {
					storageComboBox.add(storageAccount.getName());
					storageComboBox.setData(storageAccount.getName(), storageAccount);
				}
			}
		});
    }

    private Vector<ArmStorageAccount> filterSA() {
        Vector<ArmStorageAccount> filteredStorageAccounts = new Vector<>();

        for (ArmStorageAccount storageAccount : storageAccounts.values()) {
            // VM and storage account need to be in the same region; only general purpose accounts support page blobs, so only they can be used to create vm
            if (storageAccount.getLocation().equals(wizard.getRegion().toString()) 
            		&& storageAccount.getStorageAccount().kind() == Kind.STORAGE
            		&& storageAccount.getStorageAccount().sku().name() != SkuName.STANDARD_ZRS) {
                filteredStorageAccounts.add(storageAccount);
            }
        }
        return filteredStorageAccounts;
    }

    private void retrievePublicIpAddresses() {
    	DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
                if (publicIpAddresses == null) {
                    try {
                        publicIpAddresses = AzureArmManagerImpl.getManager(null).getPublicIpAddresses(wizard.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        publicIpAddresses = null;
                        String msg = "An error occurred while attempting to retrieve public ip addresses list." + "\n" + e.getMessage();
                        PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                        return;
                    }
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
					@Override
					public void run() {
						pipCombo.removeAll();
						pipCombo.add(NONE);
						pipCombo.add(CREATE_NEW);
						for (PublicIpAddress pip : filterPip()) {
							pipCombo.add(pip.name());
							pipCombo.setData(pip.name(), pip);
						}
					}
				});
            }
        });
    	pipCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (NONE.equals(pipCombo.getText())) {
                    wizard.setPublicIpAddress(null);
                    wizard.setWithNewPip(false);
                } else if (CREATE_NEW.equals(pipCombo.getText())) {
                    wizard.setWithNewPip(true);
                    wizard.setPublicIpAddress(null);
//                    showNewPipForm();
                } else if (pipCombo.getData(pipCombo.getText()) instanceof PublicIpAddress) {
                    wizard.setPublicIpAddress((PublicIpAddress) pipCombo.getData(pipCombo.getText()));
                    wizard.setWithNewPip(false);
                }
			}
		});
        if (publicIpAddresses == null) {
        	DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    pipCombo.setItems(new String[]{NONE, CREATE_NEW, LOADING});
                    pipCombo.select(0);
                    wizard.setPublicIpAddress(null);
                    wizard.setWithNewPip(false);
                }
            });
        }
    }

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
    	DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
                if (networkSecurityGroups == null) {
                    try {
                    	networkSecurityGroups = AzureArmManagerImpl.getManager(null).getNetworkSecurityGroups(wizard.getSubscription().getId());
                    } catch (AzureCmdException e) {
                    	networkSecurityGroups = null;
                        String msg = "An error occurred while attempting to retrieve network security groups list." + "\n" + e.getMessage();
                        PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                        return;
                    }
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
					@Override
					public void run() {
						nsgCombo.removeAll();
						nsgCombo.add(NONE);
						for (NetworkSecurityGroup nsg : networkSecurityGroups) {
							nsgCombo.add(nsg.name());
							nsgCombo.setData(nsg.name(), nsg);
						}
					}
				});
            }
        });
    	nsgCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (NONE.equals(nsgCombo.getText())) {
                    wizard.setNetworkSecurityGroup(null);
                } else if (nsgCombo.getData(nsgCombo.getText()) instanceof NetworkSecurityGroup) {
                    wizard.setNetworkSecurityGroup((NetworkSecurityGroup) nsgCombo.getData(nsgCombo.getText()));
                }
			}
		});
        if (networkSecurityGroups == null) {
        	DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    nsgCombo.setItems(new String[]{NONE, LOADING});
                }
            });
        }
    }
    
    private void retrieveAvailabilitySets() {
    	DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
                if (availabilitySets == null) {
                    try {
                        availabilitySets = AzureArmManagerImpl.getManager(null).getAvailabilitySets(wizard.getSubscription().getId());
                    } catch (AzureCmdException e) {
                        availabilitySets = null;
                        String msg = "An error occurred while attempting to retrieve availablity sets list." + "\n" + e.getMessage();
                        PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                        return;
                    }
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
					@Override
					public void run() {
						availabilityCombo.removeAll();
						availabilityCombo.add(NONE);
						availabilityCombo.add(CREATE_NEW);
						for (AvailabilitySet availabilitySet : availabilitySets) {
							availabilityCombo.add(availabilitySet.name());
							availabilityCombo.setData(availabilitySet.name(), availabilitySet);
						}
					}
				});
            }
        });
    	availabilityCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (NONE.equals(availabilityCombo.getText())) {
                    wizard.setAvailabilitySet(null);
                    wizard.setWithNewAvailabilitySet(false);
                } else if (CREATE_NEW.equals(pipCombo.getText())) {
                    wizard.setWithNewAvailabilitySet(true);
                    wizard.setAvailabilitySet(null);
                } else if (availabilityCombo.getData(availabilityCombo.getText()) instanceof AvailabilitySet) {
                    wizard.setAvailabilitySet((AvailabilitySet) availabilityCombo.getData(availabilityCombo.getText()));
                    wizard.setWithNewAvailabilitySet(false);
                }
			}
		});
        if (availabilitySets == null) {
        	DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    availabilityCombo.setItems(new String[]{NONE, CREATE_NEW, LOADING});
                }
            });
        }
    }


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

    private void showNewStorageForm() {
        final CreateArmStorageAccountForm form = new CreateArmStorageAccountForm(PluginUtil.getParentShell(), wizard.getSubscription(), wizard.getRegion());

		form.setOnCreate(new Runnable() {
			@Override
			public void run() {
				ArmStorageAccount newStorageAccount = form.getStorageAccount();
				if (newStorageAccount != null) {
					wizard.setStorageAccount(newStorageAccount);
					storageAccounts.put(newStorageAccount.getName(), newStorageAccount);
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
