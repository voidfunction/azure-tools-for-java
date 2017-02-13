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
package com.microsoft.intellij.wizards;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.wizards.createarmvm.MachineSettingsStep;
import com.microsoft.intellij.wizards.createarmvm.SelectImageStep;
import com.microsoft.intellij.wizards.createarmvm.SettingsStep;
import com.microsoft.intellij.wizards.createarmvm.SubscriptionStep;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class VMWizardModel extends WizardModel {
    protected String region;
    protected VirtualMachineImage virtualMachineImage;
    protected Network virtualNetwork;
    private boolean withNewNetwork;
    protected StorageAccount storageAccount;
    //    private String availabilitySet;
    protected PublicIpAddress publicIpAddress;
    protected boolean withNewPip;
    protected NetworkSecurityGroup networkSecurityGroup;
    protected AvailabilitySet availabilitySet;
    protected boolean withNewAvailabilitySet;
    private String name;
    private String size;
    private String userName;
    private String password;
    private String certificate;
    private String subnet;
    private SubscriptionDetail subscription;

    public VMWizardModel(VMArmModule node) {
        super(ApplicationNamesInfo.getInstance().getFullProductName() + " - Create new Virtual Machine");
        Project project = (Project) node.getProject();

        add(new SubscriptionStep(this, project));
        add(new SelectImageStep(this, project));
        add(new MachineSettingsStep(this, project));
        add(new SettingsStep(this, project, node));
    }

    public String[] getStepTitleList() {
        return new String[]{
                "Subscription",
                "Select Image",
                "Machine Settings",
                "Associated Resources"
        };
    }

    public void configStepList(JList jList, int step) {

        jList.setListData(getStepTitleList());
        jList.setSelectedIndex(step);
        jList.setBorder(new EmptyBorder(10, 0, 10, 0));

        jList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean b, boolean b1) {
                return super.getListCellRendererComponent(jList, "  " + o.toString(), i, b, b1);
            }
        });

        for (MouseListener mouseListener : jList.getMouseListeners()) {
            jList.removeMouseListener(mouseListener);
        }

        for (MouseMotionListener mouseMotionListener : jList.getMouseMotionListeners()) {
            jList.removeMouseMotionListener(mouseMotionListener);
        }
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
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

    public void setSubscription(SubscriptionDetail subscription) {
        this.subscription = subscription;
    }

    public SubscriptionDetail getSubscription() {
        return subscription;
    }

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Network getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(Network virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    public boolean isWithNewNetwork() {
        return withNewNetwork;
    }

    public void setWithNewNetwork(boolean withNewNetwork) {
        this.withNewNetwork = withNewNetwork;
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
}
