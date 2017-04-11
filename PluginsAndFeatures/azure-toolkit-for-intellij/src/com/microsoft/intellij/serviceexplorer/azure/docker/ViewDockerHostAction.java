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
package com.microsoft.intellij.serviceexplorer.azure.docker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.intellij.docker.dialogs.AzureEditDockerLoginCredsDialog;
import com.microsoft.intellij.docker.dialogs.AzureViewDockerDialog;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;


@Name("Details")
public class ViewDockerHostAction extends NodeActionListener {
  private static final Logger LOGGER = Logger.getInstance(ViewDockerHostAction.class);
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  Project project;
  DockerHostNode dockerHostNode;

  public ViewDockerHostAction(DockerHostNode dockerHostNode) {
    this.dockerManager = dockerHostNode.getDockerManager();
    this.dockerHost = dockerHostNode.getDockerHost();
    this.project = (Project) dockerHostNode.getProject();
    this.dockerHostNode = dockerHostNode;
  }

  @Override
  public void actionPerformed(NodeActionEvent e) {
    AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(project, dockerHost, dockerManager);
    viewDockerDialog.show();

    if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
      EditableDockerHost editableDockerHost = new EditableDockerHost(dockerHost);

      AzureEditDockerLoginCredsDialog loginCredsDialog = new AzureEditDockerLoginCredsDialog(project, editableDockerHost, dockerManager);
      loginCredsDialog.show();

      if (loginCredsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        // Update Docker host log in credentials
        dockerHost.isUpdating = true;
        DefaultLoader.getIdeHelper().runInBackground(project, String.format("Updating %s Log In Credentials", dockerHost.name), false, true, String.format("Updating log in credentials for %s...", dockerHost.name), new Runnable() {
          @Override
          public void run() {
            try {
              AzureDockerVMOps.updateDockerHostVM(dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient, editableDockerHost.updatedDockerHost);
              dockerHost.certVault = editableDockerHost.updatedDockerHost.certVault;
              dockerHost.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
              dockerHost.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
              Session session = AzureDockerSSHOps.createLoginInstance(dockerHost);
              AzureDockerVMOps.UpdateCurrentDockerUser(session);
              dockerHost.session = session;
            } catch (Exception ee) {
              if (AzureDockerUtils.DEBUG) ee.printStackTrace();
              LOGGER.error("onEditDockerHostAction", ee);
            }
            dockerHost.isUpdating = false;
          }
        });
      }
    }
  }
}
