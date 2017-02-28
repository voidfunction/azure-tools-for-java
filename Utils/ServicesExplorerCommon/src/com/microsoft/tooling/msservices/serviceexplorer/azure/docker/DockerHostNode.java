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
import com.microsoft.azure.docker.model.DockerImage;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

public class DockerHostNode extends AzureRefreshableNode {
  //TODO: Replace the icons with the real Docker host icons
  private static final String DOCKERHOST_WAIT_ICON_PATH = "virtualmachinewait.png";
  private static final String DOCKERHOST_STOP_ICON_PATH = "virtualmachinestop.png";
  private static final String DOCKERHOST_RUN_ICON_PATH = "virtualmachinerun.png";
  private static final String DOCKERHOST_STARTING_ICON_PATH = "storagequery.png";

  public static final String ACTION_START = "Start";
  public static final String ACTION_RESTART = "Restart";
  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_SSH_CONNECT = "Connect";
  public static final String ACTION_SHUTDOWN = "Shutdown";
  public static final String ACTION_VIEW = "Details";
  public static final String ACTION_DEPLOY = "Publish";

  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  private boolean isLoaded;

  public DockerHostNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost)
      throws AzureCmdException {
    super(dockerHost.apiUrl, dockerHost.name, parent, DOCKERHOST_WAIT_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;
    isLoaded = true; // Docker hosts are loaded by the parent node

    loadActions();

    // update vm icon based on vm status
    //refreshItemsInternal();
    setIconPath(getDockerHostIcon());
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
      Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
      VirtualMachine vm = azureClient.virtualMachines().getByGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
      if (vm != null) {
        DockerHost updatedDockerHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
        if (updatedDockerHost != null) {
          dockerHost = updatedDockerHost;
          setName(dockerHost.name);
          setIconPath(getDockerHostIcon());

          for (DockerImage dockerImage : updatedDockerHost.dockerImages.values()) {
            addChildNode(new DockerImageNode(this, dockerManager, updatedDockerHost, dockerImage));
          }

          isLoaded = true;
        }
      }
    } catch (Exception e) {}
  }

  private String getDockerHostIcon() {
    switch (dockerHost.state) {
      case RUNNING:
        return DOCKERHOST_RUN_ICON_PATH;
      case STOPPED:
      case UNKNOWN:
        return DOCKERHOST_STOP_ICON_PATH;
      case DEALLOCATING:
      case DEALLOCATED:
      case STARTING:
      case UPDATING:
      default:
        return DOCKERHOST_WAIT_ICON_PATH;
    }
  }

  public DockerHost getDockerHost() {
    return dockerHost;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

  @Override
  protected void loadActions() {
    addAction(ACTION_START, DOCKERHOST_STARTING_ICON_PATH, new NodeActionListener() {
      @Override
      public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getIdeHelper().runInBackground(null, "Starting Docker Host", false, true, "Starting Docker Host...", new Runnable() {
          @Override
          public void run() {
            removeAllChildNodes();
            setIconPath(DOCKERHOST_WAIT_ICON_PATH);
            try {
              Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
              VirtualMachine vm = azureClient.virtualMachines().getByGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
              if (vm != null) {
                vm.start();
                setIconPath(DOCKERHOST_RUN_ICON_PATH);
                refreshItems();
              }
            } catch (Exception e) {}
          }
        });
      }
    });
    addAction(ACTION_SHUTDOWN,new ShutdownDockerHostAction());
    addAction(ACTION_RESTART,new RestartDockerHostAction());
    super.loadActions();
  }

  public class RestartDockerHostAction extends AzureNodeActionPromptListener {
    public RestartDockerHostAction() {
      super(DockerHostNode.this,
          String.format("Are you sure you want to restart the virtual machine %s?", dockerHost.name),
          "Restarting Docker Host");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e)
        throws AzureCmdException {
      try {
        removeAllChildNodes();
        setIconPath(DOCKERHOST_WAIT_ICON_PATH);
        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
        VirtualMachine vm = azureClient.virtualMachines().getByGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
        if (vm != null) {
          vm.restart();
          setIconPath(DOCKERHOST_RUN_ICON_PATH);
        }
      } catch (Exception ee) {}
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }
  }

  public class ShutdownDockerHostAction extends AzureNodeActionPromptListener {
    public ShutdownDockerHostAction() {
      super(DockerHostNode.this, String.format(
          "This operation will result in losing the virtual IP address that was assigned to this virtual machine. " +
              "Are you sure that you want to shut down virtual machine %s?", dockerHost.name),
          "Shutting Down Docker Host");
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e)
        throws AzureCmdException {
      try {
        removeAllChildNodes();
        setIconPath(DOCKERHOST_STOP_ICON_PATH);

        Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
        VirtualMachine vm = azureClient.virtualMachines().getByGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
        if (vm != null) {
          vm.powerOff();
        }
      } catch (Exception ee) {}
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }
  }

}
