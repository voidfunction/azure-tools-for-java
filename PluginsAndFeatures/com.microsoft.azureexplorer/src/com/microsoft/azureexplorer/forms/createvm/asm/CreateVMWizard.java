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
package com.microsoft.azureexplorer.forms.createvm.asm;

import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.forms.createvm.MachineSettingsStep;
import com.microsoft.azureexplorer.forms.createvm.SubscriptionStep;
import com.microsoft.azureexplorer.forms.createvm.VMWizard;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class CreateVMWizard extends VMWizard {
    private static final String BASE_HTML_VM_IMAGE = "<html>\n" +
            "<body style=\"padding: 5px; width: 250px\">\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 12pt;font-weight: bold;\">#TITLE#</p>\n" +
            "    <p style=\"font-family: 'Segoe UI';font-size: 8pt; width:200px \">#DESCRIPTION#</p>\n" +
            "    <p>\n" +
            "        <table style='width:200px'>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;width:60px;vertical-align:top;\"><b>PUBLISHED</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;\">#PUBLISH_DATE#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;vertical-align:top;\"><b>PUBLISHER</b></td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;\">#PUBLISH_NAME#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;vertical-align:top;\"><b>OS FAMILY</b></td>\n" +
            "                <td style =\"font-family: 'Segoe UI';font-size: 8pt;\">#OS#</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;v-align:top;font-weight:bold;\">LOCATION</td>\n" +
            "                <td style=\"font-family: 'Segoe UI';font-size: 8pt;\">#LOCATION#</td>\n" +
            "            </tr>\n" +
            "        </table>\n" +
            "    </p>\n" +
            "    #PRIVACY#\n" +
            "    #LICENCE#\n" +
            "</body>\n" +
            "</html>";

    private VMServiceModule node;

    private CloudService cloudService;
    private boolean filterByCloudService;
    private VirtualNetwork virtualNetwork;
    private java.util.List<Endpoint> endpoints;

    private EndpointStep endpointStep;

    public CreateVMWizard(VMServiceModule node) {
        this.node = node;
        setWindowTitle("Create new Virtual Machine");
    }

    @Override
    public void addPages() {
        addPage(new SubscriptionStep(this));
        addPage(new SelectImageStep(this));
        addPage(new MachineSettingsStep(this));
        addPage(new CloudServiceStep(this));
        addPage(endpointStep = new EndpointStep(this));
    }

    @Override
    public boolean performFinish() {
//        final EndpointTableModel tableModel = (EndpointTableModel) endpointsTable.getModel();
        DefaultLoader.getIdeHelper().runInBackground(null, "Creating virtual machine...", false, true, "Creating virtual machine...", new Runnable() {
            @Override
            public void run() {
                try {
                    VirtualMachine virtualMachine = new VirtualMachine(
                            name,
                            cloudService.getName(),
                            cloudService.getProductionDeployment().getName(),
                            availabilitySet,
                            subnet,
                            size.getName(),
                            VirtualMachine.Status.Unknown,
                            subscription.getId()
                    );

                    virtualMachine.getEndpoints().addAll(endpointStep.getEndpointsList());

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
                            virtualNetwork != null ? virtualNetwork.getName() : "",
                            userName,
                            password,
                            certData);
                    virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);
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
        return getContainer().getCurrentPage() instanceof EndpointStep;
    }

    public String[] getStepTitleList() {
        return new String[]{
                "Subscription",
                "Select Image",
                "Machine Settings",
                "Cloud Service",
                "Endpoints"
        };
    }

    public String getHtmlFromVMImage(VirtualMachineImage virtualMachineImage) {
        String html = BASE_HTML_VM_IMAGE;
        html = html.replace("#TITLE#", virtualMachineImage.getLabel());
        html = html.replace("#DESCRIPTION#", virtualMachineImage.getDescription());
        html = html.replace("#PUBLISH_DATE#", new SimpleDateFormat("dd-M-yyyy").format(virtualMachineImage.getPublishedDate().getTime()));
        html = html.replace("#PUBLISH_NAME#", virtualMachineImage.getPublisherName());
        html = html.replace("#OS#", virtualMachineImage.getOperatingSystemType());
        html = html.replace("#LOCATION#", virtualMachineImage.getLocation());

        html = html.replace("#PRIVACY#", virtualMachineImage.getPrivacyUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getPrivacyUri() + "' style=\"font-family: 'Segoe UI';font-size: 8pt;\">Privacy statement</a></p>");


        html = html.replace("#LICENCE#", virtualMachineImage.getEulaUri().isEmpty()
                ? ""
                : "<p><a href='" + virtualMachineImage.getEulaUri() + "' style=\"font-family: 'Segoe UI';font-size: 8pt;\">Licence agreement</a></p>");

        return html;
    }

    public CloudService getCloudService() {
        return cloudService;
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public boolean isFilterByCloudService() {
        return filterByCloudService;
    }

    public void setFilterByCloudService(boolean filterByCloudService) {
        this.filterByCloudService = filterByCloudService;
    }

    public VirtualNetwork getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    public java.util.List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(java.util.List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}
