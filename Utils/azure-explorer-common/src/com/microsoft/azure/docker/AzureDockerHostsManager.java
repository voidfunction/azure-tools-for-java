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
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.*;

public class AzureDockerHostsManager {
  private static boolean DEBUG = true;

  private static AzureDockerHostsManager instance = null;

  private AzureManager azureAuthManager;

  private List<AzureDockerSubscription> subscriptionsList;
  private Map<String, AzureDockerSubscription> subscriptionsMap;
  private Map<String, Pair<Vault, KeyVaultClient>> vaultsMap;
  private Map<String, AzureDockerCertVault> dockerVaultsMap;
  private Map<String, DockerHost> dockerHostsMap;
  private List<DockerHost> dockerHostsList;
  private Map<String, List<AzureDockerVnet>> dockerNetworkMap;
  private Map<String, List<AzureDockerStorageAccount>> dockerStorageAccountMap;
  private String userId;
  private AzureDockerPreferredSettings dockerPreferredSettings;

  public AzureDockerPreferredSettings getDockerPreferredSettings() { return dockerPreferredSettings; }

  public void setDockerPreferredSettings(AzureDockerPreferredSettings dockerPreferredSettings) {
    this.dockerPreferredSettings = dockerPreferredSettings;
  }

  public static AzureDockerHostsManager getAzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {
    if (instance == null || instance.azureAuthManager != azureAuthManager) {
      instance = new AzureDockerHostsManager(azureAuthManager);
    }

    // read docker hosts, key vaults and subscriptions here.
    if (!instance.isInitialized()) instance.forceRefreshSubscriptions();

    return instance;
  }

  public static AzureDockerHostsManager getAzureDockerHostsManagerEmpty(AzureManager azureAuthManager) throws Exception {
    if (instance == null) {
      instance = new AzureDockerHostsManager(azureAuthManager);
    } else {
      if (azureAuthManager != null && instance.azureAuthManager != azureAuthManager) {
        instance.azureAuthManager = azureAuthManager;
        instance.userId = azureAuthManager.getCurrentUserId();
        instance.forceRefreshSubscriptions();
      }
    }

    return instance;
  }

  private AzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {
    this.azureAuthManager = azureAuthManager;
    this.userId = azureAuthManager.getCurrentUserId();
  }

  public AzureDockerHostsManager forceRefresh(AzureManager azureAuthManager) {
    try {
      instance = new AzureDockerHostsManager(azureAuthManager);
      instance.forceRefreshSubscriptions();
    } catch (Exception e) {
      e.printStackTrace();
      instance = null;
    }

    return instance;
  }

  public List<AzureDockerSubscription> getSubscriptionsList() { return subscriptionsList; }

  public Map<String, AzureDockerSubscription> getSubscriptionsMap() { return subscriptionsMap; }

  public Map<String, Pair<Vault, KeyVaultClient>> getVaultsMap() { return vaultsMap; }

  public Map<String, AzureDockerCertVault> getDockerVaultsMap() { return dockerVaultsMap;}

  public String getUserId() { return userId; }

  public List<DockerHost> getDockerHostsList() { return dockerHostsList; }

  public boolean isInitialized() {
    return subscriptionsList != null && !subscriptionsList.isEmpty() &&
        vaultsMap != null && !vaultsMap.isEmpty() &&
        dockerVaultsMap != null && !dockerVaultsMap.isEmpty() &&
        dockerHostsList != null && !dockerHostsList.isEmpty() &&
        dockerNetworkMap != null && !dockerNetworkMap.isEmpty() &&
        dockerStorageAccountMap != null && !dockerStorageAccountMap.isEmpty();
  }

  public Map<String, AzureDockerSubscription> refreshDockerSubscriptions() {
    try {
      subscriptionsMap = AzureDockerUtils.refreshDockerSubscriptions(azureAuthManager);
      subscriptionsList = new ArrayList<>(subscriptionsMap.values());
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading subscription details");
    }

    return subscriptionsMap;
  }

