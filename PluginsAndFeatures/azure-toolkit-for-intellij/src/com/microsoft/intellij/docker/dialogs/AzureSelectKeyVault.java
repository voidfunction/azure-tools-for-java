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
package com.microsoft.intellij.docker.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureSelectKeyVault extends DialogWrapper{
  private JPanel rootPanel;
  private JComboBox<String> dockerKeyvaultsComboBox;

  private AzureDockerHostsManager dockerManager;
  private String keyvault;

  public AzureSelectKeyVault(Project project, AzureDockerHostsManager dockerManager) {
    super(project, false);

    this.dockerManager = dockerManager;
    keyvault = null;

    // TODO: call into dockerManager to retrieve the list of current keyvaults
    DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
    comboBoxModel.addElement("dockerVault121121");
    comboBoxModel.addElement("dockerVault121");
    dockerKeyvaultsComboBox.setModel(comboBoxModel);

    init();
    setTitle("Select Azure Key Vault");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  @Override
  protected void doOKAction() {
    keyvault = (String) dockerKeyvaultsComboBox.getSelectedItem();
    super.doOKAction();
  }

  public String getSelectedKeyvault() {
    return keyvault;
  }

}
