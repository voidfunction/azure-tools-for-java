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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardStep;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class AzureNewDockerLoginStep extends AzureNewDockerWizardStep {
  private JPanel rootConfigureContainerPanel;
  private ButtonGroup groupNewCredsType;
  private JRadioButton dockerHostImportKeyvaultCredsRadioButton;
  private JComboBox<AzureDockerCertVault> dockerHostImportKeyvaultComboBox;
  private JRadioButton dockerHostNewCredsRadioButton;
  private JTabbedPane credsTabbedPane;
  private JPanel vmCredsPanel;
  private JTextField dockerHostUsernameTextField;
  private JPasswordField dockerHostFirstPwdField;
  private JPasswordField dockerHostSecondPwdField;
  private ButtonGroup groupSSH;
  private JRadioButton dockerHostNoSshRadioButton;
  private JRadioButton dockerHostAutoSshRadioButton;
  private JRadioButton dockerHostImportSshRadioButton;
  private TextFieldWithBrowseButton dockerHostImportSSHBrowseTextField;
  private JPanel daemonCredsPanel;
  private JTextField dockerDaemonPortTextField;
  private ButtonGroup groupTLS;
  private JRadioButton dockerHostNoTlsRadioButton;
  private JRadioButton dockerHostAutoTlsRadioButton;
  private JRadioButton dockerHostImportTlsRadioButton;
  private TextFieldWithBrowseButton dockerHostImportTLSBrowseTextField;
  private JCheckBox dockerHostSaveCredsCheckBox;
  private JTextField dockerHostNewKeyvaultTextField;
  private JLabel dockerDaemonPortLabel;
  private JLabel dockerHostNewKeyvaultLabel;
  private JLabel dockerHostImportTLSBrowseLabel;
  private JLabel dockerHostUsernameLabel;
  private JLabel dockerHostFirstPwdLabel;
  private JLabel dockerHostImportSSHBrowseLabel;

  private AzureNewDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private DockerHost newHost;

  public AzureNewDockerLoginStep(String title, AzureNewDockerWizardModel model) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure the new Docker virtual machine log in credentials and port settings");
    this.model = model;
    this.dockerManager = model.getDockerManager();
    this.newHost = model.getDockerHost();

    initDialog();
    dockerHostImportKeyvaultCredsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportKeyvaultComboBox.setEnabled(true);

        dockerHostUsernameLabel.setVisible(false);
        dockerHostUsernameTextField.setEnabled(false);
        dockerHostFirstPwdLabel.setVisible(false);
        dockerHostFirstPwdField.setEnabled(false);
        dockerHostSecondPwdField.setEnabled(false);
        dockerHostNoSshRadioButton.setEnabled(false);
        dockerHostAutoSshRadioButton.setEnabled(false);
        dockerHostImportSshRadioButton.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostNoTlsRadioButton.setEnabled(false);
        dockerHostAutoTlsRadioButton.setEnabled(false);
        dockerHostImportTlsRadioButton.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
        dockerHostImportTLSBrowseTextField.setEnabled(false);
      }
    });
    dockerHostNewCredsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportKeyvaultComboBox.setEnabled(false);

        dockerHostUsernameTextField.setEnabled(true);
        dockerHostFirstPwdField.setEnabled(true);
        dockerHostSecondPwdField.setEnabled(true);
        dockerHostNoSshRadioButton.setEnabled(true);
        dockerHostAutoSshRadioButton.setEnabled(true);
        dockerHostImportSshRadioButton.setEnabled(true);
        if (dockerHostImportSshRadioButton.isSelected()) {
          dockerHostImportSSHBrowseTextField.setEnabled(true);
        }
        dockerDaemonPortTextField.setEnabled(true);
        dockerHostNoTlsRadioButton.setEnabled(true);
        dockerHostAutoTlsRadioButton.setEnabled(true);
        dockerHostImportTlsRadioButton.setEnabled(true);
        if (dockerHostImportTlsRadioButton.isSelected()) {
          dockerHostImportTLSBrowseTextField.setEnabled(true);
        }

      }
    });
    dockerHostNoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
      }
    });
    dockerHostAutoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(false);
        dockerHostImportSSHBrowseLabel.setVisible(false);
      }
    });
    dockerHostImportSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSSHBrowseTextField.setEnabled(true);
      }
    });
    dockerHostImportSSHBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportSSHBrowseTextField, model.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));

    dockerHostNoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
      }
    });
    dockerHostAutoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
        dockerHostImportTLSBrowseLabel.setVisible(false);
      }
    });
    dockerHostImportTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(true);
      }
    });
    dockerHostImportTLSBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportTLSBrowseTextField, model.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
  }

  private void initDialog() {
    // New or import Key Vault credentials
    groupNewCredsType = new ButtonGroup();
    groupNewCredsType.add(dockerHostImportKeyvaultCredsRadioButton);
    groupNewCredsType.add(dockerHostNewCredsRadioButton);
    dockerHostNewCredsRadioButton.setSelected(true);
    dockerHostImportKeyvaultComboBox.setEnabled(false);
    dockerHostImportKeyvaultComboBox.setModel(new DefaultComboBoxModel<AzureDockerCertVault>(new Vector<AzureDockerCertVault>(dockerManager.getDockerKeyVaults())));

    groupSSH = new ButtonGroup();
    groupSSH.add(dockerHostNoSshRadioButton);
    groupSSH.add(dockerHostAutoSshRadioButton);
    groupSSH.add(dockerHostImportSshRadioButton);
    dockerHostAutoSshRadioButton.setSelected(true);
    dockerHostImportSSHBrowseLabel.setVisible(false);
    dockerHostImportSSHBrowseTextField.setEnabled(false);
    dockerHostImportSSHBrowseTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostSshDirectoryTip());
    dockerHostImportSSHBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportSSHBrowseTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(text)) {
          dockerHostImportSSHBrowseLabel.setVisible(true);
          return false;
        } else {
          dockerHostImportSSHBrowseLabel.setVisible(false);
          return true;
        }
      }
    });
    dockerHostUsernameLabel.setVisible(false);
    dockerHostUsernameTextField.setText(newHost.certVault.vmUsername);
    dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
    dockerHostUsernameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostUsernameTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostUserName(text)) {
          dockerHostUsernameLabel.setVisible(true);
          return false;
        } else {
          dockerHostUsernameLabel.setVisible(false);
          return true;
        }
      }
    });
    dockerHostFirstPwdField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = new String(dockerHostFirstPwdField.getPassword());
        if (text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostPassword(text)) {
          dockerHostFirstPwdLabel.setVisible(true);
          return false;
        } else {
          dockerHostFirstPwdLabel.setVisible(false);
          return true;
        }
      }
    });
    dockerHostFirstPwdLabel.setVisible(false);
    dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
    dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());

    dockerDaemonPortLabel.setVisible(false);
    dockerDaemonPortTextField.setText(newHost.port);
    dockerDaemonPortTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostPortTip());
    dockerDaemonPortTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerDaemonPortTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostPort(text)) {
          dockerDaemonPortLabel.setVisible(true);
          return false;
        } else {
          dockerDaemonPortLabel.setVisible(false);
          return true;
        }
      }
    });
    groupTLS = new ButtonGroup();
    groupTLS.add(dockerHostNoTlsRadioButton);
    groupTLS.add(dockerHostAutoTlsRadioButton);
    groupTLS.add(dockerHostImportTlsRadioButton);
    dockerHostAutoTlsRadioButton.setSelected(true);
    dockerHostImportTLSBrowseLabel.setVisible(false);
    dockerHostImportTLSBrowseTextField.setEnabled(false);
    dockerHostImportTLSBrowseTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostTlsDirectoryTip());
    dockerHostImportTLSBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostImportTLSBrowseTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostTlsDirectory(text)) {
          dockerHostImportTLSBrowseLabel.setVisible(true);
          return false;
        } else {
          dockerHostImportTLSBrowseLabel.setVisible(false);
          return true;
        }
      }
    });

    dockerHostSaveCredsCheckBox.setSelected(true);
    dockerHostNewKeyvaultLabel.setVisible(false);
    dockerHostNewKeyvaultTextField.setText(newHost.certVault.name);
    dockerHostNewKeyvaultTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostKeyvaultNameTip());
    dockerHostNewKeyvaultTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        String text = dockerHostNewKeyvaultTextField.getText();
        if (text == null || text.isEmpty() || !AzureDockerValidationUtils.validateDockerHostKeyvaultName(text, dockerManager)) {
          dockerHostNewKeyvaultLabel.setVisible(true);
          return false;
        } else {
          dockerHostNewKeyvaultLabel.setVisible(false);
          return true;
        }
      }
    });
  }

  public DockerHost getDockerHost() {
    return newHost;
  }

  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }

  private ValidationInfo doValidate(boolean shakeOnError) {
    if (dockerHostImportKeyvaultCredsRadioButton.isSelected()) {
      // read key vault secrets and set the credentials for the new host
      AzureDockerCertVault certVault = (AzureDockerCertVault) dockerHostImportKeyvaultComboBox.getSelectedItem();
      if (certVault == null) {
        ValidationInfo info = new ValidationInfo("Missing vault", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        dockerHostImportKeyvaultComboBox.requestFocus();
        return info;
      }

      newHost.certVault.name = certVault.name;
      newHost.certVault.resourceGroupName = certVault.resourceGroupName;
      newHost.certVault.region = certVault.region;
      newHost.certVault.uri = certVault.uri;
      AzureDockerCertVaultOps.copyVaultLoginCreds(newHost.certVault, certVault);
      AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
      AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);

      // create a weak link (resource tag) between the virtual machine and the key vault
      //  we will not create/update the key vault unless the user checks the specific option
      newHost.certVault.hostName = null;
      newHost.hasKeyVault = true;
    } else {
      // User name
      String vmUsername = dockerHostUsernameTextField.getText();
      if (vmUsername == null || vmUsername.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostUserName(vmUsername))
      {
        ValidationInfo info = new ValidationInfo("Missing username", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        vmCredsPanel.requestFocus();
        dockerHostUsernameTextField.requestFocus();
        dockerHostUsernameLabel.setVisible(true);
        return info;
      }
      newHost.certVault.vmUsername = vmUsername;

      // Password login
      String vmPwd1 = new String(dockerHostFirstPwdField.getPassword());
      String vmPwd2 = new String(dockerHostSecondPwdField.getPassword());
      if ((dockerHostNoSshRadioButton.isSelected() || dockerHostFirstPwdField.getPassword().length > 0 ||
          dockerHostSecondPwdField.getPassword().length > 0) &&
          (vmPwd1.isEmpty() || vmPwd2.isEmpty() || ! vmPwd1.equals(vmPwd2) ||
          !AzureDockerValidationUtils.validateDockerHostPassword(vmPwd1)))
      {
        ValidationInfo info = new ValidationInfo("Incorrect password", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        vmCredsPanel.requestFocus();
        dockerHostFirstPwdField.requestFocus();
        dockerHostFirstPwdLabel.setVisible(true);
        return info;
      }
      if (dockerHostFirstPwdField.getPassword().length > 0) {
        newHost.certVault.vmPwd = new String(dockerHostFirstPwdField.getPassword());
        newHost.hasPwdLogIn = true;
      }

      // SSH key auto generated
      if (dockerHostAutoSshRadioButton.isSelected()) {
        AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH keys for " + newHost.name);
        AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
        newHost.hasSSHLogIn = true;
      }

      // SSH key imported from local file directory
      if (dockerHostImportSshRadioButton.isSelected()) {
        if (dockerHostImportSSHBrowseTextField.getText() == null || dockerHostImportSSHBrowseTextField.getText().isEmpty() ||
            !AzureDockerValidationUtils.validateDockerHostSshDirectory(dockerHostImportSSHBrowseTextField.getText())) {
          ValidationInfo info = new ValidationInfo("SHH key files were not found in the selected directory", model.getNewDockerWizardDialog().getContentPanel());
          if (shakeOnError) {
            model.DialogShaker(info);
          }
          vmCredsPanel.requestFocus();
          dockerHostImportSSHBrowseTextField.requestFocus();
          dockerHostImportSSHBrowseLabel.setVisible(true);
          return info;
        } else {
          AzureDockerCertVault certVault = AzureDockerCertVaultOps.getSSHKeysFromLocalFile(dockerHostImportSSHBrowseTextField.getText());
          AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
          newHost.hasSSHLogIn = true;
        }
      }

      // No Docker daemon security
      if (dockerHostNoTlsRadioButton.isSelected()) {
        newHost.isTLSSecured = false;
      }

      // TLS certs auto generated
      if (dockerHostAutoTlsRadioButton.isSelected()) {
        AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateTLSCerts("TLS certs for " + newHost.name);
        AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
        newHost.isTLSSecured = true;
      }

      // TLS certs imported from local file directory
      if (dockerHostImportTlsRadioButton.isSelected()) {
        if (dockerHostImportTLSBrowseTextField.getText() == null || dockerHostImportTLSBrowseTextField.getText().isEmpty() ||
            !AzureDockerValidationUtils.validateDockerHostTlsDirectory(dockerHostImportTLSBrowseTextField.getText())) {
          ValidationInfo info = new ValidationInfo("SHH key files were not found in the selected directory", model.getNewDockerWizardDialog().getContentPanel());
          if (shakeOnError) {
            model.DialogShaker(info);
          }
          daemonCredsPanel.requestFocus();
          dockerHostImportTLSBrowseTextField.requestFocus();
          dockerHostImportTLSBrowseLabel.setVisible(true);
          return info;
        } else {
          AzureDockerCertVault certVault = AzureDockerCertVaultOps.getTLSCertsFromLocalFile(dockerHostImportTLSBrowseTextField.getText());
          AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
          newHost.isTLSSecured = true;
        }
      }
    }

    // Docker daemon port settings
    if (dockerDaemonPortTextField.getText() == null || dockerDaemonPortTextField.getText().isEmpty() ||
        !AzureDockerValidationUtils.validateDockerHostUserName(dockerDaemonPortTextField.getText()))
    {
      ValidationInfo info = new ValidationInfo("Invalid Docker daemon port settings", model.getNewDockerWizardDialog().getContentPanel());
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      daemonCredsPanel.requestFocus();
      dockerDaemonPortTextField.requestFocus();
      dockerDaemonPortLabel.setVisible(true);
      return info;
    }
    newHost.port = dockerDaemonPortTextField.getText();

    // create new key vault for storing the credentials
    if (dockerHostSaveCredsCheckBox.isSelected()) {
      if (dockerHostNewKeyvaultTextField.getText() == null || dockerHostNewKeyvaultTextField.getText().isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostKeyvaultName(dockerHostNewKeyvaultTextField.getText(), dockerManager)) {
        ValidationInfo info = new ValidationInfo("Incorrect Azure Key Vault", model.getNewDockerWizardDialog().getContentPanel());
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        dockerHostNewKeyvaultTextField.requestFocus();
        dockerHostNewKeyvaultLabel.setVisible(true);
        return info;
      } else {
        newHost.hasKeyVault = true;
        newHost.certVault.name = dockerHostNewKeyvaultTextField.getText();
        newHost.certVault.hostName = (newHost.name != null) ? newHost.name : null;
        newHost.certVault.region = (newHost.hostVM.region != null) ? newHost.hostVM.region : null;
        newHost.certVault.resourceGroupName = (newHost.hostVM.resourceGroupName != null) ? newHost.hostVM.resourceGroupName : null;
        newHost.certVault.uri = (newHost.hostVM.region != null && newHost.hostVM.resourceGroupName != null) ?
            "https://" + newHost.certVault.name + ".vault.azure.net" :
            null;
      }
    } else {
      newHost.certVault.hostName = null;
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