  public Map<String, Pair<Vault, KeyVaultClient>> refreshDockerVaults() {
    try {
      vaultsMap = AzureDockerUtils.refreshDockerVaults(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vaults");
    }

    return vaultsMap;
  }

  public Map<String, AzureDockerCertVault> refreshDockerVaultDetails() {
    try {
      dockerVaultsMap = AzureDockerUtils.refreshDockerVaultDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vault details");
    }

    return dockerVaultsMap;
  }

  public Map<String, List<AzureDockerVnet>> refreshDockerVnetDetails() {
    try {
      dockerNetworkMap = AzureDockerUtils.refreshDockerVnetDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual network details");
    }

    return dockerNetworkMap;
  }

  public Map<String, List<AzureDockerStorageAccount>> refreshDockerStorageAccountDetails() {
    try {
      dockerStorageAccountMap = AzureDockerUtils.refreshDockerStorageAccountDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading storage account details");
    }

    return dockerStorageAccountMap;
  }

  public Map<String, DockerHost> refreshDockerHostDetails() {
    try {
      Map<String, DockerHost> localDockerHostsMap = new HashMap<>();
      for (List<DockerHost> dockerHostList : AzureDockerUtils.refreshDockerHostDetails(subscriptionsList, dockerVaultsMap).values()) {
        for (DockerHost dockerHost : dockerHostList) {
          localDockerHostsMap.put(dockerHost.apiUrl, dockerHost);
          if (dockerHost.certVault == null && dockerHostsMap != null) {
            DockerHost oldHost = dockerHostsMap.get(dockerHost.apiUrl);
            dockerHost.certVault = oldHost != null ? oldHost.certVault : null;
          }
        }
      }

      dockerHostsMap = localDockerHostsMap;
      dockerHostsList = new ArrayList<>(dockerHostsMap.values());
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual machine details");
    }

    return dockerHostsMap;
  }

  public void forceRefreshDockerHosts() {
    // call into Ops to retrieve the latest list of Docker VMs
    refreshDockerHostDetails();

//    dockerHostsList = createNewFakeDockerHostList();
  }

  public void forceRefreshSubscriptions() {
    refreshDockerSubscriptions();

    refreshDockerVaults();

    refreshDockerVaultDetails();

    refreshDockerVnetDetails();

    refreshDockerStorageAccountDetails();

    refreshDockerHostDetails();

  }

  public boolean checkStorageNameAvailability(AzureDockerSubscription subscription, String name) {
    if (subscription != null && subscription.azureClient != null && name != null) {
      return AzureDockerUtils.checkStorageNameAvailability(subscription.azureClient, name);
    } else {
      return false;
    }
  }

  public List<KnownDockerImages> getDefaultDockerImages() {
    List<KnownDockerImages> dockerImagesList = new ArrayList<KnownDockerImages>();
    dockerImagesList.add(KnownDockerImages.JBOSS_WILDFLY);
    dockerImagesList.add(KnownDockerImages.TOMCAT8);

    return dockerImagesList;
  }

  public List<AzureDockerCertVault> getDockerKeyVaults() {
    return new ArrayList<>(dockerVaultsMap.values());
  }

  public AzureDockerCertVault getDockerVault(String name) {
    return dockerVaultsMap.get(name);
  }

  public List<String> getResourceGroups(AzureDockerSubscription subscription) {
    if (subscription != null && subscription.azureClient != null) {
      // TODO: use memory cache to retrieve the resource groups
      return AzureDockerUtils.getResourceGroups(subscription.azureClient);
    } else {
      return new ArrayList<>();
    }
  }

  public List<AzureDockerVnet> getNetworksAndSubnets(AzureDockerSubscription subscription) {
    if (subscription != null && subscription.id != null) {
      return dockerNetworkMap.get(subscription.id);
    } else {
      return new ArrayList<>();
    }
  }

  public Map<String, Pair<String, List<String>>> getNetworksAndSubnets(AzureDockerSubscription subscription, String region) {
    if (subscription != null && subscription.azureClient != null) {
      // TODO: use memory cache to retrieve the storage accouns
      return AzureDockerUtils.getVirtualNetworks(subscription.azureClient, region);
    } else {
      return new HashMap<>();
    }
  }

  public List<String> getAvailableStorageAccounts(String sid, String vmImageSizeType) {
    if (sid != null && !sid.isEmpty()) {
      return AzureDockerUtils.getAvailableStorageAccounts(dockerStorageAccountMap.get(sid), vmImageSizeType);
    } else {
      return new ArrayList<String>();
    }
  }

  public DockerHost createNewDockerHostDescription(String name) {
    // TODO: limit the number of characters within the name
    DockerHost host = new DockerHost();
    host.name = name.toLowerCase();
    host.apiUrl = "http://" + name.toLowerCase() + ".centralus.cloudapp.azure.com";
    host.port = "2376"; /* Default Docker dockerHost port when TLS is enabled, "2375" otherwise */
    host.state = DockerHost.DockerHostVMState.STARTING;
    host.hostOSType = (dockerPreferredSettings != null && dockerPreferredSettings.vmOS != null) ? DockerHost.DockerHostOSType.valueOf(dockerPreferredSettings.vmOS) : DockerHost.DockerHostOSType.UBUNTU_SERVER_16_04_LTS;
    host.hostVM = new AzureDockerVM();
    host.hostVM.name = host.name;
    host.hostVM.vmSize = (dockerPreferredSettings != null && dockerPreferredSettings.vmSize != null) ? dockerPreferredSettings.vmSize : KnownDockerVirtualMachineSizes.Standard_DS2_v2.name();
    host.hostVM.region = (dockerPreferredSettings != null && dockerPreferredSettings.region != null)? dockerPreferredSettings.region : null;
    host.hostVM.resourceGroupName = name.toLowerCase() + "-rg";
    host.hostVM.vnetName = AzureDockerUtils.getDefaultRandomName(host.name, 20) + "-vnet";
    host.hostVM.vnetAddressSpace = "10.0.0.0/16";
    host.hostVM.subnetName = "subnet1";
    host.hostVM.publicIpName = name.toLowerCase() + "-pip";
    host.hostVM.subnetAddressRange = "10.0.0.0/16";
    host.hostVM.storageAccountName = AzureDockerUtils.getDefaultRandomName(host.name, 22) + "sa";
    host.hostVM.storageAccountType = "Premium_LSR";
    host.hostVM.osDiskName = name.toLowerCase() + "-osdisk.vhd";
    host.hostVM.osHost = AzureDockerUtils.getKnownDockerVirtualMachineImage(host.hostOSType).getAzureOSHost();

    host.hasPwdLogIn = true;
    host.hasSSHLogIn = true;
    host.isTLSSecured = true;
    host.hasKeyVault = true;

    host.certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH key for " + name);;
    host.certVault.name = AzureDockerUtils.getDefaultRandomName(host.name.toLowerCase(), 22) + "kv";
    host.certVault.hostName = host.hostVM.name;
    host.certVault.resourceGroupName = host.hostVM.resourceGroupName;
    host.certVault.region = host.hostVM.region;
    host.certVault.uri = "https://" + host.certVault.name + "." + host.certVault.resourceGroupName + ".azure.net";
    if (AzureDockerUtils.hasServicePrincipalAzureManager(azureAuthManager)) {
      host.certVault.userId = null;
      host.certVault.servicePrincipalId = userId;
    } else {
      host.certVault.userId = userId;
      host.certVault.servicePrincipalId = null;
    }
    host.certVault.vmUsername = "dockerUser";
    host.certVault.vmPwd = AzureDockerUtils.getDefaultRandomName(name);
    AzureDockerCertVaultOps.copyVaultTlsCerts(host.certVault, AzureDockerCertVaultOps.generateTLSCerts("TLS certs for " + name));

    host.hostVM.tags = new HashMap<>();
//    dockerHost.hostVM.tags.put("dockerhost", dockerHost.port);
//    dockerHost.hostVM.tags.put("dockervault", dockerHost.certVault.name);

    return host;
  }

  public AzureDockerImageInstance getDefaultDockerImageDescription(String projectName, DockerHost dockerHost) {
    AzureDockerImageInstance dockerImageDescription = new AzureDockerImageInstance();
    dockerImageDescription.dockerImageName = AzureDockerUtils.getDefaultDockerImageName(projectName).toLowerCase();
    dockerImageDescription.dockerContainerName = AzureDockerUtils.getDefaultDockerContainerName(dockerImageDescription.dockerImageName);
    dockerImageDescription.artifactName = AzureDockerUtils.getDefaultArtifactName(projectName).toLowerCase();
    dockerImageDescription.isHttpsWebApp = false;
    dockerImageDescription.hasNewDockerHost = false;
    if (dockerHost != null) {
      dockerImageDescription.host = dockerHost;
      dockerImageDescription.sid = dockerHost.sid;
    } else {
      dockerImageDescription.host = createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(projectName)));
//      dockerImageDescription.hasNewDockerHost = true;
    }
    if (dockerPreferredSettings != null) {
      dockerImageDescription.predefinedDockerfile = dockerPreferredSettings.dockerfileOption;
    }

