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
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

public class DockerHostNode extends AzureRefreshableNode {
  //TODO: Replace the icons with the real Docker host icons
  private static final String WAIT_ICON_PATH = "virtualmachinewait.png";
  private static final String STOP_ICON_PATH = "virtualmachinestop.png";
  private static final String RUN_ICON_PATH = "virtualmachinerun.png";

  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_DOWNLOAD_RDP_FILE = "Connect";
  public static final String ACTION_SHUTDOWN = "Shutdown";
  public static final String ACTION_START = "Start";
  public static final String ACTION_RESTART = "Restart";

  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  private boolean isLoaded;

  public DockerHostNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost)
      throws AzureCmdException {
    super(dockerHost.apiUrl, dockerHost.name, parent, WAIT_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;
    isLoaded = false;

    loadActions();

    // update vm icon based on vm status
    //refreshItemsInternal();
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

    isLoaded = true;
  }

  public DockerHost getDockerHost() {
    return dockerHost;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

}
