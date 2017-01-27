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
package com.microsoft.azure.docker;

import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.Pair;

import java.util.*;

public class AzureDockerHostsManager {
  public final int MAX_RESOURCE_LENGTH = 30;
  private static AzureDockerHostsManager instance = null;

  private AzureManager azureAuthManager;

  private Azure azureMainClient;
  private List<AzureDockerSubscription> subscriptionsList;
  private Map<String, AzureDockerSubscription> subscriptionsMap;
  private Map<String, Pair<Vault, KeyVaultClient>> vaultsMap;
  private List<DockerHost> dockerHostsList;
  private String userId;

  public static AzureDockerHostsManager getAzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {
    if (instance == null) {
      instance = new AzureDockerHostsManager(azureAuthManager);
    }

    return instance;
  }

  private AzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {

    this.azureAuthManager = azureAuthManager;

    // read docker hosts, key vaults and subscriptions here.
    forceRefreshSubscriptions();
  }

  public List<DockerHost> getDockerHostsList() {
    return dockerHostsList;
  }

  private void retrieveSubscriptions() {
    Map<String, AzureDockerSubscription> subsMap = new HashMap<>();
    List<AzureDockerSubscription> subsList = new ArrayList<AzureDockerSubscription>();
    Map<String, Pair<Vault, KeyVaultClient>> vaults = new HashMap<>();

    try {
      SubscriptionManager subscriptionManager = azureAuthManager.getSubscriptionManager();
      List<SubscriptionDetail> subscriptions = subscriptionManager.getSubscriptionDetails();
      for (SubscriptionDetail subscriptionDetail : subscriptions ) {
        if(subscriptionDetail.isSelected()) {
          AzureDockerSubscription dockerSubscription = new AzureDockerSubscription();
          dockerSubscription.id = subscriptionDetail.getSubscriptionId();
          dockerSubscription.name = subscriptionDetail.getSubscriptionName();
          dockerSubscription.azureClient = azureAuthManager.getAzure(dockerSubscription.id);
          dockerSubscription.keyVaultClient = azureAuthManager.getKeyVaultClient(subscriptionDetail.getTenantId());

          for (Vault vault : dockerSubscription.azureClient.vaults().list()) {
            vaults.put(vault.vaultUri(), new Pair<>(vault, dockerSubscription.keyVaultClient));
          }

          subsMap.put(dockerSubscription.id, dockerSubscription);
          subsList.add(dockerSubscription);
        }
      }

      List<Subscription> azureSubscriptionList = azureAuthManager.getSubscriptions();
      for (Subscription subscription : azureSubscriptionList) {
        AzureDockerSubscription dockerSubscription = subsMap.get(subscription.subscriptionId());

        if (dockerSubscription != null) {
          dockerSubscription.locations = new ArrayList<>();
          for (Location location : subscription.listLocations()) {
            dockerSubscription.locations.add(location.name());
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    vaultsMap = vaults;
    subscriptionsMap = subsMap;
    subscriptionsList = subsList;

    // Update Docker hosts list in case subscription info has changed
    forceRefreshDockerHosts();
  }

  public List<String> listKeyVaults() {
    List<String> result = new ArrayList<>();

    for (Map.Entry<String, Pair<Vault, KeyVaultClient>> entry : vaultsMap.entrySet()) {
      result.add(entry.getKey());
    }

    return result;
  }

  public List<String> getKeyVaultsAndSecrets() {
    List<String> result = new ArrayList<>();

    for (AzureDockerSubscription dockerSubscription : subscriptionsList) {
      Azure azureClient = dockerSubscription.azureClient;

      for (Vault vault : dockerSubscription.azureClient.vaults().list()) {
        result.add(vault.vaultUri());
      }
    }

    return result;
  }

  public void forceRefreshDockerHosts() {
    // call into Ops to retrieve the latest list of Docker VMs
    // TODO: make the real thing happen here

    dockerHostsList = createNewFakeDockerHostList();
  }

  public void forceRefreshSubscriptions() {
    retrieveSubscriptions();
  }

  public boolean checkStorageNameAvailability(String name) {
    return true;
  }

  public List<KnownDockerImages> getDefaultDockerImages() {
    List<KnownDockerImages> dockerImagesList = new ArrayList<KnownDockerImages>();
    dockerImagesList.add(KnownDockerImages.JBOSS_WILDFLY);
    dockerImagesList.add(KnownDockerImages.TOMCAT8);

    return dockerImagesList;
  }

  public DockerHost createNewDockerHostDescription(String name) {
    // TODO: limit the number of characters within the name
    DockerHost host = new DockerHost();
    host.name = name.toLowerCase();
    host.apiUrl = "http://" + name.toLowerCase() + ".centralus.cloudapp.azure.com";
    host.port = "2375";
    host.state = DockerHost.DockerHostVMState.RUNNING;
    host.hostOSType = DockerHost.DockerHostOSType.UBUNTU_SERVER_16;
    host.hostVM = new AzureDockerVM();
    host.hostVM.name = host.name;
    host.hostVM.vmSize = "Standard_DS1_v2";
    host.hostVM.region = "centralus";
    host.hostVM.resourceGroupName = name.toLowerCase() + "-rg";
    host.hostVM.vnetName = name.toLowerCase() + "-vnet";
    host.hostVM.vnetAddressSpace = "10.0.0.0/8";
    host.hostVM.subnetName = "subnet1";
    host.hostVM.subnetAddressRange = "10.0.0.0/16";
    host.hostVM.storageAccountName = getDefaultRandomName(host.name) + "sa";
    host.hostVM.storageAccountType = "Premium_LSR";

    host.hasPwdLogIn = true;
    host.hasSSHLogIn = true;
    host.isTLSSecured = true;
    host.hasKeyVault = true;

    host.certVault = new AzureDockerCertVault();
    host.certVault.name = name.toLowerCase() + "vault";
    host.certVault.url = host.certVault.name + ".centralus.azure.com";
    host.certVault.hostName = host.hostVM.name;
    host.certVault.resourceGroupName = host.hostVM.resourceGroupName;
    host.certVault.region = "centralus";
    host.certVault.userName = name;
    host.certVault.vmPwd = getDefaultRandomName(name);
    host.certVault.sshKey = null; // "id_rsa"
    host.certVault.sshPubKey = null; // "id_rsa.pub";
    host.certVault.tlsCACert = null; // "ca.pem";
    host.certVault.tlsCAKey = null; // "ca-key.pem";
    host.certVault.tlsClientCert = null; // "cert.pem";
    host.certVault.tlsClientKey = null; // "key.pem";
    host.certVault.tlsServerCert = null; // "server.pem";
    host.certVault.tlsServerKey = null; // "server-key.pem";

    return host;
  }

  public AzureDockerImageInstance getDefaultDockerImageDescription(String projectName) {
    AzureDockerImageInstance dockerImageDescription = new AzureDockerImageInstance();
    dockerImageDescription.dockerImageName = getDefaultDockerImageName(projectName);
    dockerImageDescription.dockerContainerName = getDefaultDockerContainerName(dockerImageDescription.dockerImageName);
    dockerImageDescription.artifactName = getDefaultArtifactName(projectName);
    dockerImageDescription.host = new DockerHost();
    dockerImageDescription.host.name = "myDockerHost";
    dockerImageDescription.hasNewDockerHost = false;

    return dockerImageDescription;
  }

  public String getDefaultDockerHostName() {
    return String.format("%s%d", "mydocker", new Random().nextInt(1000000));
  }

  public String getDefaultRandomName(String namePrefix) {
    if (namePrefix.length() > MAX_RESOURCE_LENGTH) {
      return String.format("%s%d", namePrefix.substring(0, MAX_RESOURCE_LENGTH), new Random().nextInt(1000000));
    } else {
      return String.format("%s%d", namePrefix, new Random().nextInt(1000000));
    }
  }

  public String getDefaultName(String projectName) {
    return projectName.replaceAll(" ", "");
  }

  public String getDefaultDockerImageName(String projectName) {
    return String.format("%s%d", getDefaultName(projectName), new Random().nextInt(10000));
  }

  public String getDefaultDockerContainerName(String imageName) {
    return String.format("%s-%d", imageName, new Random().nextInt(10000));
  }

  public String getDefaultArtifactName(String projectName) {
    return getDefaultName(projectName) + ".war";
  }

  public void updateDockerHost(DockerHost originalDockerHost, DockerHost updatedDockerHost) {
    try {
      Thread.sleep(20000);
    } catch (Exception e) {}
  }

  /* Retrieves a Docker host object for a given API
   *   The API URL is unique and can be safely used to get a specific docker host description
   */
  public DockerHost getDockerHostForURL(String apiURL) {
    // TODO: make the real call into Docker Ops to retrieve the host info
    return createNewFakeDockerHost("someHost");
  }

  public List<String> getDockerVMStates() {
    return Arrays.asList("Running", "Starting", "Stopped", "Deleting", "Failed");
  }

  public static List<String> getDockerVMStateToActionList(DockerHost.DockerHostVMState currentVMState) {
    if (currentVMState == DockerHost.DockerHostVMState.RUNNING) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.STOPPED) {
      return Arrays.asList(currentVMState.toString(), "Start", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.FAILED) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else {
      return Arrays.asList(currentVMState.toString());
    }
  }

  // ********************************* //

  public List<AzureDockerSubscription> createNewFakeSubscriptionList() {
    List<AzureDockerSubscription> subscriptionList = new ArrayList<AzureDockerSubscription>();


    return subscriptionList;
  }

  public DockerHost createNewFakeDockerHost(String name) {
    DockerHost host = new DockerHost();
    host.name = name;
    host.apiUrl = "http://" + name + ".centralus.cloudapp.azure.com";
    host.port = "2375";
    host.isTLSSecured = false;
    host.state = DockerHost.DockerHostVMState.RUNNING;
    host.hostOSType = DockerHost.DockerHostOSType.UBUNTU_SERVER_16;
    host.hostVM = new AzureDockerVM();
    host.hostVM.name = name;
    host.hostVM.vmSize = "Standard_DS1_v2";
    host.hostVM.region = "centralus";
    host.hostVM.resourceGroupName = "myresourcegroup";
    host.hostVM.vnetName = "network1";
    host.hostVM.vnetAddressSpace = "10.0.0.0/8";
    host.hostVM.subnetName = "subnet1";
    host.hostVM.subnetAddressRange = "10.0.0.0/16";
    host.hostVM.storageAccountName = "sa12313111244";
    host.hostVM.storageAccountType = "Premium_LSR";

    host.hasPwdLogIn = true;
    host.hasSSHLogIn = true;
    host.isTLSSecured = true;
    host.hasKeyVault = true;

    host.certVault = new AzureDockerCertVault();
    host.certVault.name = "mykeyvault1";
    host.certVault.url = host.certVault.name + ".someregion.azure.com";
    host.certVault.hostName = name;
    host.certVault.resourceGroupName = "mykeyvault1rg";
    host.certVault.region = "centralus";
    host.certVault.userName = "ubuntu";
    host.certVault.vmPwd = "PasswordGoesHere";
    host.certVault.sshKey = "id_rsa";
    host.certVault.sshPubKey = "id_rsa.pub";
    host.certVault.tlsCACert = "ca.pem";
    host.certVault.tlsCAKey = "ca-key.pem";
    host.certVault.tlsClientCert = "cert.pem";
    host.certVault.tlsClientKey = "key.pem";
    host.certVault.tlsServerCert = "server.pem";
    host.certVault.tlsServerKey = "server-key.pem";

    return host;
  }

  public List<DockerHost> createNewFakeDockerHostList() {
    List<DockerHost> hosts = new ArrayList<DockerHost>();
    hosts.add(createNewFakeDockerHost("someDockerHost112"));
    hosts.add(createNewFakeDockerHost("otherDockerHost212"));
    hosts.add(createNewFakeDockerHost("qnyDockerHost132"));
    hosts.add(createNewFakeDockerHost("anyDockerHost612"));
    dockerHostsList = hosts;

    return hosts;
  }

}
