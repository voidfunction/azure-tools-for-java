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
import com.microsoft.azure.docker.model.DockerContainer;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.DockerImage;
import com.microsoft.azure.docker.ops.AzureDockerContainerOps;
import com.microsoft.azure.docker.ops.AzureDockerImageOps;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

import java.util.Map;

public class DockerImageNode extends AzureRefreshableNode {
  //TODO: Replace the icons with the real Docker host icons
  private static final String DOCKER_IMAGE_ICON_PATH = "endpoint.png";

  public static final String ACTION_DELETE = "Delete";
  public static final String ACTION_PUSH2REGISTRY = "Push";
  public static final String ACTION_NEW_CONTAINER = "Publish";

  DockerImage dockerImage;
  DockerHost dockerHost;
  AzureDockerHostsManager dockerManager;
  private boolean isLoaded;

  public DockerImageNode(Node parent, AzureDockerHostsManager dockerManager, DockerHost dockerHost, DockerImage dockerImage)
      throws AzureCmdException {
    super(dockerHost.apiUrl, dockerImage.name, parent, DOCKER_IMAGE_ICON_PATH, true);

    this.dockerManager = dockerManager;
    this.dockerHost = dockerHost;
    this.dockerImage = dockerImage;
    isLoaded = true; // Docker hosts are loaded by the parent node

    loadActions();
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
      Map<String, DockerImage> dockerImages = AzureDockerImageOps.getImages(dockerHost);
      Map<String, DockerContainer> dockerContainers = AzureDockerContainerOps.getContainers(dockerHost);
      AzureDockerContainerOps.setContainersAndImages(dockerContainers, dockerImages);
      dockerHost.dockerImages = dockerImages;
      if (dockerImages != null) {
        dockerImage = dockerImages.get(dockerImage.name);
        if (dockerImage != null) {
          for (DockerContainer dockerContainer : dockerImage.containers.values()) {
            addChildNode(new DockerContainerNode(this, dockerManager, dockerHost, dockerContainer));
          }
          isLoaded = true;
        }
      }
    } catch (Exception e) {}
  }

  public DockerHost getDockerHost() {
    return dockerHost;
  }

  public AzureDockerHostsManager getDockerManager() {
    return dockerManager;
  }

}
