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
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.NicIpConfiguration;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceNamer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.isValid;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.*;

public class AzureDockerVMOps {

  public static VirtualMachine createDefaultDockerHostVM(Azure azureClient, AzureDockerCertVault certVault) throws AzureDockerException {

    try {
      VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKey defLinuxCreateStage = azureClient.virtualMachines().define(certVault.hostName)
          .withRegion(certVault.region)
          .withNewResourceGroup(certVault.resourceGroupName)
          .withNewPrimaryNetwork("10.0.0.0/16")
          .withPrimaryPrivateIpAddressDynamic()
          .withNewPrimaryPublicIpAddress(certVault.hostName)
          .withSpecificLinuxImageVersion(KnownDockerVirtualMachineImage.UBUNTU_SERVER_14_04_LTS.imageReference())
          .withRootUsername(certVault.vmUsername);

      VirtualMachine.DefinitionStages.WithCreate defCreateStage =
          ((certVault.vmPwd != null && !certVault.vmPwd.isEmpty())
              ? defLinuxCreateStage.withRootPassword(certVault.vmPwd)
              : defLinuxCreateStage.withSsh(certVault.sshPubKey))
              .withSize(VirtualMachineSizeTypes.STANDARD_DS2_V2);
      //
      defCreateStage =
          ((certVault.tlsServerCert != null && !certVault.tlsServerCert.isEmpty())
              ? defCreateStage.withTag("port", DOCKER_API_PORT_TLS_ENABLED) /* Default Docker host port when TLS is enabled */
              : defCreateStage.withTag("port", DOCKER_API_PORT_TLS_DISABLED)) /* Default Docker host port when TLS is disabled */
              .withTag("hostType", "Docker");

      return defCreateStage.create();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static VirtualMachine createDockerHostVM(Azure azureClient, AzureDockerCertVault certVault, ImageReference imgRef) throws AzureDockerException {
    try {
      VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKey defLinuxCreateStage = azureClient.virtualMachines().define(certVault.hostName)
          .withRegion(certVault.region)
          .withNewResourceGroup(certVault.resourceGroupName)
          .withNewPrimaryNetwork("10.0.0.0/16")
          .withPrimaryPrivateIpAddressDynamic()
          .withNewPrimaryPublicIpAddress(certVault.hostName)
          .withSpecificLinuxImageVersion(imgRef)
          .withRootUsername(certVault.vmUsername);

      VirtualMachine.DefinitionStages.WithCreate defCreateStage =
          ((certVault.vmPwd != null && !certVault.vmPwd.isEmpty())
              ? defLinuxCreateStage.withRootPassword(certVault.vmPwd)
              : defLinuxCreateStage.withSsh(certVault.sshPubKey))
              .withSize(VirtualMachineSizeTypes.STANDARD_DS2_V2);
      defCreateStage =
          ((certVault.tlsServerCert != null && !certVault.tlsServerCert.isEmpty())
              ? defCreateStage.withTag("port", "2376") /* Default Docker host port when TLS is enabled */
              : defCreateStage.withTag("port", "2375")) /* Default Docker host port when TLS is disabled */
              .withTag("hostType", "Docker");

      return defCreateStage.create();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerVM getDockerVM(Azure azureClient, String resourceGroup, String hostName) {
    try {
      VirtualMachine vm = azureClient.virtualMachines().getByGroup(resourceGroup, hostName);
      AzureDockerVM dockerVM = new AzureDockerVM();
      PublicIpAddress publicIp = vm.getPrimaryPublicIpAddress();
      NicIpConfiguration nicIpConfiguration = publicIp.getAssignedNetworkInterfaceIpConfiguration();

      dockerVM.name = vm.name();
      dockerVM.resourceGroupName = vm.resourceGroupName();
      dockerVM.region = vm.regionName();
      dockerVM.availabilitySet = (vm.availabilitySetId() != null) ? ResourceUtils.nameFromResourceId(vm.availabilitySetId()) : null;
      dockerVM.publicIpName = publicIp.name();
      dockerVM.publicIp = publicIp.ipAddress();
      dockerVM.dnsName = publicIp.fqdn();
      dockerVM.vnetName = nicIpConfiguration.getNetwork().name();
      dockerVM.subnetName = nicIpConfiguration.subnetName();
      dockerVM.networkSecurityGroupName = (nicIpConfiguration.parent().networkSecurityGroupId() != null) ? ResourceUtils.nameFromResourceId(nicIpConfiguration.parent().networkSecurityGroupId()) : null;
      dockerVM.storageAccountName = vm.storageProfile().osDisk().vhd().uri().split("[.]")[0].split("/")[2];
      dockerVM.osDiskName = vm.storageProfile().osDisk().name();
      if (vm.storageProfile().imageReference() != null) {
        dockerVM.osHost = new AzureOSHost();
        dockerVM.osHost.publisher = vm.storageProfile().imageReference().publisher();
        dockerVM.osHost.offer = vm.storageProfile().imageReference().offer();
        dockerVM.osHost.sku = vm.storageProfile().imageReference().sku();
        dockerVM.osHost.version = vm.storageProfile().imageReference().version();
      }
      dockerVM.tags = vm.tags();

      return dockerVM;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static VirtualMachine getVM(Azure azureClient, String resourceGroup, String hostName) throws  AzureDockerException {
    try {
      return azureClient.virtualMachines().getByGroup(resourceGroup, hostName);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost getDockerHost(AzureDockerCertVault certVault, AzureDockerVM hostVM) {
    if (certVault == null || hostVM == null) {
      throw new AzureDockerException("Unexpected param values; certVault and hostVM cannot be null");
    }

    DockerHost dockerHost = new DockerHost();
    dockerHost.name = hostVM.name;
    dockerHost.certVault = certVault;
    dockerHost.apiUrl = hostVM.dnsName;
    dockerHost.isTLSSecured = certVault.tlsServerCert != null && !certVault.tlsServerCert.isEmpty();
    dockerHost.port = (hostVM.tags != null) ? hostVM.tags.get("port") : (dockerHost.isTLSSecured) ? DOCKER_API_PORT_TLS_ENABLED : DOCKER_API_PORT_TLS_DISABLED;
    dockerHost.hostVM = hostVM;
    dockerHost.dockerImages = new ArrayList<DockerImage>();

    if (hostVM.osHost != null) {
      switch (hostVM.osHost.offer) {
        case "Ubuntu_Snappy_Core":
          dockerHost.hostOSType = DockerHost.DockerHostOSType.UBUNTU_SNAPPY_CORE;
          break;
        case "CoreOS":
          dockerHost.hostOSType = DockerHost.DockerHostOSType.COREOS;
          break;
        case "CentOS":
          dockerHost.hostOSType = DockerHost.DockerHostOSType.OPENLOGIC_CENTOS;
          break;
        case "UbuntuServer":
          dockerHost.hostOSType = hostVM.osHost.offer.equals("14.04.4-LTS") ? DockerHost.DockerHostOSType.UBUNTU_SERVER_14 : DockerHost.DockerHostOSType.UBUNTU_SERVER_16;
          break;
        default:
          dockerHost.hostOSType = DockerHost.DockerHostOSType.LINUX_OTHER;
          break;
      }
    }

    return dockerHost;
  }

  public static void installDocker(DockerHost dockerHost) {
    if (dockerHost == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }

    try {
      switch (dockerHost.hostOSType) {
        case UBUNTU_SERVER_14:
        case UBUNTU_SERVER_16:
          installDockerOnUbuntuServer(dockerHost);
          break;
        default:
          throw new AzureDockerException("Docker host OS type is not supported");
      }
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost installDockerOnUbuntuServer(DockerHost dockerHost) {
    if (dockerHost == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }

    try {
      Session session = AzureDockerSSHOps.createLoginInstance(dockerHost);

      System.out.println("Start executing docker install command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand(INSTALL_DOCKER_FOR_UBUNTU, session, false);
      System.out.println(cmdOut1);
      System.out.println("Done executing docker install command");

      if (dockerHost.isTLSSecured) {
        if (isValid(dockerHost.certVault.tlsServerCert)) {
          // Docker certificates are passed in; copy them to the docker host
          setDockerCertsForUbuntuServer(dockerHost, session);
        } else {
          // Create new TLS certificates and upload them into the current machine representation
          dockerHost = createDockerCertsForUbuntuServer(dockerHost, session);
        }
      }

      // Create the Docker daemon configuration file
      System.out.println("Start executing docker config command");
      String dockerApiPort = (dockerHost.port != null && !dockerHost.port.isEmpty()) ? dockerHost.port : dockerHost.isTLSSecured ? DOCKER_API_PORT_TLS_ENABLED : DOCKER_API_PORT_TLS_DISABLED;
      String createDockerOpts = dockerHost.isTLSSecured ? CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED : CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED;
      createDockerOpts = createDockerOpts.replaceAll(DOCKER_API_PORT_PARAM, dockerApiPort);
      String cmdOut3 = AzureDockerSSHOps.executeCommand(createDockerOpts, session, false);
      System.out.println(cmdOut3);
      System.out.println("Done executing docker config command");

//      System.out.println("Start executing download to string command");
//      String cmdOut4 = AzureDockerSSHOps.download("some_file.txt", "./", session);
//      System.out.println(cmdOut4);
//      System.out.println("Done executing download to string command");
//
//      System.out.println("Start executing upload from string command");
//      String toWrite = "this is a string\nuploaded to a new file\nend of my file\n";
//      System.out.println(toWrite);
//      ByteArrayInputStream buff = new ByteArrayInputStream(new String(toWrite).getBytes());
//      AzureDockerSSHOps.upload(buff, "some_file2.txt", "./", session);
//      System.out.println("Done executing upload from string command");

//      System.out.println("Start executing ssh command");
//      String cmdOut5 = AzureDockerSSHOps.executeCommand("ls /", session, false);
//      System.out.println(cmdOut5);
//      System.out.println("Done executing ssh command");
//
//      System.out.println("Start executing ssh download command");
//      AzureDockerSSHOps.download("some_file.txt", "temp", System.getProperty("user.home") + File.separator + "temp", session);
//      System.out.println("Done executing ssh download command");
//
//      System.out.println("Start executing ssh command");
//      String cmdOut6 = AzureDockerSSHOps.executeCommand("rm temp/some_file.txt", session, false);
//      System.out.println(cmdOut6);
//      System.out.println("Done executing ssh command");
//
//      System.out.println("Start executing ssh upload command");
//      AzureDockerSSHOps.upload("some_file.txt", System.getProperty("user.home") + File.separator + "temp", "temp", session);
//      System.out.println("Done executing ssh upload command");
//
//      System.out.println("Start executing docker install TLS command");
//      String cmdOut7 = AzureDockerSSHOps.executeCommand(INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU, session, false);
//      System.out.println(cmdOut7);
//      System.out.println("Done executing docker install TLS command");
//
//      System.out.println("Start executing docker TLS config command");
//      String createDockerTLSOpts = CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED;
//      createDockerTLSOpts = createDockerTLSOpts.replaceAll(DOCKER_API_PORT_PARAM, "2375");
//      String cmdOut8 = AzureDockerSSHOps.executeCommand(createDockerTLSOpts, session, false);
//      System.out.println(cmdOut8);
//      System.out.println("Done executing docker TLS config command");

      session.disconnect();

      return dockerHost;

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost createDockerCertsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost, host name, host dns and login session cannot be null");
    }

    try {
      // Generate a random password to be used when creating the TLS certificates
      String certCAPwd = ResourceNamer.randomResourceName("", 15);
      String createTLScerts = CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU;
      createTLScerts = createTLScerts.replaceAll(CERT_CA_PWD_PARAM, certCAPwd);
      createTLScerts = createTLScerts.replaceAll(HOSTNAME, dockerHost.hostVM.name);
      createTLScerts = createTLScerts.replaceAll(FQDN_PARAM, dockerHost.hostVM.dnsName);
      createTLScerts = createTLScerts.replaceAll(DOMAIN_PARAM, dockerHost.hostVM.dnsName.substring(dockerHost.hostVM.dnsName.indexOf('.')));

      System.out.println("Executing:\n" + createTLScerts);
      System.out.println("Start executing docker create TLS certs command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand(createTLScerts, session, false);
      System.out.println(cmdOut1);
      System.out.println("Done executing docker create TLS certs command");

      System.out.println("Start executing docker install TLS certs command");
      String cmdOut2 = AzureDockerSSHOps.executeCommand(INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU, session, false);
      System.out.println(cmdOut2);
      System.out.println("Done executing docker install TLS certs command");

      System.out.println("Start downloading the TLS certs");
      dockerHost.certVault.tlsCACert     = AzureDockerSSHOps.download("ca.pem", ".azuredocker/tls", session);
      dockerHost.certVault.tlsCAKey      = AzureDockerSSHOps.download("ca-key.pem", ".azuredocker/tls", session);
      dockerHost.certVault.tlsClientCert = AzureDockerSSHOps.download("cert.pem", ".azuredocker/tls", session);
      dockerHost.certVault.tlsClientKey  = AzureDockerSSHOps.download("key.pem", ".azuredocker/tls", session);
      dockerHost.certVault.tlsServerCert = AzureDockerSSHOps.download("server.pem", ".azuredocker/tls", session);
      dockerHost.certVault.tlsServerKey  = AzureDockerSSHOps.download("server-key.pem", ".azuredocker/tls", session);
      System.out.println("Done downloading TLS certs");

      return dockerHost;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void setDockerCertsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost, host name, host dns and login session cannot be null");
    }

    try {
      System.out.println("Start uploading the TLS certs");
      if (isValid(dockerHost.certVault.tlsCACert))     AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsCACert.getBytes())),     "ca.pem", ".azuredocker/tls", session);
      if (isValid(dockerHost.certVault.tlsCAKey))      AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsCAKey.getBytes())),      "ca-key.pem", ".azuredocker/tls", session);
      if (isValid(dockerHost.certVault.tlsClientCert)) AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsClientCert.getBytes())), "cert.pem", ".azuredocker/tls", session);
      if (isValid(dockerHost.certVault.tlsClientKey))  AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsClientKey.getBytes())),  "key.pem", ".azuredocker/tls", session);
      if (isValid(dockerHost.certVault.tlsServerCert)) AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsServerCert.getBytes())), "server.pem", ".azuredocker/tls", session);
      if (isValid(dockerHost.certVault.tlsServerKey))  AzureDockerSSHOps.upload((new ByteArrayInputStream(dockerHost.certVault.tlsServerKey.getBytes())),  "server-key.pem", ".azuredocker/tls", session);
      System.out.println("Done uploading TLS certs");

      System.out.println("Start executing docker install TLS certs command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand(INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU, session, false);
      System.out.println(cmdOut1);
      System.out.println("Done executing docker install TLS certs command");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }
}