    return dockerImageDescription;
  }

  /* Retrieves a Docker dockerHost object for a given API
   *   The API URL is unique and can be safely used to get a specific docker dockerHost description
   */
  public DockerHost getDockerHostForURL(String apiURL) {
    return dockerHostsMap.get(apiURL);
  }

  public List<String> getDockerVMStates() {
    List<String> result = new ArrayList<>();
    for (DockerHost.DockerHostVMState state : DockerHost.DockerHostVMState.values()) {
      result.add(state.name());
    }

    return result;
  }

  public static List<String> getDockerVMStateToActionList(DockerHost.DockerHostVMState currentVMState) {
    if (currentVMState == DockerHost.DockerHostVMState.RUNNING) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.STOPPED) {
      return Arrays.asList(currentVMState.toString(), "Start", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.UNKNOWN) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else {
      return Arrays.asList(currentVMState.toString());
    }
  }

  public void updateDockerHost(DockerHost originalDockerHost, DockerHost updatedDockerHost) {
    try {
      Thread.sleep(20000);
    } catch (Exception e) {}
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
    host.hostOSType = DockerHost.DockerHostOSType.UBUNTU_SERVER_16_04_LTS;
    host.hostVM = new AzureDockerVM();
    host.hostVM.name = name;
    host.hostVM.vmSize = KnownDockerVirtualMachineSizes.Standard_DS2_v2.name();
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
    host.certVault.uri = host.certVault.name + ".someregion.azure.com";
    host.certVault.hostName = name;
    host.certVault.resourceGroupName = "mykeyvault1rg";
    host.certVault.region = "centralus";
    host.certVault.userId = "dockerUser";
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
