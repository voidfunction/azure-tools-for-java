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
package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationResponse;
import com.microsoft.windowsazure.core.OperationStatus;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.core.pipeline.apache.ApacheConfigurationProperties;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.exception.CloudError;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.*;
import com.microsoft.windowsazure.management.compute.*;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.configuration.PublishSettingsLoader;
import com.microsoft.windowsazure.management.models.AffinityGroupListResponse;
import com.microsoft.windowsazure.management.models.LocationsListResponse;
import com.microsoft.windowsazure.management.models.RoleSizeListResponse;
import com.microsoft.windowsazure.management.models.SubscriptionGetResponse;
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.network.NetworkManagementService;
import com.microsoft.windowsazure.management.network.NetworkOperations;
import com.microsoft.windowsazure.management.network.models.NetworkListResponse;
import com.microsoft.windowsazure.management.storage.StorageAccountOperations;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementService;
import com.microsoft.windowsazure.management.storage.models.*;
import org.xml.sax.SAXException;

import javax.security.cert.X509Certificate;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class AzureSDKHelper {
    private static class StatusLiterals {
        private static final String UNKNOWN = "Unknown";
        private static final String READY_ROLE = "ReadyRole";
        private static final String STOPPED_VM = "StoppedVM";
        private static final String STOPPED_DEALLOCATED = "StoppedDeallocated";
        private static final String BUSY_ROLE = "BusyRole";
        private static final String CREATING_VM = "CreatingVM";
        private static final String CREATING_ROLE = "CreatingRole";
        private static final String STARTING_VM = "StartingVM";
        private static final String STARTING_ROLE = "StartingRole";
        private static final String STOPPING_VM = "StoppingVM";
        private static final String STOPPING_ROLE = "StoppingRole";
        private static final String DELETING_VM = "DeletingVM";
        private static final String RESTARTING_ROLE = "RestartingRole";
        private static final String CYCLING_ROLE = "CyclingRole";
        private static final String FAILED_STARTING_VM = "FailedStartingVM";
        private static final String FAILED_STARTING_ROLE = "FailedStartingRole";
        private static final String UNRESPONSIVE_ROLE = "UnresponsiveRole";
        private static final String PREPARING = "Preparing";
    }

    private static final String PERSISTENT_VM_ROLE = "PersistentVMRole";
    private static final String NETWORK_CONFIGURATION = "NetworkConfiguration";
    private static final String PLATFORM_IMAGE = "Platform";
    private static final String USER_IMAGE = "User";
    private static final String WINDOWS_OS_TYPE = "Windows";
    private static final String LINUX_OS_TYPE = "Linux";
    private static final String WINDOWS_PROVISIONING_CONFIGURATION = "WindowsProvisioningConfiguration";
    private static final String LINUX_PROVISIONING_CONFIGURATION = "LinuxProvisioningConfiguration";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static boolean IS_INTELLIJ_PLUGIN = false;

    private static CloudStorageAccount getCloudStorageAccount(String blobLink, String saKey) throws Exception {
        if (blobLink == null || blobLink.isEmpty()) {
            throw new IllegalArgumentException("Invalid blob link, it's null or empty: " + blobLink);
        }
        if (saKey == null || saKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid storage account key, it's null or empty: " + saKey);
        }
        // check the link is valic
        URI blobUri = new URL(blobLink).toURI();
        String host =  blobUri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid blobLink, can't find host: " + blobLink);
        }
        String storageAccountName = host.substring(0, host.indexOf("."));
        String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageAccountName, saKey);
        CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);
        return cloudStorageAccount;
    }

    public static String  getBlobSasUri(String blobLink, String saKey) throws Exception {
        CloudStorageAccount cloudStorageAccount = getCloudStorageAccount(blobLink, saKey);
        // Create the blob client.
        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        // Get container and blob name from the link
        String path = new URI(blobLink).getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid blobLink: " + blobLink);
        }
        int containerNameEndIndex = path.indexOf("/", 1);
        String containerName = path.substring(1, containerNameEndIndex);
        if (containerName == null || containerName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find container name: " + blobLink);
        }
        String blobName = path.substring(path.indexOf("/", containerNameEndIndex)+1);
        if (blobName == null || blobName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find blob name: " + blobLink);
        }
        // Retrieve reference to a previously created container.
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        SharedAccessBlobPolicy sharedAccessBlobPolicy = new SharedAccessBlobPolicy();
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(new Date());
        sharedAccessBlobPolicy.setSharedAccessStartTime(calendar.getTime());
        calendar.add(Calendar.HOUR, 23);
        sharedAccessBlobPolicy.setSharedAccessExpiryTime(calendar.getTime());
        sharedAccessBlobPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        container.uploadPermissions(containerPermissions);
        String signature = container.generateSharedAccessSignature(sharedAccessBlobPolicy, null);
        return blobLink + "?" + signature;
    }

    @NotNull
    public static SDKRequestCallback<List<CloudService>, ComputeManagementClient> getCloudServices(@NotNull final String subscriptionId) {
        return new SDKRequestCallback<List<CloudService>, ComputeManagementClient>() {
            @NotNull
            @Override
            public List<CloudService> execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                List<CloudService> csList = new ArrayList<CloudService>();
                ArrayList<HostedServiceListResponse.HostedService> hostedServices = getHostedServices(client).getHostedServices();

                if (hostedServices == null) {
                    return csList;
                }

                for (HostedServiceListResponse.HostedService hostedService : hostedServices) {
                    ListenableFuture<DeploymentGetResponse> productionFuture = getDeploymentAsync(
                            client,
                            hostedService.getServiceName(),
                            DeploymentSlot.Production);
                    ListenableFuture<DeploymentGetResponse> stagingFuture = getDeploymentAsync(
                            client,
                            hostedService.getServiceName(),
                            DeploymentSlot.Staging);

                    DeploymentGetResponse prodDGR = productionFuture.get();

                    DeploymentGetResponse stagingDGR = stagingFuture.get();

                    CloudService cloudService = new CloudService(
                            hostedService.getServiceName() != null ? hostedService.getServiceName() : "",
                            hostedService.getProperties() != null && hostedService.getProperties().getLocation() != null ?
                                    hostedService.getProperties().getLocation() :
                                    "",
                            hostedService.getProperties() != null && hostedService.getProperties().getAffinityGroup() != null ?
                                    hostedService.getProperties().getAffinityGroup() :
                                    "",
                            subscriptionId);
                    cloudService.setUri(hostedService.getUri());

                    loadDeployment(prodDGR, cloudService);

                    cloudService = loadDeployment(prodDGR, cloudService);
                    cloudService = loadDeployment(stagingDGR, cloudService);

                    csList.add(cloudService);
                }

                return csList;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<StorageAccount>, StorageManagementClient> getStorageAccounts(@NotNull final String subscriptionId, final boolean detailed) {
        return new SDKRequestCallback<List<StorageAccount>, StorageManagementClient>() {
            @NotNull
            @Override
            public List<StorageAccount> execute(@NotNull StorageManagementClient client)
                    throws Throwable {
                List<StorageAccount> saList = new ArrayList<StorageAccount>();
                ArrayList<com.microsoft.windowsazure.management.storage.models.StorageAccount> storageAccounts =
                        getStorageAccounts(client).getStorageAccounts();

                if (storageAccounts == null) {
                    return saList;
                }

                List<ListenableFuture<StorageAccount>> saFutureList = new ArrayList<ListenableFuture<StorageAccount>>();

                for (com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount : storageAccounts) {
                    saFutureList.add(getStorageAccountAsync(subscriptionId, client, storageAccount, detailed));
                }

                saList.addAll(Futures.allAsList(saFutureList).get());

                return saList;
            }
        };
    }

    public static SDKRequestCallback<Boolean, StorageManagementClient> checkStorageNameAvailability(final String storageAccountName) {
        return new SDKRequestCallback<Boolean, StorageManagementClient>() {
            @NotNull
            @Override
            public Boolean execute(@NotNull StorageManagementClient client)
                    throws Throwable {
                CheckNameAvailabilityResponse response = getStorageAccountOperations(client).checkNameAvailability(storageAccountName);
                return response.isAvailable();
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<Location>, ManagementClient> getLocations() {
        return new SDKRequestCallback<List<Location>, ManagementClient>() {
            @NotNull
            @Override
            public List<Location> execute(@NotNull ManagementClient client) throws Throwable {
                List<Location> locationList = new ArrayList<Location>();
                locationList = loadLocations(client, locationList);

                return locationList;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<AffinityGroup>, ManagementClient> getAffinityGroups() {
        return new SDKRequestCallback<List<AffinityGroup>, ManagementClient>() {
            @NotNull
            @Override
            public List<AffinityGroup> execute(@NotNull ManagementClient client) throws Throwable {
                List<AffinityGroup> affinityGroupList = new ArrayList<AffinityGroup>();
                affinityGroupList = loadAffinityGroups(client, affinityGroupList);

                return affinityGroupList;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<VirtualNetwork>, NetworkManagementClient> getVirtualNetworks(@NotNull final String subscriptionId) {
        return new SDKRequestCallback<List<VirtualNetwork>, NetworkManagementClient>() {
            @NotNull
            @Override
            public List<VirtualNetwork> execute(@NotNull NetworkManagementClient client)
                    throws Throwable {
                List<VirtualNetwork> vnList = new ArrayList<VirtualNetwork>();

                ArrayList<NetworkListResponse.VirtualNetworkSite> virtualNetworkSites =
                        getNetworks(client).getVirtualNetworkSites();

                if (virtualNetworkSites == null) {
                    return vnList;
                }

                for (NetworkListResponse.VirtualNetworkSite virtualNetworkSite : virtualNetworkSites) {
                    VirtualNetwork vn = new VirtualNetwork(
                            virtualNetworkSite.getName() != null ? virtualNetworkSite.getName() : "",
                            virtualNetworkSite.getId() != null ? virtualNetworkSite.getId() : "",
                            virtualNetworkSite.getLocation() != null ? virtualNetworkSite.getLocation() : "",
                            virtualNetworkSite.getAffinityGroup() != null ? virtualNetworkSite.getAffinityGroup() : "",
                            subscriptionId);

                    if (virtualNetworkSite.getSubnets() != null) {
                        Set<String> vnSubnets = vn.getSubnets();

                        for (NetworkListResponse.Subnet subnet : virtualNetworkSite.getSubnets()) {
                            if (subnet.getName() != null && !subnet.getName().isEmpty()) {
                                vnSubnets.add(subnet.getName());
                            }
                        }
                    }

                    vnList.add(vn);
                }

                return vnList;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<OperationStatusResponse, StorageManagementClient> createStorageAccount(@NotNull final StorageAccount storageAccount) {
        return new SDKRequestCallback<OperationStatusResponse, StorageManagementClient>() {
            @NotNull
            @Override
            public OperationStatusResponse execute(@NotNull StorageManagementClient client)
                    throws Throwable {
                StorageAccountOperations sao = getStorageAccountOperations(client);
                StorageAccountCreateParameters sacp = new StorageAccountCreateParameters(storageAccount.getName(),
                        storageAccount.getName());
                sacp.setAccountType(storageAccount.getType());

                if (!storageAccount.getAffinityGroup().isEmpty()) {
                    sacp.setAffinityGroup(storageAccount.getAffinityGroup());
                } else if (!storageAccount.getLocation().isEmpty()) {
                    sacp.setLocation(storageAccount.getLocation());
                }

                OperationStatusResponse osr = sao.create(sacp);
                validateOperationStatus(osr);

                return osr;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<Void, ComputeManagementClient> createCloudService(@NotNull final CloudService cloudService) {
        return new SDKRequestCallback<Void, ComputeManagementClient>() {
            @NotNull
            @Override
            public Void execute(@NotNull ComputeManagementClient client) throws Throwable {
                HostedServiceOperations hso = getHostedServiceOperations(client);
                HostedServiceCreateParameters hscp = new HostedServiceCreateParameters(cloudService.getName(),
                        cloudService.getName());

                if (!cloudService.getAffinityGroup().isEmpty()) {
                    hscp.setAffinityGroup(cloudService.getAffinityGroup());
                } else if (!cloudService.getLocation().isEmpty()) {
                    hscp.setLocation(cloudService.getLocation());
                }
                hscp.setDescription(cloudService.getDescription());
                OperationResponse or = hso.create(hscp);

                if (or == null) {
                    throw new Exception("Unable to retrieve Operation");
                }

                OperationStatusResponse osr = getOperationStatusResponse(client, or);
                validateOperationStatus(osr);

                return null;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<CloudService, ComputeManagementClient> getCloudServiceDetailed(@NotNull final String subscriptionId, @NotNull final String name) {
        return new SDKRequestCallback<CloudService, ComputeManagementClient>() {
            @NotNull
            @Override
            public CloudService execute(@NotNull ComputeManagementClient client) throws Throwable {
                HostedServiceOperations hso = getHostedServiceOperations(client);
                HostedServiceGetDetailedResponse response = hso.getDetailed(name);
                HostedServiceProperties properties = response.getProperties();
                CloudService cloudService =
                        new CloudService(response.getServiceName(), properties.getLocation(), properties.getAffinityGroup(), subscriptionId, properties.getDescription());
                cloudService.setUri(response.getUri());

                for (HostedServiceGetDetailedResponse.Deployment deployment : response.getDeployments()) {
                    CloudService.Deployment d;
                	if (deployment.getDeploymentSlot() == DeploymentSlot.Production) {
                		d = cloudService.getProductionDeployment();
                	} else {
                		d = cloudService.getStagingDeployment();
                	}
                	d.setName(deployment.getName());
                	d.setLabel(deployment.getLabel());
                	d.setStatus(deployment.getStatus());
                }

                return cloudService;
            }
        };
    }

    public static SDKRequestCallback<Boolean, ComputeManagementClient> checkHostedServiceNameAvailability(final String hostedServiceName) {
        return new SDKRequestCallback<Boolean, ComputeManagementClient>() {
            @NotNull
            @Override
            public Boolean execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                HostedServiceCheckNameAvailabilityResponse response = getHostedServiceOperations(client).checkNameAvailability(hostedServiceName);
                return response.isAvailable();
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<OperationStatusResponse, ComputeManagementClient> createDeployment(final String serviceName, final String slotName,
                                                                                     final DeploymentCreateParameters parameters, final String unpublish) {
        return new SDKRequestCallback<OperationStatusResponse, ComputeManagementClient>() {
            @NotNull
            @Override
            public OperationStatusResponse execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                DeploymentOperations deploymentOperations = getDeploymentOperations(client);
                DeploymentSlot deploymentSlot;
                if (DeploymentSlot.Staging.toString().equalsIgnoreCase(slotName)) {
                    deploymentSlot = DeploymentSlot.Staging;
                } else if (DeploymentSlot.Production.toString().equalsIgnoreCase(slotName)) {
                    deploymentSlot = DeploymentSlot.Production;
                } else {
                    throw new Exception("Invalid deployment slot name");
                }
                OperationStatusResponse response;
                try {
                    response = createDeployment(client, deploymentOperations, serviceName, deploymentSlot, parameters);
                    return response;
                } catch (ServiceException ex) {
			/*
			 * If delete deployment option is selected and
			 * conflicting deployment exists then upgrade deployment.
			 */
                    if (unpublish.equalsIgnoreCase("true") && ex.getHttpStatusCode() == 409) {
                        DeploymentUpgradeParameters upgradeParameters = new DeploymentUpgradeParameters();
                        upgradeParameters.setConfiguration(parameters.getConfiguration());
                        upgradeParameters.setForce(true);
                        upgradeParameters.setLabel(parameters.getName());
                        upgradeParameters.setMode(DeploymentUpgradeMode.Auto);
                        upgradeParameters.setPackageUri(parameters.getPackageUri());
                        response = upgradeDeployment(client, deploymentOperations, serviceName, deploymentSlot, upgradeParameters);
                        return response;
                    } else {
                        throw ex;
                    }
                }
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<OperationStatusResponse, ComputeManagementClient> deleteDeployment(@NotNull final String serviceName, final String deploymentName,
                                                                                                        final boolean deleteFromStorage) {
        return new SDKRequestCallback<OperationStatusResponse, ComputeManagementClient>() {
            @NotNull
            @Override
            public OperationStatusResponse execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                return deleteDeployment(client, serviceName, deploymentName, deleteFromStorage);
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<OperationStatusResponse, ComputeManagementClient> waitForStatus(final OperationStatusResponse operationStatusResponse) {
        return new SDKRequestCallback<OperationStatusResponse, ComputeManagementClient>() {
            @NotNull
            @Override
            public OperationStatusResponse execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                return getOperationStatusResponse(client, operationStatusResponse);
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<StorageAccount, StorageManagementClient> refreshStorageAccountInformation(@NotNull final StorageAccount storageAccount) {
        return new SDKRequestCallback<StorageAccount, StorageManagementClient>() {
            @NotNull
            @Override
            public StorageAccount execute(@NotNull StorageManagementClient client)
                    throws Throwable {
                StorageAccountOperations sao = getStorageAccountOperations(client);
                StorageAccountGetResponse sagr = sao.get(storageAccount.getName());

                if (sagr == null) {
                    throw new Exception("Unable to retrieve Operation");
                }

                OperationStatusResponse osr = getOperationStatusResponse(client, sagr);
                validateOperationStatus(osr);

                if (sagr.getStorageAccount() == null) {
                    throw new Exception("Invalid Storage Account information. No Storage Account matches the specified data.");
                }

                StorageAccount sa = getStorageAccount(storageAccount.getSubscriptionId(), client, sagr.getStorageAccount(), true);
                storageAccount.setType(sa.getType());
                storageAccount.setDescription(sa.getDescription());
                storageAccount.setLabel(sa.getLabel());
                storageAccount.setStatus(sa.getStatus());
                storageAccount.setLocation(sa.getLocation());
                storageAccount.setAffinityGroup(sa.getAffinityGroup());
                storageAccount.setPrimaryKey(sa.getPrimaryKey());
                storageAccount.setSecondaryKey(sa.getSecondaryKey());
                storageAccount.setManagementUri(sa.getManagementUri());
                storageAccount.setBlobsUri(sa.getBlobsUri());
                storageAccount.setQueuesUri(sa.getQueuesUri());
                storageAccount.setTablesUri(sa.getTablesUri());
                storageAccount.setPrimaryRegion(sa.getPrimaryRegion());
                storageAccount.setPrimaryRegionStatus(sa.getPrimaryRegionStatus());
                storageAccount.setSecondaryRegion(sa.getSecondaryRegion());
                storageAccount.setSecondaryRegionStatus(sa.getSecondaryRegionStatus());
                storageAccount.setLastFailover(sa.getLastFailover());

                return storageAccount;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<String, ComputeManagementClient> createServiceCertificate(@NotNull final String serviceName,
                                                                                               @NotNull final byte[] data,
                                                                                               @NotNull final String password,
                                                                                               final boolean needThumbprint) {
        return new SDKRequestCallback<String, ComputeManagementClient>() {
            @NotNull
            @Override
            public String execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
            	String thumbprint = "";
            	if (needThumbprint) {
            		MessageDigest md = MessageDigest.getInstance("SHA1");
            		X509Certificate cert = X509Certificate.getInstance(data);
            		md.update(cert.getEncoded());
            		thumbprint = bytesToHex(md.digest());
            	}
                ServiceCertificateOperations sco = getServiceCertificateOperations(client);
                ServiceCertificateCreateParameters sccp = new ServiceCertificateCreateParameters(data,
                        CertificateFormat.Pfx);
                sccp.setPassword(password);

                OperationStatusResponse osr = sco.create(serviceName, sccp);
                validateOperationStatus(osr);

                return thumbprint;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<ServiceCertificateListResponse.Certificate>, ComputeManagementClient> getCertificates(@NotNull final String serviceName) {
        return new SDKRequestCallback<List<ServiceCertificateListResponse.Certificate>, ComputeManagementClient>() {
            @NotNull
            @Override
            public List<ServiceCertificateListResponse.Certificate> execute(@NotNull ComputeManagementClient client)
                    throws Throwable {
                ServiceCertificateOperations sco = getServiceCertificateOperations(client);
                return sco.list(serviceName).getCertificates();
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<Void, StorageManagementClient> deleteStorageAccount(@NotNull final ClientStorageAccount storageAccount) {
        return new SDKRequestCallback<Void, StorageManagementClient>() {
            @NotNull
            @Override
            public Void execute(@NotNull StorageManagementClient client)
                    throws Throwable {
                StorageAccountOperations sao = getStorageAccountOperations(client);
                OperationResponse or = sao.delete(storageAccount.getName());
                OperationStatusResponse osr = getOperationStatusResponse(client, or);
                validateOperationStatus(osr);
                return null;
            }
        };
    }

    @NotNull
    public static SDKRequestCallback<List<Resource>, ApplicationInsightsManagementClient> getApplicationInsightsResources(
    		@NotNull final String subscriptionId) {
    	return new SDKRequestCallback<List<Resource>, ApplicationInsightsManagementClient>() {
    		@NotNull
    		@Override
    		public List<Resource> execute(@NotNull ApplicationInsightsManagementClient client)
    				throws Throwable {
    			return client.getResources(subscriptionId);
    		}
    	};
    }

    @NotNull
    public static SDKRequestCallback<List<String>, ApplicationInsightsManagementClient> getLocationsForApplicationInsights() {
    	return new SDKRequestCallback<List<String>, ApplicationInsightsManagementClient>() {
    		@NotNull
    		@Override
    		public List<String> execute(@NotNull ApplicationInsightsManagementClient client)
    				throws Throwable {
    			return client.getAvailableGeoLocations();
    		}
    	};
    }

    @NotNull
    public static SDKRequestCallback<Resource, ApplicationInsightsManagementClient> createApplicationInsightsResource(
    		@NotNull final String subscriptionId,
    		@NotNull final String resourceGroupName,
    		@NotNull final String resourceName,
    		@NotNull final String location) {
    	return new SDKRequestCallback<Resource, ApplicationInsightsManagementClient>() {
    		@NotNull
    		@Override
    		public Resource execute(@NotNull ApplicationInsightsManagementClient client)
    				throws Throwable {
    			return client.createResource(subscriptionId, resourceGroupName, resourceName, location);
    		}
    	};
    }

    @NotNull
    public static ComputeManagementClient getComputeManagementClient(@NotNull String subscriptionId,
                                                                     @NotNull String managementCertificate,
                                                                     @NotNull String serviceManagementUrl)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            XPathExpressionException, ParserConfigurationException, SAXException, AzureCmdException {
        Configuration configuration = getConfigurationFromKeystore(subscriptionId, serviceManagementUrl);

        if (configuration == null) {
            throw new AzureCmdException("Unable to instantiate Configuration");
        }

        ComputeManagementClient client = ComputeManagementService.create(configuration);

        if (client == null) {
            throw new AzureCmdException("Unable to instantiate Compute Management client");
        }
        client.withRequestFilterFirst(new AzureToolkitFilter());
        return client;
    }

    @NotNull
    public static StorageManagementClient getStorageManagementClient(@NotNull String subscriptionId,
                                                                     @NotNull String managementCertificate,
                                                                     @NotNull String serviceManagementUrl)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            XPathExpressionException, ParserConfigurationException, SAXException, AzureCmdException {
        Configuration configuration = getConfigurationFromKeystore(subscriptionId, serviceManagementUrl);

        if (configuration == null) {
            throw new AzureCmdException("Unable to instantiate Configuration");
        }

        StorageManagementClient client = StorageManagementService.create(configuration);

        if (client == null) {
            throw new AzureCmdException("Unable to instantiate Storage Management client");
        }
        client.withRequestFilterFirst(new AzureToolkitFilter());
        return client;
    }

    @NotNull
    public static NetworkManagementClient getNetworkManagementClient(@NotNull String subscriptionId,
                                                                     @NotNull String managementCertificate,
                                                                     @NotNull String serviceManagementUrl)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            XPathExpressionException, ParserConfigurationException, SAXException, AzureCmdException {
        Configuration configuration = getConfigurationFromKeystore(subscriptionId, serviceManagementUrl);

        if (configuration == null) {
            throw new AzureCmdException("Unable to instantiate Configuration");
        }

        NetworkManagementClient client = NetworkManagementService.create(configuration);

        if (client == null) {
            throw new AzureCmdException("Unable to instantiate Network Management client");
        }
        client.withRequestFilterFirst(new AzureToolkitFilter());
        return client;
    }

    @NotNull
    public static ApplicationInsightsManagementClient getApplicationManagementClient(@NotNull String tenantId, @NotNull String accessToken)
    		throws RestOperationException, IOException {
    	String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0";
    	ApplicationInsightsManagementClient client = new ApplicationInsightsManagementClient(tenantId, accessToken, userAgent);
    	return client;
    }

    @NotNull
    public static SubscriptionGetResponse getSubscription(@NotNull Configuration config) throws AzureCmdException {
    	ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    	try {
    		// change classloader only for intellij plugin - for some reason Eclipse does not need it
    		if (IS_INTELLIJ_PLUGIN) {
    			// Change context classloader to class context loader
    			Thread.currentThread().setContextClassLoader(AzureManager.class.getClassLoader());
    		}
    		ManagementClient client = ManagementService.create(config);
    		return client.getSubscriptionsOperations().get();
    	} catch(Exception ex) {
    		throw new AzureCmdException(ex.getMessage());
    	}
    	finally {
    		// Call Azure API and reset back the context loader
    		Thread.currentThread().setContextClassLoader(contextLoader);
    	}
    }

    @NotNull
    public static ManagementClient getManagementClient(@NotNull String subscriptionId,
                                                       @NotNull String managementCertificate,
                                                       @NotNull String serviceManagementUrl)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            XPathExpressionException, ParserConfigurationException, SAXException, AzureCmdException {
        Configuration configuration = getConfigurationFromKeystore(subscriptionId, serviceManagementUrl);

        if (configuration == null) {
            throw new AzureCmdException("Unable to instantiate Configuration");
        }

        ManagementClient client = ManagementService.create(configuration);

        if (client == null) {
            throw new AzureCmdException("Unable to instantiate Management client");
        }
        client.withRequestFilterFirst(new AzureToolkitFilter());
        return client;
    }

    @NotNull
    public static CloudStorageAccount getCloudStorageAccount(@NotNull String connectionString)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString);
    }

    @NotNull
    private static CloudBlobClient getCloudBlobClient(@NotNull ClientStorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(storageAccount.getConnectionString());

        return csa.createCloudBlobClient();
    }

    @NotNull
    private static HostedServiceOperations getHostedServiceOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        HostedServiceOperations hso = client.getHostedServicesOperations();

        if (hso == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hso;
    }

    @NotNull
    private static DeploymentOperations getDeploymentOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        DeploymentOperations dop = client.getDeploymentsOperations();

        if (dop == null) {
            throw new Exception("Unable to retrieve Deployment information");
        }

        return dop;
    }

    @NotNull
    private static VirtualMachineOperations getVirtualMachineOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        VirtualMachineOperations vmo = client.getVirtualMachinesOperations();

        if (vmo == null) {
            throw new Exception("Unable to retrieve Virtual Machines Information");
        }

        return vmo;
    }

    @NotNull
    private static VirtualMachineOSImageOperations getVirtualMachineOSImageOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        VirtualMachineOSImageOperations vmosio = client.getVirtualMachineOSImagesOperations();

        if (vmosio == null) {
            throw new Exception("Unable to retrieve OS Images information");
        }

        return vmosio;
    }

    @NotNull
    private static VirtualMachineVMImageOperations getVirtualMachineVMImageOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        VirtualMachineVMImageOperations vmvmio = client.getVirtualMachineVMImagesOperations();

        if (vmvmio == null) {
            throw new Exception("Unable to retrieve VM Images information");
        }

        return vmvmio;
    }

    @NotNull
    private static RoleSizeOperations getRoleSizeOperations(@NotNull ManagementClient client)
            throws Exception {
        RoleSizeOperations rso = client.getRoleSizesOperations();

        if (rso == null) {
            throw new Exception("Unable to retrieve Role Sizes information");
        }

        return rso;
    }

    @NotNull
    private static LocationOperations getLocationsOperations(@NotNull ManagementClient client)
            throws Exception {
        LocationOperations lo = client.getLocationsOperations();

        if (lo == null) {
            throw new Exception("Unable to retrieve Locations information");
        }

        return lo;
    }

    @NotNull
    private static AffinityGroupOperations getAffinityGroupOperations(@NotNull ManagementClient client)
            throws Exception {
        AffinityGroupOperations ago = client.getAffinityGroupsOperations();

        if (ago == null) {
            throw new Exception("Unable to retrieve Affinity Groups information");
        }

        return ago;
    }

    @NotNull
    private static StorageAccountOperations getStorageAccountOperations(@NotNull StorageManagementClient client)
            throws Exception {
        StorageAccountOperations sao = client.getStorageAccountsOperations();

        if (sao == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return sao;
    }

    @NotNull
    private static NetworkOperations getNetworkOperations(@NotNull NetworkManagementClient client)
            throws Exception {
        NetworkOperations no = client.getNetworksOperations();

        if (no == null) {
            throw new Exception("Unable to retrieve Network information");
        }

        return no;
    }

    @NotNull
    private static ServiceCertificateOperations getServiceCertificateOperations(@NotNull ComputeManagementClient client)
            throws Exception {
        ServiceCertificateOperations sco = client.getServiceCertificatesOperations();

        if (sco == null) {
            throw new Exception("Unable to retrieve Service Certificate information");
        }

        return sco;
    }

    @NotNull
    private static HostedServiceListResponse getHostedServices(@NotNull ComputeManagementClient client)
            throws Exception {
        HostedServiceOperations hso = getHostedServiceOperations(client);

        HostedServiceListResponse hslr = hso.list();

        if (hslr == null) {
            throw new Exception("Unable to retrieve Hosted Services information");
        }

        return hslr;
    }

    @NotNull
    private static ListenableFuture<DeploymentGetResponse> getDeploymentAsync(@NotNull final ComputeManagementClient client,
                                                                              @NotNull final String serviceName,
                                                                              @NotNull final DeploymentSlot slot) {
        final SettableFuture<DeploymentGetResponse> future = SettableFuture.create();

        AzureManager.getManager().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getDeployment(client, serviceName, slot));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });

        return future;
    }

    @NotNull
    private static DeploymentGetResponse getDeployment(@NotNull ComputeManagementClient client,
                                                       @NotNull String serviceName,
                                                       @NotNull DeploymentSlot slot)
            throws Exception {
        try {
            DeploymentGetResponse dgr = getDeploymentOperations(client).getBySlot(serviceName, slot);

            if (dgr == null) {
                throw new Exception("Unable to retrieve Deployment information");
            }

            return dgr;
        } catch (ServiceException se) {
            if (se.getHttpStatusCode() == 404) {
                return new DeploymentGetResponse();
            } else {
                throw se;
            }
        }
    }

    public static OperationStatusResponse createDeployment(@NotNull ComputeManagementClient client, @NotNull DeploymentOperations deploymentOperations,
                                                           String serviceName, DeploymentSlot deploymentSlot, DeploymentCreateParameters parameters)
            throws Exception {
        try {
            return deploymentOperations.create(serviceName, deploymentSlot, parameters);
        } catch (ServiceException ex) {
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            }
            throw new Exception("Exception when create deployment", ex);
        } catch (Exception ex) {
            throw new Exception("Exception when create deployment", ex);
        }
    }

    public static OperationStatusResponse upgradeDeployment(@NotNull ComputeManagementClient client, @NotNull DeploymentOperations deploymentOperations,
                                                           String serviceName, DeploymentSlot deploymentSlot, DeploymentUpgradeParameters parameters)
            throws Exception {
        try {
            return deploymentOperations.upgradeBySlot(serviceName, deploymentSlot, parameters);
        } catch (ServiceException ex) {
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            }
            throw new Exception("Exception when upgrading deployment", ex);
        } catch (Exception ex) {
            throw new Exception("Exception when upgrading deployment", ex);
        }
    }

    @NotNull
    private static StorageAccountListResponse getStorageAccounts(@NotNull StorageManagementClient client) throws Exception {
        StorageAccountListResponse salr = getStorageAccountOperations(client).list();

        if (salr == null) {
            throw new Exception("Unable to retrieve Storage Accounts information");
        }

        return salr;
    }

    @NotNull
    private static ListenableFuture<StorageAccount> getStorageAccountAsync(@NotNull final String subscriptionId,
                                                                           @NotNull final StorageManagementClient client,
                                                                           @NotNull final com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount,
                                                                           final boolean detailed)
            throws Exception {
        final SettableFuture<StorageAccount> future = SettableFuture.create();

        AzureManager.getManager().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(getStorageAccount(subscriptionId, client, storageAccount, detailed));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });

        return future;
    }

    @NotNull
    private static StorageAccount getStorageAccount(@NotNull String subscriptionId,
                                                    @NotNull StorageManagementClient client,
                                                    @NotNull com.microsoft.windowsazure.management.storage.models.StorageAccount storageAccount,
                                                    boolean detailed) throws Exception {
        String primaryKey = "";
        String secondaryKey = "";

        if (storageAccount.getName() != null && detailed) {
            StorageAccountGetKeysResponse sak = getStorageAccountKeys(client, storageAccount.getName());

            primaryKey = sak.getPrimaryKey();
            secondaryKey = sak.getSecondaryKey();
        }

        StorageAccountProperties sap = storageAccount.getProperties() != null ?
                storageAccount.getProperties() :
                new StorageAccountProperties();
        String blobsUri = "";
        String queuesUri = "";
        String tablesUri = "";

        ArrayList<URI> endpoints = sap.getEndpoints();

        if (endpoints != null && endpoints.size() > 0) {
            blobsUri = endpoints.get(0).toString();

            if (endpoints.size() > 1) {
                queuesUri = endpoints.get(1).toString();

                if (endpoints.size() > 2) {
                    tablesUri = endpoints.get(2).toString();
                }
            }
        }

        StorageAccount sa = new StorageAccount(Strings.nullToEmpty(storageAccount.getName()), subscriptionId);

        sa.setPrimaryKey(Strings.nullToEmpty(primaryKey));
        sa.setProtocol("https");
        sa.setBlobsUri(blobsUri);
        sa.setQueuesUri(queuesUri);
        sa.setTablesUri(tablesUri);
        sa.setUseCustomEndpoints(true);

        sa.setType(Strings.nullToEmpty(sap.getAccountType()));
        sa.setDescription(Strings.nullToEmpty(sap.getDescription()));
        sa.setLabel(Strings.nullToEmpty(sap.getLabel()));
        sa.setStatus(sap.getStatus() != null ? sap.getStatus().toString() : "");
        sa.setLocation(Strings.nullToEmpty(sap.getLocation()));
        sa.setAffinityGroup(Strings.nullToEmpty(sap.getAffinityGroup()));
        sa.setSecondaryKey(Strings.nullToEmpty(secondaryKey));
        sa.setManagementUri(storageAccount.getUri() != null ? storageAccount.getUri().toString() : "");
        sa.setPrimaryRegion(Strings.nullToEmpty(sap.getGeoPrimaryRegion()));
        sa.setPrimaryRegionStatus(sap.getStatusOfGeoPrimaryRegion() != null ? sap.getStatusOfGeoPrimaryRegion().toString() : "");
        sa.setSecondaryRegion(Strings.nullToEmpty(sap.getGeoSecondaryRegion()));
        sa.setSecondaryRegionStatus(sap.getStatusOfGeoSecondaryRegion() != null ? sap.getStatusOfGeoSecondaryRegion().toString() : "");
        sa.setLastFailover(sap.getLastGeoFailoverTime() != null ? sap.getLastGeoFailoverTime() : new GregorianCalendar());

        return sa;
    }

    @NotNull
    private static StorageAccountGetKeysResponse getStorageAccountKeys(@NotNull StorageManagementClient client,
                                                                       @NotNull String storageName)
            throws Exception {
        StorageAccountGetKeysResponse sagkr = getStorageAccountOperations(client).getKeys(storageName);

        if (sagkr == null) {
            throw new Exception("Unable to retrieve Storage Account Keys information");
        }

        return sagkr;
    }

    @NotNull
    private static List<Role> getVMDeploymentRoles(@NotNull DeploymentGetResponse deployment) throws Exception {
        ArrayList<Role> roles = deployment.getRoles();

        if (roles == null) {
            throw new Exception("Invalid Virtual Machine information. No Roles match the VM data.");
        }

        return roles;
    }

    @NotNull
    private static NetworkListResponse getNetworks(@NotNull NetworkManagementClient client) throws Exception {
        NetworkListResponse nlr = getNetworkOperations(client).list();

        if (nlr == null) {
            throw new Exception("Unable to retrieve Networks information");
        }

        return nlr;
    }

    private static void validateOperationStatus(@Nullable OperationStatusResponse osr) throws Exception {
        if (osr == null) {
            throw new Exception("Unable to retrieve Operation Status");
        }

        if (osr.getError() != null) {
            throw new Exception(osr.getError().getMessage());
        }
    }

    @Nullable
    private static OperationStatusResponse getOperationStatusResponse(@NotNull ComputeManagementClient client,
                                                                      @NotNull OperationResponse or)
            throws InterruptedException, ExecutionException, ServiceException {
        OperationStatusResponse osr = client.getOperationStatusAsync(or.getRequestId()).get();
        int delayInSeconds = 5;

        if (client.getLongRunningOperationInitialTimeout() >= 0) {
            delayInSeconds = client.getLongRunningOperationInitialTimeout();
        }

        while (osr.getStatus() == OperationStatus.InProgress) {
            Thread.sleep(delayInSeconds * 1000);
            osr = client.getOperationStatusAsync(or.getRequestId()).get();
            delayInSeconds = 5;

            if (client.getLongRunningOperationRetryTimeout() >= 0) {
                delayInSeconds = client.getLongRunningOperationRetryTimeout();
            }
        }

        if (osr.getStatus() != OperationStatus.Succeeded) {
            if (osr.getError() != null) {
                ServiceException ex = new ServiceException(osr.getError().getCode() + " : " + osr.getError().getMessage());
                CloudError cloudError = new CloudError();
                cloudError.setCode(osr.getError().getCode());
                cloudError.setMessage(osr.getError().getMessage());
                ex.setError(cloudError);
                throw ex;
            } else {
                throw new ServiceException("");
            }
        }

        return osr;
    }

    @Nullable
    private static OperationStatusResponse getOperationStatusResponse(@NotNull StorageManagementClient client,
                                                                      @NotNull OperationResponse or)
            throws InterruptedException, ExecutionException, ServiceException {
        OperationStatusResponse osr = client.getOperationStatusAsync(or.getRequestId()).get();
        int delayInSeconds = 30;

        if (client.getLongRunningOperationInitialTimeout() >= 0) {
            delayInSeconds = client.getLongRunningOperationInitialTimeout();
        }

        while (osr.getStatus() == OperationStatus.InProgress) {
            Thread.sleep(delayInSeconds * 1000);
            osr = client.getOperationStatusAsync(or.getRequestId()).get();
            delayInSeconds = 30;

            if (client.getLongRunningOperationRetryTimeout() >= 0) {
                delayInSeconds = client.getLongRunningOperationRetryTimeout();
            }
        }

        if (osr.getStatus() != OperationStatus.Succeeded) {
            if (osr.getError() != null) {
                ServiceException ex = new ServiceException(osr.getError().getCode() + " : " + osr.getError().getMessage());
                CloudError cloudError = new CloudError();
                cloudError.setCode(osr.getError().getCode());
                cloudError.setMessage(osr.getError().getMessage());
                ex.setError(cloudError);
                throw ex;
            } else {
                throw new ServiceException("");
            }
        }

        return osr;
    }

    @NotNull
    private static CloudService loadDeployment(@NotNull DeploymentGetResponse deployment,
                                               @NotNull CloudService cloudService)
            throws Exception {
        if (deployment.getDeploymentSlot() != null) {
            CloudService.Deployment dep;

            switch (deployment.getDeploymentSlot()) {
                case Production:
                    dep = cloudService.getProductionDeployment();
                    break;
                case Staging:
                    dep = cloudService.getStagingDeployment();
                    break;
                default:
                    return cloudService;
            }

            dep.setName(deployment.getName() != null ? deployment.getName() : "");
            dep.setVirtualNetwork(deployment.getVirtualNetworkName() != null ? deployment.getVirtualNetworkName() : "");

            if (deployment.getRoles() != null) {
                Set<String> virtualMachines = dep.getVirtualMachines();
                Set<String> computeRoles = dep.getComputeRoles();
                Set<String> availabilitySets = dep.getAvailabilitySets();

                for (Role role : deployment.getRoles()) {
                    if (role.getRoleType() != null && role.getRoleType().equals(PERSISTENT_VM_ROLE)) {
                        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                            virtualMachines.add(role.getRoleName());
                        }

                        if (role.getAvailabilitySetName() != null && !role.getAvailabilitySetName().isEmpty()) {
                            availabilitySets.add(role.getAvailabilitySetName());
                        }
                    } else {
                        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                            computeRoles.add(role.getRoleName());
                        }
                    }
                }
            }
        }

        return cloudService;
    }

    private static void deleteVMRole(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                     @NotNull String deploymentName, @NotNull String virtualMachineName,
                                     boolean deleteFromStorage)
            throws Exception {
        VirtualMachineOperations vmo = getVirtualMachineOperations(client);

        OperationStatusResponse osr = vmo.delete(serviceName, deploymentName, virtualMachineName, deleteFromStorage);

        validateOperationStatus(osr);
    }

    private static OperationStatusResponse deleteDeployment(@NotNull ComputeManagementClient client, @NotNull String serviceName,
                                         @NotNull String deploymentName, boolean deleteFromStorage)
            throws Exception {
        DeploymentOperations dop = getDeploymentOperations(client);

        OperationStatusResponse osr = dop.deleteByName(serviceName, deploymentName, deleteFromStorage);

        validateOperationStatus(osr);
        return osr;
    }

    @NotNull
    public static SDKRequestCallback<DeploymentGetResponse, ComputeManagementClient> getDeploymentBySlot(@NotNull final String serviceName, @NotNull final DeploymentSlot deploymentSlot) {
    	return new SDKRequestCallback<DeploymentGetResponse, ComputeManagementClient>() {
    		@NotNull
    		@Override
    		public DeploymentGetResponse execute(@NotNull ComputeManagementClient client)
    				throws Throwable {
    			DeploymentGetResponse deployment = getDeployment(client, serviceName, deploymentSlot);
    			return deployment;
    		}
    	};
    }

    @NotNull
    private static List<Location> loadLocations(@NotNull ManagementClient client,
                                                @NotNull List<Location> locationList)
            throws Exception {
        LocationsListResponse llr = getLocationsOperations(client).list();

        if (llr == null) {
            throw new Exception("Unable to retrieve Locations information");
        }

        if (llr.getLocations() != null) {
            for (LocationsListResponse.Location location : llr.getLocations()) {
                locationList.add(
                        new Location(
                                location.getName() != null ? location.getName() : "",
                                location.getDisplayName() != null ? location.getDisplayName() : ""
                        ));
            }
        }

        return locationList;
    }

    @NotNull
    private static List<AffinityGroup> loadAffinityGroups(@NotNull ManagementClient client,
                                                          @NotNull List<AffinityGroup> affinityGroupList)
            throws Exception {
        AffinityGroupListResponse aglr = getAffinityGroupOperations(client).list();

        if (aglr == null) {
            throw new Exception("Unable to retrieve Affinity Groups information");
        }

        if (aglr.getAffinityGroups() != null) {
            for (AffinityGroupListResponse.AffinityGroup ag : aglr.getAffinityGroups()) {
                affinityGroupList.add(
                        new AffinityGroup(
                                ag.getName() != null ? ag.getName() : "",
                                ag.getLabel() != null ? ag.getLabel() : "",
                                ag.getLocation() != null ? ag.getLocation() : ""
                        ));
            }
        }

        return affinityGroupList;
    }

    @NotNull
    private static String bytesToHex(@NotNull byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

//    @Nullable
//    private static Configuration getConfigurationFromCertificate(@NotNull String subscriptionId,
//                                                                 @NotNull String serviceManagementUrl)
//            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
//            ParserConfigurationException, XPathExpressionException, SAXException, AzureCmdException {
//        return loadConfiguration(subscriptionId, serviceManagementUrl);
//
////        String keyStorePath = File.createTempFile("azk", null).getPath();
////
////        initKeyStore(
////                managementCertificate,
////                OpenSSLHelper.PASSWORD,
////                keyStorePath,
////                OpenSSLHelper.PASSWORD);
////
////        ClassLoader old = Thread.currentThread().getContextClassLoader();
////        Thread.currentThread().setContextClassLoader(AzureManager.class.getClassLoader());
////
////        try {
////            return ManagementConfiguration.configure(URI.create(serviceManagementUrl), subscriptionId, keyStorePath,
////                    OpenSSLHelper.PASSWORD, KeyStoreType.pkcs12);
////        } finally {
////            Thread.currentThread().setContextClassLoader(old);
////        }
//    }


    @Nullable
    private static Configuration getConfigurationFromKeystore(@NotNull String subscriptionId,
                                                              @NotNull String serviceManagementUrl)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            ParserConfigurationException, XPathExpressionException, SAXException, AzureCmdException {
        return loadConfiguration(subscriptionId, serviceManagementUrl);

//        String keyStorePath = File.createTempFile("azk", null).getPath();
//
//        initKeyStore(
//                managementCertificate,
//                OpenSSLHelper.PASSWORD,
//                keyStorePath,
//                OpenSSLHelper.PASSWORD);
//
//        ClassLoader old = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(AzureManager.class.getClassLoader());
//
//        try {
//            return ManagementConfiguration.configure(URI.create(serviceManagementUrl), subscriptionId, keyStorePath,
//                    OpenSSLHelper.PASSWORD, KeyStoreType.pkcs12);
//        } finally {
//            Thread.currentThread().setContextClassLoader(old);
//        }
    }

//    private static void initKeyStore(@NotNull String base64Certificate, @NotNull String certificatePwd,
//                                     @NotNull String keyStorePath, @NotNull String keyStorePwd)
//            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
//        FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStorePath);
//
//        try {
//            KeyStore store = KeyStore.getInstance("PKCS12");
//            store.load(null, null);
//
//            final byte[] decode = Base64.decode(base64Certificate);
//            InputStream sslInputStream = new ByteArrayInputStream(decode);
//            store.load(sslInputStream, certificatePwd.toCharArray());
//
//            // we need to a create a physical key store as well here
//            store.store(keyStoreOutputStream, keyStorePwd.toCharArray());
//        } finally {
//            keyStoreOutputStream.close();
//        }
//    }

    public static Configuration getConfiguration(File file, String subscriptionId) throws IOException {
    	// Get current context class loader
    	ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    	try {
    		// change classloader only for intellij plugin - for some reason Eclipse does not need it
    		if (IS_INTELLIJ_PLUGIN) {
    			// Change context classloader to class context loader
    			Thread.currentThread().setContextClassLoader(AzureManager.class.getClassLoader());
    		}
    		Configuration configuration = PublishSettingsLoader.createManagementConfiguration(file.getPath(), subscriptionId);
    		return configuration;
    	} finally {
    		// Call Azure API and reset back the context loader
    		Thread.currentThread().setContextClassLoader(contextLoader);
    	}
    }

    public static Configuration loadConfiguration(String subscriptionId, String url) throws IOException {
        String keystore = System.getProperty("user.home") + File.separator + ".azure" + File.separator + subscriptionId + ".out";
        URI mngUri = URI.create(url);
        // Get current context class loader
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        try {
            // change classloader only for intellij plugin - for some reason Eclipse does not need it
            if (IS_INTELLIJ_PLUGIN) {
                // Change context classloader to class context loader
                Thread.currentThread().setContextClassLoader(AzureManager.class.getClassLoader());
            }
            Configuration configuration = ManagementConfiguration.configure(null, Configuration.load(), mngUri, subscriptionId, keystore, "", KeyStoreType.pkcs12);
            return configuration;
        } finally {
            // Call Azure API and reset back the context loader
            Thread.currentThread().setContextClassLoader(contextLoader);
        }
    }

    public static void setIsIntellijPlugin(boolean isIntellijPlugin) {
        IS_INTELLIJ_PLUGIN = isIntellijPlugin;
    }
}