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
package com.microsoft.azure.docker.ops;

import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;

import java.util.List;

public class AzureDockerContainerOps {
  public static AzureDockerContainerInstance get(String name, DockerHost dockerHost) {
    AzureDockerContainerInstance container = new AzureDockerContainerInstance();
    container.dockerImage = new DockerImage();
    container.dockerImage.dockerHost = dockerHost;

    return get(container);
  }

  public static AzureDockerContainerInstance get(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerImage == null || dockerContainer.dockerImage.dockerHost == null) {
      throw new AzureDockerException("Unexpected argument values; dockerContainer, dockerImage, dockerHost can not be null");
    }
    try {
      DockerHost dockerHost = dockerContainer.dockerImage.dockerHost;
      if (dockerHost.session == null || !dockerHost.session.isConnected()) {
        dockerHost.session = AzureDockerSSHOps.createLoginInstance(dockerHost);
      }

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(String name, DockerHost dockerHost) {
    AzureDockerContainerInstance container = new AzureDockerContainerInstance();
    container.dockerImage = new DockerImage();
    container.dockerImage.dockerHost = dockerHost;

    return getDetails(container);
  }

  public static String getDetails(AzureDockerContainerInstance dockerContainerInstance) {
    if (dockerContainerInstance == null || dockerContainerInstance.dockerImage == null || dockerContainerInstance.dockerImage.dockerHost == null) {
      throw new AzureDockerException("Unexpected argument values; dockerContainer, name/id, dockerImage, dockerHost can not be null");
    }
    try {
      DockerHost dockerHost = dockerContainerInstance.dockerImage.dockerHost;
      if (dockerHost.session == null || !dockerHost.session.isConnected()) {
        dockerHost.session = AzureDockerSSHOps.createLoginInstance(dockerHost);
      }

      String dockerContainerName = AzureDockerUtils.isValid(dockerContainerInstance.dockerContainer.id) ? dockerContainerInstance.dockerContainer.id : dockerContainerInstance.dockerContainer.name;

      return AzureDockerSSHOps.executeCommand("docker inspect " + dockerContainerName, dockerHost.session, false);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void start(AzureDockerContainerInstance dockerContainer) {
    try {
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void stop(AzureDockerContainerInstance dockerContainer) {
    try {
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void delete(AzureDockerContainerInstance dockerContainer) {
    try {
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerContainerInstance create(AzureDockerContainerInstance dockerContainer) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static List<AzureDockerContainerInstance> list(AzureDockerContainerInstance dockerContainer) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static List<AzureDockerContainerInstance> list(DockerImage dockerImage) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }
}
