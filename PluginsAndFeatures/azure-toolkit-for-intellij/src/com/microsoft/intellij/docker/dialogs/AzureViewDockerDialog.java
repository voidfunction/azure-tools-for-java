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
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

public class AzureViewDockerDialog extends DialogWrapper {
  private final String defaultTitle = "Viewing %s";
  public static final int OK_EXIT_CODE = 0;
  public static final int CANCEL_EXIT_CODE = 1;
  public static final int CLOSE_EXIT_CODE = 1;
  public static final int UPDATE_EXIT_CODE = 3;
  private JPanel mainPanel;
  private JLabel dockerHostNameLabel;
  private JTabbedPane tabbedPane1;
  private JLabel dockerHostUsernameLabel;
  private JLabel dockerHostPwdLoginLabel;
  private JLabel dockerHostSshLoginLabel;
  private JLabel dockerHostTlsAuthLabel;
  private JLabel dockerHostKeyvaultLabel;
  private JXHyperlink dockerHostAuthUpdateHyperlink;
  private JXHyperlink dockerHostSshExportHyperlink;
  private JXHyperlink dockerHostTlsExportHyperlink;
  private JLabel dockerHostOSTypeLabel;
  private JLabel dockerHostVMSizeLabel;
  private JLabel dockerHostRGNameLabel;
  private JLabel dockerHostVnetNameAddrLabel;
  private JLabel dockerHostSubnetNameAddrLabel;
  private JLabel dockerHostStorageNameTypeLabel;
  private JLabel dockerHostUrlLabel;
  private JLabel dockerHostLocationLabel;
  private JLabel dockerHostStatusLabel;
  private JLabel dockerHostPortLabel;

  private Action myClickApplyAction;
  private Project project;
  private DockerHost dockerHost;
  private AzureDockerHostsManager dockerManager;
  private int exitCode;

  private void initDefaultUIValues(String updating) {
    // Docker VM info
    dockerHostNameLabel.setText(dockerHost.name);
    dockerHostUrlLabel.setText(dockerHost.apiUrl);
    dockerHostLocationLabel.setText(dockerHost.hostVM.region);
    dockerHostStatusLabel.setText((updating != null) ?
        dockerHost.state.toString() + updating :
        dockerHost.state.toString()
    );

    // Docker VM settings
    dockerHostOSTypeLabel.setText(dockerHost.hostOSType.toString());
    // TODO: enable resizing of the current VM -> see VirtualMachine::availableSizes() and update.withSize();
    dockerHostVMSizeLabel.setText((updating != null) ?
        dockerHost.hostVM.vmSize + updating :
        dockerHost.hostVM.vmSize
    );
    dockerHostRGNameLabel.setText(dockerHost.hostVM.resourceGroupName);
    dockerHostVnetNameAddrLabel.setText(String.format("%s (%s)", dockerHost.hostVM.vnetName, dockerHost.hostVM.vnetAddressSpace));
    dockerHostSubnetNameAddrLabel.setText(String.format("%s (%s)", dockerHost.hostVM.subnetName, dockerHost.hostVM.subnetAddressRange));
    dockerHostStorageNameTypeLabel.setText(String.format("%s (%s)", dockerHost.hostVM.storageAccountName, dockerHost.hostVM.storageAccountType));

    // Docker VM log in settings
    dockerHostAuthUpdateHyperlink.setEnabled(!dockerHost.isUpdating);
    dockerHostUsernameLabel.setText((updating != null) ?
        dockerHost.certVault.vmUsername + updating :
        dockerHost.certVault.vmUsername
    );
    dockerHostPwdLoginLabel.setText((updating != null) ?
        (dockerHost.hasPwdLogIn ? "Yes" : "No") + updating :
        (dockerHost.hasPwdLogIn ? "Yes" : "No")
    );
    dockerHostSshLoginLabel.setText((updating != null) ?
        (dockerHost.hasSSHLogIn ? "Yes" : "No") + updating :
        (dockerHost.hasSSHLogIn ? "Yes" : "No")
    );
    dockerHostSshExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.hasSSHLogIn);

    // Docker Daemon settings
    dockerHostTlsAuthLabel.setText((updating != null) ?
        (dockerHost.hasSSHLogIn ? "Using TLS certificates" : "Open/unsecured access") + updating :
        (dockerHost.hasSSHLogIn ? "Using TLS certificates" : "Open/unsecured access")
    );
    dockerHostTlsExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.isTLSSecured);

    dockerHostPortLabel.setText((updating != null) ?
            dockerHost.port + updating :
            dockerHost.port
    );

    // Docker Keyvault settings
    dockerHostKeyvaultLabel.setText((updating != null) ?
        (dockerHost.hasKeyVault ? dockerHost.certVault.uri : "Not using Key Vault") + updating :
        (dockerHost.hasKeyVault ? dockerHost.certVault.uri : "Not using Key Vault")
    );

    exitCode = CLOSE_EXIT_CODE;

//    myClickApplyAction.setEnabled(!editableHost.originalDockerHost.equalsTo(dockerHost));
  }

  public AzureViewDockerDialog(Project project, DockerHost host, AzureDockerHostsManager dockerManager) {
    super(project, true);

    this.project = project;
    this.dockerHost = host;
    this.dockerManager = dockerManager;
    setModal(true);

    init();
    dockerHostAuthUpdateHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onUpdateLoginCreds();
      }
    });
    dockerHostSshExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportSshKeys();
      }
    });
    dockerHostTlsExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportTlsCerts();
      }
    });

    if (dockerHost.isUpdating) {
      initDefaultUIValues(" (updating...)");
    } else {
      initDefaultUIValues(null);
    }
    setTitle(String.format(defaultTitle, dockerHost.name));

    dockerHostSshExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportSshKeys();
      }
    });

    dockerHostTlsExportHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onExportTlsCerts();
      }
    });
  }

  private void onUpdateLoginCreds() {
    exitCode = UPDATE_EXIT_CODE;
    doOKAction();
  }

  private void onExportSshKeys() {
    if (dockerHost.hasSSHLogIn && dockerHost.certVault != null) {
      AzureExportDockerSshKeysDialog exportDockerSshKeysDialog = new AzureExportDockerSshKeysDialog(project, dockerHost.certVault);
      exportDockerSshKeysDialog.show();
    }
  }

  private void onExportTlsCerts() {
    if (dockerHost.isTLSSecured && dockerHost.certVault != null) {
      AzureExportDockerTlsKeysDialog exportDockerTlsKeysDialog = new AzureExportDockerTlsKeysDialog(project, dockerHost.certVault);
      exportDockerTlsKeysDialog.show();
    }
  }

  public int getInternalExitCode() {
    return exitCode;
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

  @Override
  protected void doOKAction() {
    super.doOKAction();
  }

}
