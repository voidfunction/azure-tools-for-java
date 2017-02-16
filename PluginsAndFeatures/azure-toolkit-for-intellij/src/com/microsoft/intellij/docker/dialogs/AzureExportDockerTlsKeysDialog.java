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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AzureExportDockerTlsKeysDialog extends DialogWrapper {
  private JPanel mainPanel;
  private TextFieldWithBrowseButton exportTlsPath;

  private Project project;
  private AzureDockerCertVault certVault;

  public AzureExportDockerTlsKeysDialog(Project project, AzureDockerCertVault certVault) {
    super(project, true);

    this.project = project;
    this.certVault = certVault;

    setModal(true);

    exportTlsPath.addActionListener(UIUtils.createFileChooserListener(exportTlsPath, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    exportTlsPath.setText(project.getBasePath() + "/out/Docker/tls");
    exportTlsPath.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        return Files.isDirectory(Paths.get(exportTlsPath.getText()));
      }
    });

    init();
    setTitle("Export TLS Certificates");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return null;
  }

  @Nullable
  @Override
  protected Action[] createActions() {
    Action exportAction = getOKAction();
    exportAction.putValue(Action.NAME, "Save");
    return new Action[] {getCancelAction(), exportAction};
  }

  @Nullable
  @Override
  protected void doOKAction() {
    try {
      AzureDockerCertVaultOps.saveToLocalFiles(exportTlsPath.getText(), certVault);
      super.doOKAction();
    }
    catch (Exception e){
      String msg = "An error occurred while attempting to export the TLS keys.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

}
