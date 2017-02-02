/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azureexplorer.forms.createvm.arm;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.forms.createvm.MachineSettingsStep;
import com.microsoft.azureexplorer.forms.createvm.SubscriptionStep;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class CreateVMWizard extends Wizard {
    private VMArmModule node;

	protected SubscriptionDetail subscription;
	protected String name;
	protected String userName;
	protected String password;
	protected String certificate;
	protected String subnet;
	protected VirtualMachineSize size;
    
    private Region region;
    private Network virtualNetwork;
    private String resourceGroupName;
    private boolean isNewResourceGroup;
	private VirtualMachineImage virtualMachineImage;
	private StorageAccount storageAccount;
    private PublicIpAddress publicIpAddress;
    private boolean withNewPip;
    private AvailabilitySet availabilitySet;
    private boolean withNewAvailabilitySet;
    private NetworkSecurityGroup networkSecurityGroup;
    
    private Azure azure;

    public CreateVMWizard(VMArmModule node) {
        this.node = node;
        setWindowTitle("Create new Virtual Machine");
    }

    @Override
    public void addPages() {
        addPage(new SubscriptionStep(this));
        addPage(new SelectImageStep(this));
        addPage(new MachineSettingsStep(this));
        addPage(new SettingsStep(this));
    }

    @Override
    public boolean performFinish() {
    	DefaultLoader.getIdeHelper().runInBackground(null, "Creating virtual machine " + name + "...", false, true, "Creating virtual machine " + name + "...", new Runnable() {
            @Override
            public void run() {
                try {
//                    virtualMachine.getEndpoints().addAll(endpointStep.getEndpointsList());

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
                    
                    VirtualMachine vm = AzureSDKManager.createVirtualMachine(subscription.getSubscriptionId(),
                            name,
                            resourceGroupName,
                            size.name(),
                            virtualMachineImage,
                            storageAccount,
                            virtualNetwork,
                            subnet,
                            publicIpAddress,
                            withNewPip,
                            availabilitySet,
                            withNewAvailabilitySet,
                            userName,
                            password,
                            certData.length > 0 ? new String(certData) : null);

//                    virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                node.addChildNode(new VMNode(node, subscription.getSubscriptionId(), vm));
                            } catch (AzureCmdException e) {
                            	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
                            			"An error occurred while refreshing the list of virtual machines.", e);
                            }
                        }
                    });
                } catch (Exception e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying to create the specified virtual machine",
                            e,
                            "Error Creating Virtual Machine",
                            false,
                            true);
                    Activator.getDefault().log("Error Creating Virtual Machine", e);
                }
            }
        });
        return true;
    }

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() instanceof SettingsStep;
    }

	public List configStepList(Composite parent, final int step) {
		GridData gridData = new GridData();
		gridData.widthHint = 100;
		//
		gridData.verticalAlignment = GridData.BEGINNING;
		gridData.grabExcessVerticalSpace = true;
		List createVmStepsList = new List(parent, SWT.BORDER);
		createVmStepsList.setItems(getStepTitleList());
		createVmStepsList.setSelection(step);
		createVmStepsList.setLayoutData(gridData);
		createVmStepsList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List l = (List) e.widget;
				l.setSelection(step);
			}
		});
		// createVmStepsList.setEnabled(false);

		// jList.setBorder(new EmptyBorder(10, 0, 10, 0));

		// jList.setCellRenderer(new DefaultListCellRenderer() {
		// @Override
		// public Component getListCellRendererComponent(JList jList, Object o,
		// int i, boolean b, boolean b1) {
		// return super.getListCellRendererComponent(jList, " " + o.toString(),
		// i, b, b1);
		// }
		// });
		//
		// for (MouseListener mouseListener : jList.getMouseListeners()) {
		// jList.removeMouseListener(mouseListener);
		// }
		//
		// for (MouseMotionListener mouseMotionListener :
		// jList.getMouseMotionListeners()) {
		// jList.removeMouseMotionListener(mouseMotionListener);
		// }
		return createVmStepsList;
	}
    
    public String[] getStepTitleList() {
        return new String[]{
                "Subscription",
                "Select Image",
                "Machine Settings",
                "Associated resources"
        };
    }

    public Azure getAzure() {
		return azure;
	}

	public void setAzure(Azure azure) {
		this.azure = azure;
	}

	public void setSubscription(SubscriptionDetail subscription) {
    	try {
    		this.subscription = subscription;
    		AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
    		azure = azureManager.getAzure(subscription.getSubscriptionId());
    	} catch (Exception ex) {
			DefaultLoader.getUIHelper().showException(ex.getMessage(), ex, "Error selecting subscription", true, false);
		}
    }
	
	public SubscriptionDetail getSubscription() {
	    return subscription;
	}

	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	public String getUserName() {
	    return userName;
	}

	public void setUserName(String userName) {
	    this.userName = userName;
	}

	public String getPassword() {
	    return password;
	}

	public void setPassword(String password) {
	    this.password = password;
	}

	public String getCertificate() {
	    return certificate;
	}

	public void setCertificate(String certificate) {
	    this.certificate = certificate;
	}

	public String getSubnet() {
	    return subnet;
	}

	public void setSubnet(String subnet) {
	    this.subnet = subnet;
	}
    
    public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Network getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(Network virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

	public String getResourceGroupName() {
		return resourceGroupName;
	}

	public void setResourceGroupName(String resourceGroupName) {
		this.resourceGroupName = resourceGroupName;
	}

	public boolean isNewResourceGroup() {
		return isNewResourceGroup;
	}

	public void setNewResourceGroup(boolean isNewResourceGroup) {
		this.isNewResourceGroup = isNewResourceGroup;
	}
	
	public VirtualMachineImage getVirtualMachineImage() {
	    return virtualMachineImage;
	}

	public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
	    this.virtualMachineImage = virtualMachineImage;
	}

	public StorageAccount getStorageAccount() {
		return storageAccount;
	}

	public void setStorageAccount(StorageAccount storageAccount) {
		this.storageAccount = storageAccount;
	}

	public PublicIpAddress getPublicIpAddress() {
		return publicIpAddress;
	}

	public void setPublicIpAddress(PublicIpAddress publicIpAddress) {
		this.publicIpAddress = publicIpAddress;
	}

	public boolean isWithNewPip() {
		return withNewPip;
	}

	public void setWithNewPip(boolean withNewPip) {
		this.withNewPip = withNewPip;
	}

	public NetworkSecurityGroup getNetworkSecurityGroup() {
		return networkSecurityGroup;
	}

	public void setNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup) {
		this.networkSecurityGroup = networkSecurityGroup;
	}

	public AvailabilitySet getAvailabilitySet() {
		return availabilitySet;
	}

	public void setAvailabilitySet(AvailabilitySet availabilitySet) {
		this.availabilitySet = availabilitySet;
	}

	public boolean isWithNewAvailabilitySet() {
		return withNewAvailabilitySet;
	}

	public void setWithNewAvailabilitySet(boolean withNewAvailabilitySet) {
		this.withNewAvailabilitySet = withNewAvailabilitySet;
	}
	
	public VirtualMachineSize getSize() {
	    return size;
	}

	public void setSize(VirtualMachineSize size) {
	    this.size = size;
	}	
}
