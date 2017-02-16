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
package com.microsoft.intellij.docker.forms;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.dialogs.AzureSelectKeyVault;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureDockerHostUpdateLoginPanel {
  private JPanel contentPane;
  private JPanel mainPanel;
  private JRadioButton dockerHostAutoSshRadioButton;
  private JRadioButton dockerHostImportSshRadioButton;
  private JTextField dockerHostUsernameTextField;
  private JPasswordField dockerHostFirstPwdField;
  private JPasswordField dockerHostSecondPwdField;
  private TextFieldWithBrowseButton dockerHostImportSSHBrowseTextField;
  private JRadioButton dockerHostKeepSshRadioButton;
  private JButton copyFromAzureKeyButton;
  private JRadioButton dockerHostNoSshRadioButton;
  private ButtonGroup authSelectionGroup;


  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;

  public AzureDockerHostUpdateLoginPanel(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager) {
    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerUIManager;

    authSelectionGroup = new ButtonGroup();
    authSelectionGroup.add(dockerHostNoSshRadioButton);
    authSelectionGroup.add(dockerHostKeepSshRadioButton);
    authSelectionGroup.add(dockerHostAutoSshRadioButton);
    authSelectionGroup.add(dockerHostImportSshRadioButton);

    initDefaultUI();

    copyFromAzureKeyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureSelectKeyVault selectKeyvaultDialog = new AzureSelectKeyVault(project, dockerUIManager);
        selectKeyvaultDialog.show();

        if (selectKeyvaultDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          updateUIWithKeyvault(selectKeyvaultDialog.getSelectedKeyvault());
        }
      }
    });
    dockerHostNoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(false);
      }
    });
    dockerHostKeepSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(false);
      }
    });
    dockerHostAutoSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(false);
      }
    });
    dockerHostImportSshRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportSshRadioButton.setEnabled(true);
      }
    });

    dockerHostImportSSHBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportSSHBrowseTextField, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    dockerHostImportSSHBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        return AzureDockerValidationUtils.validateDockerHostSshDirectory(dockerHostImportSSHBrowseTextField.getText());
      }
    });


  }

  private void initDefaultUI() {
    String currentUserAuth = editableHost.originalDockerHost.certVault.vmUsername;
    if (editableHost.originalDockerHost.hasPwdLogIn) {
      dockerHostFirstPwdField.setText(editableHost.originalDockerHost.certVault.vmPwd);
      dockerHostSecondPwdField.setText(editableHost.originalDockerHost.certVault.vmPwd);
    }
    if (editableHost.originalDockerHost.hasSSHLogIn) {
      dockerHostKeepSshRadioButton.setSelected(true);
    } else {
      dockerHostNoSshRadioButton.setSelected(true);
    }
    if (editableHost.isUpdated) {
      currentUserAuth += " (updating...)";
      dockerHostFirstPwdField.setEnabled(false);
      dockerHostSecondPwdField.setEnabled(false);
      dockerHostUsernameTextField.setEnabled(false);
      dockerHostNoSshRadioButton.setEnabled(false);
      dockerHostKeepSshRadioButton.setEnabled(false);
      dockerHostAutoSshRadioButton.setEnabled(false);
      dockerHostImportSshRadioButton.setEnabled(false);
    }
    dockerHostUsernameTextField.setText(currentUserAuth);
    dockerHostImportSSHBrowseTextField.setEnabled(false);
  }

  private void updateUIWithKeyvault(String keyvault) {
    // TODO: call into dockerManager to retrieve the keyvault secrets
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
