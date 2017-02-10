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

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.DEFAULT_DOCKER_IMAGES_DIRECTORY;

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

  public static String getDetails(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker inspect %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

      return cmdOut1;

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);

    return getDetails(dockerContainerInstance, session);
  }

  public static String getDetails(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    return getDetails(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void start(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    AzureDockerVMOps.waitForDockerDaemonStartup(session);

    try {
      if (!session.isConnected()) session.connect();

      String cmd1 = String.format("docker start %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void start(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    start(dockerContainerInstance, session);
  }

  public static void start(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    start(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void stop(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker stop %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void stop(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    stop(dockerContainerInstance, session);
  }

  public static void stop(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    stop(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void delete(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      stop(dockerContainer, session);

      String cmd1 = String.format("docker rm %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void delete(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    delete(dockerContainerInstance, session);
  }

  public static void delete(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    delete(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static AzureDockerContainerInstance create(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker create -p \"%s\" --name %s %s \n", dockerContainer.dockerPortSettings, dockerContainer.dockerContainerName, dockerContainer.dockerImageName);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

      return  dockerContainer;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerContainerInstance create(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    dockerContainerInstance = create(dockerContainerInstance, session);

    return dockerContainerInstance;
  }

  public static AzureDockerContainerInstance create(AzureDockerContainerInstance dockerContainerInstance) {
    if (dockerContainerInstance == null || dockerContainerInstance.dockerHost == null || dockerContainerInstance.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    return create(dockerContainerInstance, dockerContainerInstance.dockerHost.session);
  }

}
