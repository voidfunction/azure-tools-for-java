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
package com.microsoft.tooling.msservices.serviceexplorer.azure.docker;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

public class DockerHostModule extends AzureRefreshableNode {
  private static final String DOCKER_HOST_MODULE_ID = DockerHostModule.class.getName();
  private static final String DOCKER_HOST_ICON = "virtualmachines.png";
  private static final String BASE_MODULE_NAME = "Docker Hosts";

  private boolean isLoaded;
  AzureDockerHostsManager dockerManager;

  public DockerHostModule(Node parent) {
    super(DOCKER_HOST_MODULE_ID, BASE_MODULE_NAME, parent, DOCKER_HOST_ICON);
    isLoaded = false;
    dockerManager = null;
  }

  @Override
  protected void onNodeClick(NodeActionEvent e) {
    if (!isLoaded || dockerManager == null) {
      this.load();
    }
  }

  @Override
  protected void refreshItems() throws AzureCmdException {
    isLoaded = false;
    // remove all child nodes
    removeAllChildNodes();

    try {
      AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
      // not signed in
      if (azureAuthManager == null) {
        return;
      }

      dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);
      // TODO: Implement progress bar steps and split the force refresh into multiple chunks
      dockerManager.forceRefreshSubscriptions();

      for (DockerHost host : dockerManager.getDockerHostsList()) {
        addChildNode(new DockerHostNode(this, dockerManager, host));
      }

      isLoaded = true;
    } catch (Exception ex) {
      DefaultLoader.getUIHelper().showException("An error occurred while attempting to load the Docker virtual machines from Azure", ex,
          "Azure Services Explorer - Error Deleting Web App", false, true);
    }
  }
}
