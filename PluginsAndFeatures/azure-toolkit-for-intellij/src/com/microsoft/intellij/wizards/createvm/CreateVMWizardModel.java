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

package com.microsoft.intellij.wizards.createvm;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.wizards.VMWizardModel;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;

public class CreateVMWizardModel extends VMWizardModel {

    private VirtualMachineImage virtualMachineImage;
    private CloudService cloudService;
    private boolean filterByCloudService;
    private Endpoint[] endpoints;
    private VirtualNetwork virtualNetwork;
    private StorageAccount storageAccount;
    private String availabilitySet;

    public CreateVMWizardModel(VMServiceModule node) {
        super();

        Project project = (Project) node.getProject();

        add(new SubscriptionStep(this, project));
        add(new SelectImageStep(this, project));
        add(new MachineSettingsStep(this, project));
        add(new CloudServiceStep(this, project));
        add(new EndpointStep(this, project, node));

        filterByCloudService = true;
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

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
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

    public Endpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public VirtualNetwork getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public String getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(String availabilitySet) {
        this.availabilitySet = availabilitySet;
    }
}
