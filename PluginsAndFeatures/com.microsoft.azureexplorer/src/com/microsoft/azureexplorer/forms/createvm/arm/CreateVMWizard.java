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

import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.forms.createvm.MachineSettingsStep;
import com.microsoft.azureexplorer.forms.createvm.SubscriptionStep;
import com.microsoft.azureexplorer.forms.createvm.VMWizard;
import com.microsoft.azureexplorer.forms.createvm.asm.SelectImageStep;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmServiceModule;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CreateVMWizard extends VMWizard {
    private VMArmServiceModule node;

    private Region region;
    private Network virtualNetwork;
    private java.util.List<Endpoint> endpoints;
    private String resourceGroupName;
    private boolean isNewResourceGroup;

    public CreateVMWizard(VMArmServiceModule node) {
        this.node = node;
        setWindowTitle("Create new Virtual Machine");
    }

    @Override
    public void addPages() {
        addPage(new SubscriptionStep(this));
//        addPage(new SelectImageStep(this));
        addPage(new MachineSettingsStep(this));
        addPage(new SettingsStep(this));
    }

    @Override
    public boolean performFinish() {
    	DefaultLoader.getIdeHelper().runInBackground(null, "Creating virtual machine...", false, true, "Creating virtual machine...", new Runnable() {
            @Override
            public void run() {
                try {
                    VirtualMachine virtualMachine = new VirtualMachine(
							name, 
							resourceGroupName,
                            availabilitySet,
                            subnet,
                            size.getName(),
                            VirtualMachine.Status.Unknown,
                            subscription.getId()
                    );


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

                    AzureManagerImpl.getManager().createVirtualMachine(virtualMachine,
                            virtualMachineImage,
                            storageAccount,
                            virtualNetwork != null ? virtualNetwork.name() : "",
                            userName,
                            password,
                            certData);
//                    virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);
                    final VirtualMachine vm = virtualMachine;
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                node.addChildNode(new VMNode(node, vm));
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

    public String[] getStepTitleList() {
        return new String[]{
                "Subscription",
//                "Select Image",
                "Machine Settings",
                "Associated resources"
        };
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

    public java.util.List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(java.util.List<Endpoint> endpoints) {
        this.endpoints = endpoints;
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
}
