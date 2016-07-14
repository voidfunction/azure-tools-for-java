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

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.wizards.VMWizardModel;
import com.microsoft.intellij.wizards.createvm.SelectImageStep;
import com.microsoft.intellij.wizards.createvm.SubscriptionStep;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmServiceModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CreateVMWizardModel extends VMWizardModel {

//    private VirtualMachineImage virtualMachineImage;
    private String name;
//    private VirtualMachineSize size;
    private String userName;
    private String password;
    private String certificate;
//    private CloudService cloudService;
//    private boolean filterByCloudService;
//    private StorageAccount storageAccount;
//    private VirtualNetwork virtualNetwork;
//    private String subnet;
//    private String availabilitySet;
//    private Endpoint[] endpoints;

    public CreateVMWizardModel(VMArmServiceModule node) {
        super();

        Project project = (Project) node.getProject();

        add(new SubscriptionStep(this, project));
        add(new SelectImageStep(this, project));
//        add(new MachineSettingsStep(this, project));
//        add(new CloudServiceStep(this, project));
//        add(new EndpointStep(this, project, node));

//        filterByCloudService = true;
    }

    public String[] getStepTitleList() {
        return new String[]{
                "Subscription",
                "Select Image",
                /*"Machine Settings",
                "Cloud Service",
                "Endpoints"*/
        };
    }

//    public String getHtmlFromVMImage(VirtualMachineImage virtualMachineImage) {
//        String html = BASE_HTML_VM_IMAGE;
//        html = html.replace("#TITLE#", virtualMachineImage.getLabel());
//        html = html.replace("#DESCRIPTION#", virtualMachineImage.getDescription());
//        html = html.replace("#PUBLISH_DATE#", new SimpleDateFormat("dd-M-yyyy").format(virtualMachineImage.getPublishedDate().getTime()));
//        html = html.replace("#PUBLISH_NAME#", virtualMachineImage.getPublisherName());
//        html = html.replace("#OS#", virtualMachineImage.getOperatingSystemType());
//        html = html.replace("#LOCATION#", virtualMachineImage.getLocation());
//
//        html = html.replace("#PRIVACY#", virtualMachineImage.getPrivacyUri().isEmpty()
//                ? ""
//                : "<p><a href='" + virtualMachineImage.getPrivacyUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Privacy statement</a></p>");
//
//
//        html = html.replace("#LICENCE#", virtualMachineImage.getEulaUri().isEmpty()
//                ? ""
//                : "<p><a href='" + virtualMachineImage.getEulaUri() + "' style=\"font-family: 'Segoe UI';font-size: 12pt;\">Licence agreement</a></p>");
//
//        return html;
//    }

//    public VirtualMachineImage getVirtualMachineImage() {
//        return virtualMachineImage;
//    }

//    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
//        this.virtualMachineImage = virtualMachineImage;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public VirtualMachineSize getSize() {
//        return size;
//    }

//    public void setSize(VirtualMachineSize size) {
//        this.size = size;
//    }

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

//    public CloudService getCloudService() {
//        return cloudService;
//    }

//    public void setCloudService(CloudService cloudService) {
//        this.cloudService = cloudService;
//    }

//    public boolean isFilterByCloudService() {
//        return filterByCloudService;
//    }

//    public void setFilterByCloudService(boolean filterByCloudService) {
//        this.filterByCloudService = filterByCloudService;
//    }

//    public VirtualNetwork getVirtualNetwork() {
//        return virtualNetwork;
//    }

//    public void setVirtualNetwork(VirtualNetwork virtualNetwork) {
//        this.virtualNetwork = virtualNetwork;
//    }

//    public String getSubnet() {
//        return subnet;
//    }

//    public void setSubnet(String subnet) {
//        this.subnet = subnet;
//    }
//
//    public StorageAccount getStorageAccount() {
//        return storageAccount;
//    }
//
//    public void setStorageAccount(StorageAccount storageAccount) {
//        this.storageAccount = storageAccount;
//    }

//    public String getAvailabilitySet() {
//        return availabilitySet;
//    }
//
//    public void setAvailabilitySet(String availabilitySet) {
//        this.availabilitySet = availabilitySet;
//    }

//    public Endpoint[] getEndpoints() {
//        return endpoints;
//    }
//
//    public void setEndpoints(Endpoint[] endpoints) {
//        this.endpoints = endpoints;
//    }
}
