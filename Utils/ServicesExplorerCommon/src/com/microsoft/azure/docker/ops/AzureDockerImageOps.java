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
import com.microsoft.azure.docker.model.AzureDockerException;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.DockerImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.DEFAULT_DOCKER_IMAGES_DIRECTORY;

public class AzureDockerImageOps {

  public static void delete(DockerImage dockerImage) {
    try {
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void delete(DockerImage dockerImage, Session session) {
    if (dockerImage == null || dockerImage.dockerHost == null || (session == null && dockerImage.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerImage.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      if (DEBUG) System.out.println("Start executing docker rmi " + dockerImage.name);
      String cmdOut1 = AzureDockerSSHOps.executeCommand("docker rmi " + dockerImage.name, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing docker rmi " + dockerImage.name);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerImage create(DockerImage dockerImage) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerImage create(DockerImage dockerImage, Session session) {
    if (dockerImage == null || dockerImage.dockerHost == null || (session == null && dockerImage.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerImage.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String dockerImageDir = DEFAULT_DOCKER_IMAGES_DIRECTORY + "/" + dockerImage.name;
      String cmd1 = String.format("docker build -t %s -f %s/Dockerfile %s/ \n", dockerImage.name, dockerImageDir, dockerImageDir);
      if (DEBUG) System.out.format("Start executing: %s", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd1);

      // docker images ubuntu --format "{ \"id\": \"{{.ID}}\", \"name\": \"{{.Repository}} }\""
      // Getting the image ID
      String cmd2 = String.format("docker images %s --format {{.ID}} \n", dockerImage.name);
      if (DEBUG) System.out.format("Start executing: %s", cmd2);
      String cmdOut2 = AzureDockerSSHOps.executeCommand(cmd2, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s", cmd2);
      dockerImage.id = cmdOut2.trim();

      // add the image to the Docker dockerHost list of Docker images
      if (dockerImage.dockerHost.dockerImages == null) {
        dockerImage.dockerHost.dockerImages = new HashMap<>();
      }
      dockerImage.dockerHost.dockerImages.put(dockerImage.name, dockerImage);
      dockerImage.remotePath = dockerImageDir;

      return dockerImage;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerImageInstance create(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    DockerImage dockerImage = AzureDockerImageOps.create(new DockerImage(dockerImageInstance), session);
    dockerImageInstance.id = dockerImage.id;
    dockerImageInstance.remotePath = dockerImage.remotePath;

    return dockerImageInstance;
  }

  public static DockerImage get(DockerImage dockerImage) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerImage get(DockerImage dockerImage, Session session) {
    if (dockerImage == null || dockerImage.dockerHost == null || (session == null && dockerImage.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerImage.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(DockerImage dockerImage) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(DockerImage dockerImage, Session session) {
    if (dockerImage == null || dockerImage.dockerHost == null || (session == null && dockerImage.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerImage.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static Map<String, DockerImage> getImages(DockerHost dockerHost) {
    if (dockerHost == null || (dockerHost.session == null && dockerHost.certVault == null)) {
      throw new AzureDockerException("Unexpected param values: dockerHost and login session cannot be null");
    }

    if (dockerHost.session == null) dockerHost.session = AzureDockerSSHOps.createLoginInstance(dockerHost);

    try {
      if (!dockerHost.session.isConnected()) dockerHost.session.connect();

      Map<String, DockerImage> dockerImageMap = new HashMap<>();

      AzureDockerVMOps.waitForDockerDaemonStartup(dockerHost.session);

//      String cmd1 = String.format("docker inspect %s \n", dockerContainer.dockerContainerName);
//      if (DEBUG) System.out.format("Start executing: %s", cmd1);
//      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, false);
//      if (DEBUG) System.out.println(cmdOut1);
//      if (DEBUG) System.out.format("Done executing: %s", cmd1);

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

}
