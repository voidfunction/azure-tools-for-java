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
package com.microsoft.intellij.docker.wizards.createhost.forms;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardStep;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

public class AzureNewDockerHostStep extends AzureNewDockerWizardStep {
  private JPanel rootConfigureContainerPanel;
  private JLabel dockerHostNameLabel;
  private JTextField dockerHostNameTextField;
  private JComboBox<AzureDockerSubscription> dockerSubscriptionComboBox;
  private JComboBox<String> dockerLocationComboBox;
  private JTabbedPane tabbedPane1;
  private JComboBox<KnownDockerVirtualMachineImage> dockerHostOSTypeComboBox;
  private JComboBox<String> dockerHostVMSizeComboBox;
  private ButtonGroup dockerHostRGGroup;
  private JRadioButton dockerHostNewRGRadioButton;
  private JRadioButton dockerHostSelectRGRadioButton;
  private JLabel dockerHostRGLabel;
  private JTextField dockerHostRGTextField;
  private JComboBox<String> dockerHostSelectRGComboBox;
  private ButtonGroup dockerHostVnetGroup;
  private JRadioButton dockerHostNewVNetRadioButton;
  private JRadioButton dockerHostSelectVNetRadioButton;
  private JComboBox<AzureDockerVnet> dockerHostSelectVnetComboBox;
  private JComboBox<String> dockerHostSelectSubnetComboBox;
  private JLabel dockerHostNewVNetNameLabel;
  private JTextField dockerHostNewVNetNameTextField;
  private JLabel dockerHostNewVNetAddrSpaceLabel;
  private JTextField dockerHostNewVNetAddrSpaceTextField;
  private ButtonGroup dockerHostStorageGroup;
  private JRadioButton dockerHostNewStorageRadioButton;
  private JRadioButton dockerHostSelectStorageRadioButton;
  private JLabel dockerNewStorageLabel;
  private JTextField dockerNewStorageTextField;
  private JComboBox<String> dockerSelectStorageComboBox;
  private JPanel vmKindPanel;
  private JPanel rgPanel;
  private JPanel networkPanel;
  private JPanel storagePanel;

  private AzureNewDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private DockerHost newHost;

  public AzureNewDockerHostStep(String title, AzureNewDockerWizardModel model) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure the Docker virtual machine to be created");
    this.model = model;
    this.dockerManager = model.getDockerManager();
    this.newHost = model.getDockerHost();

    dockerHostNameLabel.setVisible(false);
    dockerHostNameTextField.setText(newHost.name);
    dockerHostNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostNameTip());
    dockerHostNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostName(((JTextField) input).getText())) {
          dockerHostNameLabel.setVisible(false);
          return true;
        } else {
          dockerHostNameLabel.setVisible(true);
          return false;
        }
      }
    });

    DefaultComboBoxModel<AzureDockerSubscription> dockerSubscriptionComboModel = new DefaultComboBoxModel<AzureDockerSubscription>(new Vector<AzureDockerSubscription>(dockerManager.getSubscriptionsList()));
    dockerSubscriptionComboBox.setModel(dockerSubscriptionComboModel);
    dockerSubscriptionComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
        updateDockerLocationComboBox(currentSubscription);
        updateDockerHostSelectRGComboBox(currentSubscription);
        updateDockerSelectVnetComboBox(currentSubscription, (String) dockerLocationComboBox.getSelectedItem());
        updateDockerSelectStorageComboBox(currentSubscription);
      }
    });

    updateDockerLocationGroup();
    updateDockerHostOSTypeComboBox();
    updateDockerHostVMSizeComboBox();
    updateDockerHostRGGroup();
    updateDockerHostVnetGroup();
    updateDockerHostStorageGroup();
  }

  private void updateDockerLocationGroup() {
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    updateDockerLocationComboBox(currentSubscription);

    dockerLocationComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
        updateDockerHostSelectRGComboBox(currentSubscription);
        updateDockerSelectVnetComboBox( currentSubscription, (String) dockerLocationComboBox.getSelectedItem());
      }
    });
  }

  private void updateDockerLocationComboBox(AzureDockerSubscription currentSubscription) {
    if (currentSubscription != null && currentSubscription.locations != null) {
      DefaultComboBoxModel<String> dockerLocationComboModel = new DefaultComboBoxModel<>(new Vector<String>(currentSubscription.locations));
      dockerLocationComboBox.setModel(dockerLocationComboModel);
      try {
        dockerLocationComboModel.setSelectedItem("centralus");
      } catch (Exception e) {}
    }
  }

  private void updateDockerHostOSTypeComboBox() {
    DefaultComboBoxModel<KnownDockerVirtualMachineImage> dockerHostOSTypeComboModel = new DefaultComboBoxModel<>();
    for (KnownDockerVirtualMachineImage knownDockerVirtualMachineImage : KnownDockerVirtualMachineImage.values()) {
      dockerHostOSTypeComboModel.addElement(knownDockerVirtualMachineImage);
    }
    dockerHostOSTypeComboBox.setModel(dockerHostOSTypeComboModel);
  }

  private void updateDockerHostVMSizeComboBox() {
    DefaultComboBoxModel<String> dockerHostVMSizeComboModel = new DefaultComboBoxModel<String>();
    for (KnownDockerVirtualMachineSizes knownDockerVirtualMachineSize : KnownDockerVirtualMachineSizes.values()) {
      dockerHostVMSizeComboModel.addElement(knownDockerVirtualMachineSize.name());
    }
    dockerHostVMSizeComboBox.setModel(dockerHostVMSizeComboModel);
    if (dockerHostVMSizeComboModel.getSize() > 0) {
      dockerHostVMSizeComboModel.setSelectedItem(dockerHostVMSizeComboModel.getElementAt(0));
    }
    dockerHostVMSizeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDockerSelectStorageComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
      }
    });
  }

  private void updateDockerHostRGGroup() {
    dockerHostRGGroup = new ButtonGroup();
    dockerHostRGGroup.add(dockerHostNewRGRadioButton);
    dockerHostRGGroup.add(dockerHostSelectRGRadioButton);
    dockerHostNewRGRadioButton.setSelected(true);
    dockerHostNewRGRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostRGTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerHostResourceGroupName(dockerHostRGTextField.getText())) {
          dockerHostRGLabel.setVisible(false);
        } else {
          dockerHostRGLabel.setVisible(true);
        }
        dockerHostSelectRGComboBox.setEnabled(false);
      }
    });
    dockerHostSelectRGRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostRGTextField.setEnabled(false);
        dockerHostRGLabel.setVisible(false);
        dockerHostSelectRGComboBox.setEnabled(true);
      }
    });
    dockerHostRGLabel.setVisible(false);
    dockerHostRGTextField.setText(newHost.hostVM.resourceGroupName);
    dockerHostRGTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostResourceGroupNameTip());
    dockerHostRGTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostResourceGroupName(((JTextField) input).getText())) {
          dockerHostRGLabel.setVisible(false);
          return true;
        } else {
          dockerHostRGLabel.setVisible(true);
          return false;
        }
      }
    });
    dockerHostSelectRGComboBox.setEnabled(false);

    updateDockerHostSelectRGComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
  }

  private void updateDockerHostSelectRGComboBox(AzureDockerSubscription subscription) {
    DefaultComboBoxModel<String> dockerHostSelectRGComboModel = new DefaultComboBoxModel<>(new Vector<String>(dockerManager.getResourceGroups(subscription)));
    dockerHostSelectRGComboBox.setModel(dockerHostSelectRGComboModel);
  }

  private void updateDockerHostVnetGroup() {
    dockerHostVnetGroup = new ButtonGroup();
    dockerHostVnetGroup.add(dockerHostNewVNetRadioButton);
    dockerHostVnetGroup.add(dockerHostSelectVNetRadioButton);
    dockerHostNewVNetRadioButton.setSelected(true);
    dockerHostNewVNetRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostNewVNetNameTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerVnetName(dockerHostNewVNetNameTextField.getText())) {
          dockerHostNewVNetNameLabel.setVisible(false);
        } else {
          dockerHostNewVNetNameLabel.setVisible(true);
        }
        dockerHostNewVNetAddrSpaceTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerVnetAddrSpace(dockerHostNewVNetAddrSpaceTextField.getText())) {
          dockerHostNewVNetAddrSpaceLabel.setVisible(false);
        } else {
          dockerHostNewVNetAddrSpaceLabel.setVisible(true);
        }
        dockerHostSelectVnetComboBox.setEnabled(false);
        dockerHostSelectSubnetComboBox.setEnabled(false);
      }
    });
    dockerHostSelectVNetRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostNewVNetNameLabel.setVisible(false);
        dockerHostNewVNetNameTextField.setEnabled(false);
        dockerHostNewVNetAddrSpaceLabel.setVisible(false);
        dockerHostNewVNetAddrSpaceTextField.setEnabled(false);
        dockerHostSelectVnetComboBox.setEnabled(true);
        dockerHostSelectSubnetComboBox.setEnabled(true);
      }
    });
    dockerHostNewVNetNameLabel.setVisible(false);
    dockerHostNewVNetNameTextField.setText(newHost.hostVM.vnetName);
    dockerHostNewVNetNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetNameTip());
    dockerHostNewVNetNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerVnetName(((JTextField) input).getText())) {
          dockerHostNewVNetNameLabel.setVisible(false);
          return true;
        } else {
          dockerHostNewVNetNameLabel.setVisible(true);
          return false;
        }
      }
    });
    dockerHostNewVNetAddrSpaceLabel.setVisible(false);
    dockerHostNewVNetAddrSpaceTextField.setText(newHost.hostVM.vnetAddressSpace);
    dockerHostNewVNetAddrSpaceTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetAddrspaceTip());
    dockerHostNewVNetAddrSpaceTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerVnetAddrSpace(((JTextField) input).getText())) {
          dockerHostNewVNetAddrSpaceLabel.setVisible(false);
          return true;
        } else {
          dockerHostNewVNetAddrSpaceLabel.setVisible(true);
          return false;
        }
      }
    });
    dockerHostSelectVnetComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDockerSelectSubnetComboBox((AzureDockerVnet) dockerHostSelectVnetComboBox.getSelectedItem());
      }
    });
    dockerHostSelectVnetComboBox.setEnabled(false);
    dockerHostSelectSubnetComboBox.setEnabled(false);

    updateDockerSelectVnetComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem(), (String) dockerLocationComboBox.getSelectedItem());
  }

  private void updateDockerSelectVnetComboBox(AzureDockerSubscription subscription, String region) {
    List<AzureDockerVnet> dockerVnets = dockerManager.getNetworksAndSubnets(subscription);
    DefaultComboBoxModel<AzureDockerVnet> dockerHostSelectVnetComboModel = new DefaultComboBoxModel<>();
    for (AzureDockerVnet vnet : dockerVnets) {
      if (vnet.region.equals(region)) {
        dockerHostSelectVnetComboModel.addElement(vnet);
      }
    }
    dockerHostSelectVnetComboBox.setModel(dockerHostSelectVnetComboModel);
    if (dockerHostSelectVnetComboModel.getSize() > 0) {
      updateDockerSelectSubnetComboBox(dockerHostSelectVnetComboModel.getElementAt(0));
    }
  }

  private void updateDockerSelectSubnetComboBox(AzureDockerVnet vnet) {
    DefaultComboBoxModel<String> dockerHostSelectSubnetComboModel = (vnet != null) ?
        new DefaultComboBoxModel<>(new Vector<String>(vnet.subnets)) :
        new DefaultComboBoxModel<>();
    dockerHostSelectSubnetComboBox.setModel(dockerHostSelectSubnetComboModel);
  }

  private void updateDockerHostStorageGroup() {
    dockerHostStorageGroup = new ButtonGroup();
    dockerHostStorageGroup.add(dockerHostNewStorageRadioButton);
    dockerHostStorageGroup.add(dockerHostSelectStorageRadioButton);
    dockerHostNewStorageRadioButton.setSelected(true);
    dockerHostNewStorageRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerNewStorageTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerHostStorageName(dockerNewStorageTextField.getText(), null)) {
          dockerNewStorageLabel.setVisible(false);
        } else {
          dockerNewStorageLabel.setVisible(true);
        }
        dockerSelectStorageComboBox.setEnabled(false);
      }
    });
    dockerHostSelectStorageRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerNewStorageTextField.setEnabled(false);
        dockerNewStorageLabel.setVisible(false);
        dockerSelectStorageComboBox.setEnabled(true);
      }
    });
    dockerNewStorageLabel.setVisible(false);
    dockerNewStorageTextField.setText(newHost.hostVM.storageAccountName);
    dockerNewStorageTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostStorageNameTip());
    dockerNewStorageTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostStorageName(((JTextField) input).getText(), null)) {
          dockerNewStorageLabel.setVisible(false);
          return true;
        } else {
          dockerNewStorageLabel.setVisible(true);
          return false;
        }
      }
    });
    dockerSelectStorageComboBox.setEnabled(false);

    updateDockerSelectStorageComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
  }

  private void updateDockerSelectStorageComboBox(AzureDockerSubscription subscription) {
    String vmImageSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    if (vmImageSize != null) {
      String vmImageType = vmImageSize.contains("_DS") ? "Premium_LRS" : "Standard_LRS";
      DefaultComboBoxModel<String> dockerHostStorageComboModel = new DefaultComboBoxModel<>(new Vector<String>(dockerManager.getAvailableStorageAccounts(subscription.id, vmImageType)));
      dockerSelectStorageComboBox.setModel(dockerHostStorageComboModel);
    }
  }

  public DockerHost getDockerHost() {
    return newHost;
  }

  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }


  private ValidationInfo validateDockerHostName(boolean shakeOnError) {
    // Docker virtual machine name
    String hostName = dockerHostNameTextField.getText();
    if (hostName == null || hostName.isEmpty() ||
        !AzureDockerValidationUtils.validateDockerHostName(hostName))
    {
      ValidationInfo info = new ValidationInfo("Missing virtual machine name", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      dockerHostNameTextField.requestFocus();
      dockerHostNameLabel.setVisible(true);
      return info;
    }
    newHost.name = hostName;
    newHost.hostVM.name = hostName;
    newHost.certVault.hostName = hostName;
    newHost.hostVM.publicIpName = hostName + "-pip";

    return null;
  }

  private ValidationInfo validateDockerSubscription(boolean shakeOnError) {
    // Subscription
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    if (currentSubscription == null || currentSubscription.id == null || currentSubscription.id.isEmpty()) {
      ValidationInfo info = new ValidationInfo("Subscription not found", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      dockerSubscriptionComboBox.requestFocus();
      return info;
    }
    newHost.sid = currentSubscription.id;

    return null;
  }

  private ValidationInfo validateDockerLocation(boolean shakeOnError) {
    // Location/region
    String region = (String) dockerLocationComboBox.getSelectedItem();
    if (region == null || region.isEmpty()) {
      ValidationInfo info = new ValidationInfo("Location not found", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      dockerLocationComboBox.requestFocus();
      return info;
    }
    newHost.hostVM.region = region;
    newHost.hostVM.dnsName = String.format("%s.%s.cloudapp.azure.com", newHost.hostVM.name, newHost.hostVM.region);
    newHost.apiUrl = newHost.hostVM.dnsName;

    return null;
  }

  private ValidationInfo validateDockerOSType(boolean shakeOnError) {
    // OS type
    KnownDockerVirtualMachineImage osType = (KnownDockerVirtualMachineImage) dockerHostOSTypeComboBox.getSelectedItem();
    if (osType == null) {
      ValidationInfo info = new ValidationInfo("OS type not found", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      vmKindPanel.requestFocus();
      dockerHostOSTypeComboBox.requestFocus();
      return info;
    }
    newHost.hostOSType = DockerHost.DockerHostOSType.valueOf(osType.toString());
    newHost.hostVM.osHost = osType.getAzureOSHost();

    return null;
  }

  private ValidationInfo validateDockerVMSize(boolean shakeOnError) {
    // Docker virtual machine size
    String vmSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    if (vmSize == null || vmSize.isEmpty()) {
      ValidationInfo info = new ValidationInfo("Virtual machine size not found", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      vmKindPanel.requestFocus();
      dockerHostVMSizeComboBox.requestFocus();
      return info;
    }
    newHost.hostVM.vmSize = vmSize;

    return null;
  }

  private ValidationInfo validateDockerRG(boolean shakeOnError) {
    // Docker resource group name
    if (dockerHostNewRGRadioButton.isSelected()) {
      // New resource group
      String rgName = dockerHostRGTextField.getText();
      if (rgName == null || rgName.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostResourceGroupName(rgName)) {
        ValidationInfo info = new ValidationInfo("Missing resource group name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        rgPanel.requestFocus();
        dockerHostRGTextField.requestFocus();
        dockerHostRGLabel.setVisible(true);
        return info;
      }
      newHost.hostVM.resourceGroupName = rgName;

    } else {
      // Existing resource group
      String rgName = (String) dockerHostSelectRGComboBox.getSelectedItem();
      if (rgName == null || rgName.isEmpty()) {
        ValidationInfo info = new ValidationInfo("Missing resource group name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        rgPanel.requestFocus();
        dockerHostVMSizeComboBox.requestFocus();
        return info;
      }
      // Add "@" to mark this as an existing resource group
      newHost.hostVM.resourceGroupName = rgName + "@";
    }

    return null;
  }

  private ValidationInfo validateDockerVnet(boolean shakeOnError) {
    // Docker virtual network name
    if (dockerHostNewVNetRadioButton.isSelected()) {
      // New virtual network
      String vnetName = dockerHostNewVNetNameTextField.getText();
      if (vnetName == null || vnetName.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerVnetName(vnetName)) {
        ValidationInfo info = new ValidationInfo("Missing virtual network name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        networkPanel.requestFocus();
        dockerHostNewVNetNameTextField.requestFocus();
        dockerHostNewVNetNameLabel.setVisible(true);
        return info;
      }
      String vnetAddrSpace = dockerHostNewVNetAddrSpaceTextField.getText();
      if (vnetAddrSpace == null || vnetAddrSpace.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerVnetAddrSpace(vnetAddrSpace)) {
        ValidationInfo info = new ValidationInfo("Missing virtual network address space", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        networkPanel.requestFocus();
        dockerHostNewVNetAddrSpaceTextField.requestFocus();
        dockerHostNewVNetAddrSpaceLabel.setVisible(true);
        return info;
      }

      newHost.hostVM.vnetName = vnetName;
      newHost.hostVM.vnetAddressSpace = vnetAddrSpace;
      newHost.hostVM.subnetName = "subnet1";
    } else {
      // Existing virtual network and subnet
      AzureDockerVnet vnet = (AzureDockerVnet) dockerHostSelectVnetComboBox.getSelectedItem();
      if (vnet == null || vnet.name == null || vnet.name.isEmpty()) {
        ValidationInfo info = new ValidationInfo("Missing virtual network name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        networkPanel.requestFocus();
        dockerHostVMSizeComboBox.requestFocus();
        return info;
      }
      String subnet = (String) dockerHostSelectSubnetComboBox.getSelectedItem();
      if (subnet == null || subnet.isEmpty()) {
        ValidationInfo info = new ValidationInfo("Missing subnet", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        networkPanel.requestFocus();
        dockerHostVMSizeComboBox.requestFocus();
        return info;
      }

      // Add "@resourceGroupName" to mark this as an existing virtual network
      newHost.hostVM.vnetName = vnet.name + "@" + vnet.resourceGroup;
      newHost.hostVM.vnetAddressSpace = vnet.addrSpace;
      newHost.hostVM.subnetName = subnet;
    }

    return null;
  }

  private ValidationInfo validateDockerStorage(boolean shakeOnError) {
    // Docker storage account
    String vmSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    String storageName;
    if (dockerHostNewStorageRadioButton.isSelected()) {
      // New storage account
      storageName = dockerNewStorageTextField.getText();
      if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostStorageName(storageName, (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem())) {
        ValidationInfo info = new ValidationInfo("Missing storage account name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        storagePanel.requestFocus();
        dockerNewStorageTextField.requestFocus();
        dockerNewStorageLabel.setVisible(true);
        return info;
      }

      newHost.hostVM.storageAccountName = storageName;
      newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
    } else {
      // Existing resource group
      storageName = (String) dockerSelectStorageComboBox.getSelectedItem();
      if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty()) {
        ValidationInfo info = new ValidationInfo("Missing storage account name", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        storagePanel.requestFocus();
        dockerHostVMSizeComboBox.requestFocus();
        return info;
      }
      // Add "@" to mark this as an existing storage account
      newHost.hostVM.storageAccountName = storageName + "@";
      newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
    }

    return null;
  }

  private ValidationInfo doValidate(boolean shakeOnError) {
    ValidationInfo result;

    result = validateDockerHostName(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerSubscription(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerLocation(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerOSType(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerVMSize(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerRG(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerVnet(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerStorage(shakeOnError);
    if (result != null) {
      return result;
    }

    return null;
  }

  private void setFinishButtonState() {
    model.getCurrentNavigationState().FINISH.setEnabled(true);
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootConfigureContainerPanel.revalidate();
    setFinishButtonState();

    return rootConfigureContainerPanel;
  }

  @Override
  public WizardStep onNext(final AzureNewDockerWizardModel model) {
    if (doValidate() == null) {
      return super.onNext(model);
    } else {
      return this;
    }
  }

  @Override
  public boolean onFinish() {
    return model.doValidate() == null && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    model.finishedOK = true;

    return super.onCancel();
  }
}
