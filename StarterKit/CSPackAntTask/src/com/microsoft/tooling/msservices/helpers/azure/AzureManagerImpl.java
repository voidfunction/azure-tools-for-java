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
package com.microsoft.tooling.msservices.helpers.azure;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.auth.tenants.Tenant;
import com.microsoft.auth.tenants.TenantsClient;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.management.websites.WebSiteManagementClient;
import com.microsoft.azure.management.websites.models.WebHostingPlan;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ArtifactDescriptor;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ProjectDescriptor;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.XmlHelper;
import com.microsoft.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.rest.AzureAADHelper;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager.ContentType;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManagerBaseImpl;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.SDKRequestCallback;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.FTPPublishProfile;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.PublishProfile;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.models.DeploymentCreateParameters;
import com.microsoft.windowsazure.management.compute.models.DeploymentGetResponse;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse;
import com.microsoft.windowsazure.management.models.SubscriptionGetResponse;
import com.microsoft.windowsazure.management.network.NetworkManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoftopentechnologies.azuremanagementutil.rest.SubscriptionTransformer;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class AzureManagerImpl extends AzureManagerBaseImpl implements AzureManager {
    Logger logger = Logger.getLogger(AzureManagerImpl.class.getName());

    private interface AzureSDKClientProvider<V extends Closeable> {
        @NotNull
        V getSSLClient(@NotNull Subscription subscription)
                throws Throwable;

        @NotNull
        V getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                throws Throwable;
    }

    private static Map<Object, AzureManagerImpl> instances = new HashMap<>();

    private String accessToken; // this field to be used from cspack ant task only

    private ReentrantReadWriteLock subscriptionsChangedLock = new ReentrantReadWriteLock(true);

    private AzureManagerImpl(Object projectObject) {
        super(projectObject);
        authDataLock.writeLock().lock();

        try {
            aadManager = new AADManagerImpl();

            loadSubscriptions();
            loadUserInfo();
            loadSSLSocketFactory(); // todo????

            removeInvalidUserInfo();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();

            accessTokenByUser = new HashMap<UserInfo, String>();
            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
            subscriptionsChangedHandles = new HashSet<EventWaitHandleImpl>();
        } catch (Exception e) {
            // TODO.shch: handle the exception
            logger.warning(e.getMessage());
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    private AzureManagerImpl(String accessToken) {
        super(DEFAULT_PROJECT);
        authDataLock.writeLock().lock();

        try {
//            aadManager = AADManagerImpl.getManager();

            this.accessToken = accessToken;
//            loadSubscriptions();
//            loadUserInfo();
//            loadSSLSocketFactory(); // todo????
//
//            removeInvalidUserInfo();
//            removeUnusedSubscriptions();
//
//            storeSubscriptions();
//            storeUserInfo();
//
//            accessTokenByUser = new HashMap<UserInfo, String>();
//            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
//            subscriptionsChangedHandles = new HashSet<EventWaitHandleImpl>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    /**
     * This method for now is supposed to be used from cspack ant task only
     */
    public static synchronized AzureManager initManager(String accessToken) {
        AzureManagerImpl instance = new AzureManagerImpl(accessToken);
        instances.put(DEFAULT_PROJECT, instance);
        return instance;
    }

    /**
     * Because different IntelliJ windows share same static class information, need to associate
     */
    public static synchronized void initAzureManager(Object projectObject) {
        if (instances.get(projectObject) == null) {
            AzureManagerImpl instance = new AzureManagerImpl(projectObject);
            instances.put(projectObject, instance);
        }
    }

    @NotNull
    public static synchronized AzureManager getManager() {
        return getManager(DefaultLoader.getIdeHelper().getCurrentProject());
    }

    @NotNull
    public static synchronized AzureManagerImpl getManager(Object currentProject) {
        if (currentProject == null) {
            currentProject = DEFAULT_PROJECT;
        }
        if (instances.get(currentProject) == null) {
            AzureManagerImpl instance = new AzureManagerImpl(currentProject);
            instances.put(currentProject, instance);
        }
        return instances.get(currentProject);
//        if (instance == null) {
//            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
//            instance = new AzureManagerImpl();
//        }
//
//        return instance;
    }

    // this method is called when "Sign in" dialog button is clicked

    @Override
    public void authenticate() throws AzureCmdException {
        final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        final String managementUri = settings.getAzureServiceManagementUri();

        // FIXME.shch: need to extend interface?
        com.microsoft.auth.AuthenticationResult res = ((AADManagerImpl)aadManager).auth(null, com.microsoft.auth.PromptBehavior.Always);

        try {
            List<Tenant> tenants = TenantsClient.getByToken(res.getAccessToken());
            for (Tenant t : tenants) {
                String tid = t.getTenantId();

                // FIXME.shch: fast fix to ignore self-made AAD tenants
                res = null;
                try {
                    res = ((AADManagerImpl)aadManager).auth(tid, com.microsoft.auth.PromptBehavior.Auto);
                } catch (Exception e) {
                    logger.warning(String.format("TenantId '%s' auth error: %s", t, e.getMessage()));
                }
                if(res == null) continue;;

                UserInfo userInfo = new UserInfo(tid, res.getUserInfo().getUniqueId());

                List<Subscription> legacySubscriptions = getLegacySubscriptions(managementUri, userInfo);

                List<com.microsoft.auth.subsriptions.Subscription> subscriptions = com.microsoft.auth.subsriptions.SubscriptionsClient.getByToken(res.getAccessToken());
                for (com.microsoft.auth.subsriptions.Subscription s : subscriptions) {
                    Subscription sub = new Subscription();
                    sub.setId(s.getSubscriptionId());
                    sub.setName(s.getDisplayName());
                    sub.setTenantId(tid);
                    sub.setServiceManagementUrl(managementUri);
                    sub.setSelected(true);
                    for (Subscription subscription : legacySubscriptions) {
                        if (s.getSubscriptionId().equals(subscription.getId())) {
                            sub.setMaxHostedServices(subscription.getMaxHostedServices());
                            sub.setMaxStorageAccounts(subscription.getMaxStorageAccounts());
                        }
                    }
                    updateSubscription(sub, userInfo);
                }
                setUserInfo(userInfo);
            }
        } catch (Exception ex) {
            throw new AzureCmdException("Error loading tenants", ex);
        }
    }

    private List<Subscription> getLegacySubscriptions(final String managementUri, final UserInfo userInfo) throws AzureCmdException {
        return requestWithToken(userInfo, new RequestCallback<List<Subscription>>() {
            @Override
            public List<Subscription> execute()
                    throws Throwable {
                String accessToken = getAccessToken(userInfo);
                String subscriptionsXML = AzureAADHelper.executeRequest(managementUri,
                        "subscriptions",
                        ContentType.Json,
                        "GET",
                        null,
                        accessToken,
                        new RestServiceManagerBaseImpl() {
                            @NotNull
                            @Override
                            public String executePollRequest(@NotNull String managementUrl,
                                                             @NotNull String path,
                                                             @NotNull ContentType contentType,
                                                             @NotNull String method,
                                                             @Nullable String postData,
                                                             @NotNull String pollPath,
                                                             @NotNull HttpsURLConnectionProvider sslConnectionProvider)
                                    throws AzureCmdException {
                                throw new UnsupportedOperationException();
                            }
                        });
                return parseSubscriptionsXML(subscriptionsXML);
            }
        });
    }

    @Override
    public boolean authenticated() {
        return getUserInfo() != null;
    }

    @Override
    public boolean authenticated(@NotNull String subscriptionId) {
        return !hasSSLSocketFactory(subscriptionId) && hasUserInfo(subscriptionId);
    }

    @Override
    public void clearAuthentication() {
        try {
            ((AADManagerImpl)aadManager).clearTokenCache();
        } catch (Exception e) {
            // TODO.shch: handle the exception
            logger.warning(e.getMessage());
        }

        setUserInfo(null);
        userInfoBySubscriptionId.clear();
        removeUnusedSubscriptions();

        storeSubscriptions();
    }

    @Override
    public void importPublishSettingsFile(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        List<Subscription> subscriptions = importSubscription(publishSettingsFilePath);

        for (Subscription subscription : subscriptions) {
            try {
                SSLSocketFactory sslSocketFactory = initSSLSocketFactory(subscription.getManagementCertificate());
                updateSubscription(subscription, sslSocketFactory);
            } catch (Exception ex) {
                throw new AzureCmdException("Error importing publish settings", ex);
            }
        }
    }

    @Override
    public boolean usingCertificate() {
        authDataLock.readLock().lock();

        try {
            return sslSocketFactoryBySubscriptionId.size() > 0;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public boolean usingCertificate(@NotNull String subscriptionId) {
        return hasSSLSocketFactory(subscriptionId);
    }

    @Override
    public void clearImportedPublishSettingsFiles() {
        authDataLock.writeLock().lock();

        try {
            sslSocketFactoryBySubscriptionId.clear();
            removeUnusedSubscriptions();
            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getFullSubscriptionList()
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                result.add(subscription);
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getSubscriptionList() {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<Subscription>();

            for (Subscription subscription : subscriptions.values()) {
                if (subscription.isSelected()) {
                    result.add(subscription);
                }
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws AzureCmdException {
        authDataLock.writeLock().lock();

        try {
            for (String subscriptionId : subscriptions.keySet()) {
                Subscription subscription = subscriptions.get(subscriptionId);
                subscription.setSelected(selectedList.contains(subscriptionId));
            }

            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }

        notifySubscriptionsChanged();
    }

    @NotNull
    @Override
    public EventWaitHandle registerSubscriptionsChanged()
            throws AzureCmdException {
        subscriptionsChangedLock.writeLock().lock();

        try {
            EventWaitHandleImpl handle = new EventWaitHandleImpl();

            subscriptionsChangedHandles.add(handle);

            return handle;
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterSubscriptionsChanged(@NotNull EventWaitHandle handle)
            throws AzureCmdException {
        if (!(handle instanceof EventWaitHandleImpl)) {
            throw new AzureCmdException("Invalid handle instance");
        }

        subscriptionsChangedLock.writeLock().lock();

        try {
            subscriptionsChangedHandles.remove(handle);
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }

        ((EventWaitHandleImpl) handle).signalEvent();
    }

    @NotNull
    @Override
    public List<CloudService> getCloudServices(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCloudServices(subscriptionId));
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachines(subscriptionId));
    }

    @NotNull
    @Override
    public VirtualMachine refreshVirtualMachineInformation(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.refreshVirtualMachineInformation(vm));
    }

    @Override
    public void startVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.startVirtualMachine(vm));
    }

    @Override
    public void shutdownVirtualMachine(@NotNull VirtualMachine vm, boolean deallocate)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.shutdownVirtualMachine(vm, deallocate));
    }

    @Override
    public void restartVirtualMachine(@NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.restartVirtualMachine(vm));
    }

    @Override
    public void deleteVirtualMachine(@NotNull VirtualMachine vm, boolean deleteFromStorage)
            throws AzureCmdException {
        requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.deleteVirtualMachine(vm, deleteFromStorage));
    }

    @NotNull
    @Override
    public byte[] downloadRDP(@NotNull VirtualMachine vm) throws AzureCmdException {
        return requestComputeSDK(vm.getSubscriptionId(), AzureSDKHelper.downloadRDP(vm));
    }

    @NotNull
    @Override
    public List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId, boolean detailed)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.getStorageAccounts(subscriptionId, detailed));
    }

    @NotNull
    @Override
    public Boolean checkStorageNameAvailability(@NotNull final String subscriptionId, final String storageAccountName)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.checkStorageNameAvailability(storageAccountName));
    }

    @NotNull
    @Override
    public List<VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getVirtualMachineImages());
    }

    @NotNull
    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getVirtualMachineSizes());
    }

    @NotNull
    @Override
    public List<Location> getLocations(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getLocations());
    }
    
    @NotNull
    @Override
    public SubscriptionGetResponse getSubscription(@NotNull Configuration config) throws AzureCmdException {
    	return AzureSDKHelper.getSubscription(config);
    }

    @NotNull
    @Override
    public List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getAffinityGroups());
    }

    @NotNull
    @Override
    public List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestNetworkSDK(subscriptionId, AzureSDKHelper.getVirtualNetworks(subscriptionId));
    }

    @Override
    public OperationStatusResponse createStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.createStorageAccount(storageAccount));
    }

    @Override
    public void createCloudService(@NotNull CloudService cloudService)
            throws AzureCmdException {
        requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.createCloudService(cloudService));
    }

    @Override
    public CloudService getCloudServiceDetailed(@NotNull CloudService cloudService) throws AzureCmdException {
        return requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.getCloudServiceDetailed(cloudService.getSubscriptionId(), cloudService.getName()));
    }

    @NotNull
    @Override
    public Boolean checkHostedServiceNameAvailability(@NotNull final String subscriptionId, final String hostedServiceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.checkHostedServiceNameAvailability(hostedServiceName));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull StorageAccount storageAccount, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, storageAccount, virtualNetwork, username, password, certificate));
    }

    @Override
    public void createVirtualMachine(@NotNull VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull String mediaLocation, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        requestComputeSDK(virtualMachine.getSubscriptionId(), AzureSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, mediaLocation, virtualNetwork, username, password, certificate));
    }

    @Override
    public OperationStatusResponse createDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String slotName, @NotNull DeploymentCreateParameters parameters,
                                                    @NotNull String unpublish)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createDeployment(serviceName, slotName, parameters, unpublish));
    }

    @Override
    public OperationStatusResponse deleteDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String deploymentName, boolean deleteFromStorage)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.deleteDeployment(serviceName, deploymentName, deleteFromStorage));
    }

    public DeploymentGetResponse getDeploymentBySlot(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull DeploymentSlot deploymentSlot)
    		throws AzureCmdException {
    	return requestComputeSDK(subscriptionId, AzureSDKHelper.getDeploymentBySlot(serviceName, deploymentSlot));
    }

    @Override
    public OperationStatusResponse waitForStatus(@NotNull String subscriptionId, @NotNull OperationStatusResponse operationStatusResponse)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.waitForStatus(operationStatusResponse));
    }

    @NotNull
    @Override
    public StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(),
                AzureSDKHelper.refreshStorageAccountInformation(storageAccount));
    }

    @Override
    public String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                           @NotNull byte[] data, @NotNull String password, boolean needThumbprint)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createServiceCertificate(serviceName, data, password, needThumbprint));
    }

    @Override
    public List<ServiceCertificateListResponse.Certificate> getCertificates(@NotNull String subscriptionId, @NotNull String serviceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCertificates(serviceName));
    }


    @Override
    public void deleteStorageAccount(@NotNull ClientStorageAccount storageAccount)
            throws AzureCmdException {
        requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.deleteStorageAccount(storageAccount));
    }
    
    @Override
    public ResourceGroupExtended createResourceGroup(@NotNull String subscriptionId, @NotNull String name, @NotNull String location)
    		throws AzureCmdException {
    	return requestResourceManagementSDK(subscriptionId, AzureSDKHelper.createResourceGroup(name, location));
    }
    
    @Override
    public List<String> getResourceGroupNames(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestResourceManagementSDK(subscriptionId, AzureSDKHelper.getResourceGroupNames());
    }

    @NotNull
    @Override
    public List<WebSite> getWebSites(@NotNull String subscriptionId, @NotNull String webSpaceName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSites(webSpaceName));
    }

    @NotNull
    @Override
    public List<WebHostingPlanCache> getWebHostingPlans(@NotNull String subscriptionId, @NotNull String webSpaceName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebHostingPlans(webSpaceName));
    }

    @NotNull
    @Override
    public WebSiteConfiguration getWebSiteConfiguration(@NotNull String subscriptionId, @NotNull String webSpaceName,
                                                        @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSiteConfiguration(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSitePublishSettings getWebSitePublishSettings(@NotNull String subscriptionId, @NotNull String webSpaceName,
                                                            @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSitePublishSettings(webSpaceName, webSiteName));
    }

    @Override
    public void restartWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.restartWebSite(webSpaceName, webSiteName));
    }

    @Override
    public void stopWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.stopWebSite(webSpaceName, webSiteName));
    }

    @Override
    public void startWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        requestWebSiteSDK(subscriptionId, AzureSDKHelper.startWebSite(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSite createWebSite(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan, @NotNull String webSiteName)
    		throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.createWebSite(webHostingPlan, webSiteName));
    }

    @NotNull
    @Override
	public Void deleteWebSite(@NotNull String subscriptionId, @NotNull String webSpaceName, @NotNull String webSiteName) throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.deleteWebSite(webSpaceName, webSiteName));
    }

    @Override
    public WebSite getWebSite(@NotNull String subscriptionId, @NotNull final String webSpaceName, @NotNull String webSiteName)
            throws AzureCmdException {
        return requestWebSiteSDK(subscriptionId, AzureSDKHelper.getWebSite(webSpaceName, webSiteName));
    }

    @NotNull
    @Override
    public WebSiteConfiguration updateWebSiteConfiguration(@NotNull String subscriptionId,
    		@NotNull String webSpaceName,
    		@NotNull String webSiteName,
    		@NotNull String location,
    		@NotNull WebSiteConfiguration webSiteConfiguration) throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.updateWebSiteConfiguration(webSpaceName, webSiteName, location, webSiteConfiguration));
    }

    @NotNull
    @Override
    public WebHostingPlan createWebHostingPlan(@NotNull String subscriptionId, @NotNull WebHostingPlanCache webHostingPlan)
    		throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.createWebHostingPlan(webHostingPlan));
    }

    @Nullable
    @Override
    public ArtifactDescriptor getWebArchiveArtifact(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException {
        ArtifactDescriptor artifactDescriptor = null;

        for (ArtifactDescriptor descriptor : DefaultLoader.getIdeHelper().getArtifacts(projectDescriptor)) {
            if ("war".equals(descriptor.getArtifactType())) {
                artifactDescriptor = descriptor;
                break;
            }
        }

        return artifactDescriptor;
    }

    @Override
    public void deployWebArchiveArtifact(@NotNull final ProjectDescriptor projectDescriptor,
    		@NotNull final ArtifactDescriptor artifactDescriptor,
    		@NotNull final WebSite webSite,
    		@NotNull final boolean isDeployRoot,
    		final AzureManager manager) {
    	ListenableFuture<String> future = DefaultLoader.getIdeHelper().buildArtifact(projectDescriptor, artifactDescriptor);

    	Futures.addCallback(future, new FutureCallback<String>() {
    		@Override
    		public void onSuccess(final String artifactPath) {
    			try {
    				DefaultLoader.getIdeHelper().runInBackground(projectDescriptor, "Deploying web app", "Deploying web app...", new CancellableTask() {
    					@Override
    					public void run(CancellationHandle cancellationHandle) throws Throwable {
    						manager.publishWebArchiveArtifact(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName(),
    								artifactPath, isDeployRoot, artifactDescriptor.getName());
    					}

    					@Override
    					public void onCancel() {
    					}

    					@Override
    					public void onSuccess() {
    					}

    					@Override
    					public void onError(@NotNull Throwable throwable) {
    						DefaultLoader.getUIHelper().showException("An error occurred while attempting to deploy web app.",
    								throwable, "MS Services - Error Deploying Web App", false, true);
    					}
    				});
    			} catch (AzureCmdException ex) {
    				String msg = "An error occurred while attempting to deploy web app." + "\n" + "(Message from Azure:" + ex.getMessage() + ")";
    				DefaultLoader.getUIHelper().showException(msg,
    						ex, "MS Services - Error Deploying Web App", false, true);
    			}
    		}

    		@Override
    		public void onFailure(Throwable throwable) {
    			DefaultLoader.getUIHelper().showException("An error occurred while attempting to build web archive artifact.", throwable,
    					"MS Services - Error Building WAR Artifact", false, true);
    		}
    	});
    }

    @Override
    public void publishWebArchiveArtifact(@NotNull String subscriptionId, @NotNull String webSpaceName,
    		@NotNull String webSiteName, @NotNull String artifactPath,
    		@NotNull boolean isDeployRoot, @NotNull String artifactName) throws AzureCmdException {
    	WebSitePublishSettings webSitePublishSettings = getWebSitePublishSettings(subscriptionId, webSpaceName, webSiteName);
    	WebSitePublishSettings.FTPPublishProfile publishProfile = null;
    	for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
    		if (pp instanceof FTPPublishProfile) {
    			publishProfile = (FTPPublishProfile) pp;
    			break;
    		}
    	}

    	if (publishProfile == null) {
    		throw new AzureCmdException("Unable to retrieve FTP credentials to publish web site");
    	}

    	URI uri;

    	try {
    		uri = new URI(publishProfile.getPublishUrl());
    	} catch (URISyntaxException e) {
    		throw new AzureCmdException("Unable to parse FTP Publish Url information", e);
    	}

    	final FTPClient ftp = new FTPClient();

    	try {
    		ftp.connect(uri.getHost());
    		final int replyCode = ftp.getReplyCode();

    		if (!FTPReply.isPositiveCompletion(replyCode)) {
    			ftp.disconnect();
    			throw new AzureCmdException("Unable to connect to FTP server");
    		}

    		if (!ftp.login(publishProfile.getUserName(), publishProfile.getPassword())) {
    			ftp.logout();
    			throw new AzureCmdException("Unable to login to FTP server");
    		}

    		ftp.setFileType(FTP.BINARY_FILE_TYPE);

    		if (publishProfile.isFtpPassiveMode()) {
    			ftp.enterLocalPassiveMode();
    		}

    		String targetDir = getAbsolutePath(uri.getPath());
    		targetDir += "/webapps";

    		InputStream input = new FileInputStream(artifactPath);
    		if (isDeployRoot) {
    			removeFtpDirectory(ftp, "/site/wwwroot/webapps/ROOT", "");
    			ftp.storeFile(targetDir + "/ROOT.war", input);
    		} else {
    			artifactName = artifactName.replaceAll("[^a-zA-Z0-9_-]+","");
    			removeFtpDirectory(ftp, "/site/wwwroot/webapps/" + artifactName, "");
    			ftp.storeFile(targetDir + "/" + artifactName + ".war", input);
    		}
    		input.close();
    		ftp.logout();
    	} catch (IOException e) {
    		throw new AzureCmdException("Unable to connect to the FTP server", e);
    	} finally {
    		if (ftp.isConnected()) {
    			try {
    				ftp.disconnect();
    			} catch (IOException ignored) {
    			}
    		}
    	}
    }

    public static void removeFtpDirectory(FTPClient ftpClient, String parentDir,
    		String currentDir) throws IOException {
    	String dirToList = parentDir;
    	if (!currentDir.equals("")) {
    		dirToList += "/" + currentDir;
    	}
    	FTPFile[] subFiles = ftpClient.listFiles(dirToList);
    	if (subFiles != null && subFiles.length > 0) {
    		for (FTPFile ftpFile : subFiles) {
    			String currentFileName = ftpFile.getName();
    			if (currentFileName.equals(".") || currentFileName.equals("..")) {
    				// skip parent directory and the directory itself
    				continue;
    			}
    			String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
    			if (currentDir.equals("")) {
    				filePath = parentDir + "/" + currentFileName;
    			}

    			if (ftpFile.isDirectory()) {
    				// remove the sub directory
    				removeFtpDirectory(ftpClient, dirToList, currentFileName);
    			} else {
    				// delete the file
    				ftpClient.deleteFile(filePath);
    			}
    		}
    		// remove the empty directory
    		ftpClient.removeDirectory(dirToList);
    	}
    }

    @NotNull
    private List<Subscription> parseSubscriptionsXML(@NotNull String subscriptionsXML)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList subscriptionList = (NodeList) XmlHelper.getXMLValue(subscriptionsXML, "//Subscription", XPathConstants.NODESET);

        ArrayList<Subscription> subscriptions = new ArrayList<Subscription>();

        for (int i = 0; i < subscriptionList.getLength(); i++) {
            Subscription subscription = new Subscription();
            subscription.setName(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionName"));
            subscription.setId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "SubscriptionID"));
            subscription.setTenantId(XmlHelper.getChildNodeValue(subscriptionList.item(i), "AADTenantID"));
            subscription.setMaxStorageAccounts(Integer.valueOf(XmlHelper.getChildNodeValue(subscriptionList.item(i), "MaxStorageAccounts")));
            subscription.setMaxHostedServices(Integer.valueOf(XmlHelper.getChildNodeValue(subscriptionList.item(i), "MaxHostedServices")));
            subscription.setSelected(true);

            subscriptions.add(subscription);
        }

        return subscriptions;
    }

    private List<Subscription> importSubscription(@NotNull String publishSettingsFilePath)
            throws AzureCmdException {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(publishSettingsFilePath));
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            String publishSettingsFile = sb.toString();
            String managementCertificate = null;
            String serviceManagementUrl = null;
            boolean isPublishSettings2 = true;
            Node publishProfile = (Node) XmlHelper.getXMLValue(publishSettingsFile, "//PublishProfile", XPathConstants.NODE);
            if (XmlHelper.getAttributeValue(publishProfile, "SchemaVersion") == null
                    || !XmlHelper.getAttributeValue(publishProfile, "SchemaVersion").equals("2.0")) {
                isPublishSettings2 = false;
                managementCertificate = XmlHelper.getAttributeValue(publishProfile, "ManagementCertificate");
                serviceManagementUrl = XmlHelper.getAttributeValue(publishProfile, "Url");
            }
            NodeList subscriptionNodes = (NodeList) XmlHelper.getXMLValue(publishSettingsFile, "//Subscription",
                    XPathConstants.NODESET);
            List<Subscription> subscriptions = new ArrayList<Subscription>();
            for (int i = 0; i < subscriptionNodes.getLength(); i++) {
                Node subscriptionNode = subscriptionNodes.item(i);
                Subscription subscription = new Subscription();
                subscription.setName(XmlHelper.getAttributeValue(subscriptionNode, "Name"));
                subscription.setId(XmlHelper.getAttributeValue(subscriptionNode, "Id"));
                if (isPublishSettings2) {
                    subscription.setManagementCertificate(XmlHelper.getAttributeValue(subscriptionNode, "ManagementCertificate"));
                    subscription.setServiceManagementUrl(XmlHelper.getAttributeValue(subscriptionNode, "ServiceManagementUrl"));
                } else {
                    subscription.setManagementCertificate(managementCertificate);
                    subscription.setServiceManagementUrl(serviceManagementUrl);
                }
                subscription.setSelected(true);
                Configuration config = AzureSDKHelper.getConfiguration(new File(publishSettingsFilePath), subscription.getId());
                SubscriptionGetResponse response = getSubscription(config);
                com.microsoftopentechnologies.azuremanagementutil.model.Subscription sub = SubscriptionTransformer.transform(response);
                subscription.setMaxStorageAccounts(sub.getMaxStorageAccounts());
                subscription.setMaxHostedServices(sub.getMaxHostedServices());
                subscriptions.add(subscription);
            }
            return subscriptions;
        } catch (Exception ex) {
            if (ex instanceof AzureCmdException) {
                throw (AzureCmdException) ex;
            }

            throw new AzureCmdException("Error importing subscriptions from publish settings file", ex);
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    subscriptions.get(subscriptionId).setTenantId(subscription.getTenantId());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setUserInfo(subscriptionId, userInfo);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void updateSubscription(@NotNull Subscription subscription, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            String subscriptionId = subscription.getId();
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                if (subscriptions.containsKey(subscriptionId)) {
                    Subscription existingSubscription = subscriptions.get(subscriptionId);
                    existingSubscription.setManagementCertificate(subscription.getManagementCertificate());
                    existingSubscription.setServiceManagementUrl(subscription.getServiceManagementUrl());
                } else {
                    subscriptions.put(subscriptionId, subscription);
                }

                setSSLSocketFactory(subscriptionId, sslSocketFactory);
                storeSubscriptions();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void notifySubscriptionsChanged() {
        subscriptionsChangedLock.readLock().lock();

        try {
            for (EventWaitHandleImpl handle : subscriptionsChangedHandles) {
                handle.signalEvent();
            }
        } finally {
            subscriptionsChangedLock.readLock().unlock();
        }
    }

    private void setUserInfo(@Nullable UserInfo userInfo) {
        authDataLock.writeLock().lock();

        try {
            this.userInfo = userInfo;
//            userInfoBySubscriptionId.clear();
//            removeUnusedSubscriptions();
//
//            storeSubscriptions();
            storeUserInfo();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    private boolean hasUserInfo(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return userInfoBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setUserInfo(@NotNull String subscriptionId, @NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                userInfoBySubscriptionId.put(subscriptionId, userInfo);

                storeUserInfo();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasSSLSocketFactory(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return sslSocketFactoryBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private Optional<SSLSocketFactory> getSSLSocketFactory(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                    return Optional.absent();
                }

                return Optional.of(sslSocketFactoryBySubscriptionId.get(subscriptionId));
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setSSLSocketFactory(@NotNull String subscriptionId, @NotNull SSLSocketFactory sslSocketFactory)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasAccessToken() {
        authDataLock.readLock().lock();

        try {
            return !(accessToken == null || accessToken.isEmpty());
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    private Optional<ReentrantReadWriteLock> getSubscriptionLock(@NotNull String subscriptionId) {
        subscriptionMapLock.readLock().lock();

        try {
            if (lockBySubscriptionId.containsKey(subscriptionId)) {
                return Optional.of(lockBySubscriptionId.get(subscriptionId));
            } else {
                return Optional.absent();
            }
        } finally {
            subscriptionMapLock.readLock().unlock();
        }
    }

    @NotNull
    private <T> T requestComputeSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, ComputeManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ComputeManagementClient>() {
            @NotNull
            @Override
            public ComputeManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ComputeManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getComputeManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestStorageSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, StorageManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<StorageManagementClient>() {
            @NotNull
            @Override
            public StorageManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public StorageManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getStorageManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }

    @NotNull
    private <T> T requestNetworkSDK(@NotNull final String subscriptionId,
    		@NotNull final SDKRequestCallback<T, NetworkManagementClient> requestCallback)
    				throws AzureCmdException {
    	return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<NetworkManagementClient>() {
    		@NotNull
    		@Override
    		public NetworkManagementClient getSSLClient(@NotNull Subscription subscription)
    				throws Throwable {
    			return AzureSDKHelper.getNetworkManagementClient(subscription.getId(),
    					subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
    		}

    		@NotNull
    		@Override
    		public NetworkManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
    				throws Throwable {
    			return AzureSDKHelper.getNetworkManagementClient(subscriptionId,
    					accessToken);
    		}
    	});
    }

    @NotNull
    private <T> T requestWebSiteSDK(@NotNull final String subscriptionId,
    		@NotNull final SDKRequestCallback<T, WebSiteManagementClient> requestCallback)
    				throws AzureCmdException {
    	return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<WebSiteManagementClient>() {
    		@NotNull
    		@Override
    		public WebSiteManagementClient getSSLClient(@NotNull Subscription subscription)
    				throws Throwable {
    			return AzureSDKHelper.getWebSiteManagementClient(subscription.getId(),
    					subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
    		}

    		@NotNull
    		@Override
    		public WebSiteManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
    				throws Throwable {
    			return AzureSDKHelper.getWebSiteManagementClient(subscriptionId,
    					accessToken);
    		}
    	});
    }
    
    @NotNull
    private <T> T requestResourceManagementSDK(@NotNull final String subscriptionId,
                                    @NotNull final SDKRequestCallback<T, ResourceManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ResourceManagementClient>() {
            @NotNull
            @Override
            public ResourceManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getResourceManagementClient(subscriptionId, accessToken);
            }

			@Override
			public ResourceManagementClient getSSLClient(Subscription subscription) throws Throwable {
				return AzureSDKHelper.getResourceManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
			}
        });
    }
    

    @NotNull
    private <T> T requestManagementSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, ManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ManagementClient>() {
            @NotNull
            @Override
            public ManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscription.getId(),
                        subscription.getManagementCertificate(), subscription.getServiceManagementUrl());
            }

            @NotNull
            @Override
            public ManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureSDKHelper.getManagementClient(subscriptionId,
                        accessToken);
            }
        });
    }
    
    @NotNull
    private <T> T requestApplicationInsightsSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, ApplicationInsightsManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKClientProvider<ApplicationInsightsManagementClient>() {
            @NotNull
            @Override
            public ApplicationInsightsManagementClient getSSLClient(@NotNull Subscription subscription)
                    throws Throwable {
            	// Application insights does not support publish settings file as authentication
                return null;
            }

            @NotNull
            @Override
            public ApplicationInsightsManagementClient getAADClient(@NotNull String subscriptionId, @NotNull String accessToken)
            		throws Throwable {
                return AzureSDKHelper.getApplicationManagementClient(getUserInfo(subscriptionId).getTenantId(), accessToken);
            }
        });
    }

    @NotNull
    private <T, V extends Closeable> T requestAzureSDK(@NotNull final String subscriptionId,
                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
                                                       @NotNull final AzureSDKClientProvider<V> clientProvider)
            throws AzureCmdException {
        if (hasSSLSocketFactory(subscriptionId)) {
            try {
                Subscription subscription = getSubscription(subscriptionId);
                V client = clientProvider.getSSLClient(subscription);

                try {
                    return requestCallback.execute(client);
                } finally {
                    client.close();
                }
            } catch (Throwable t) {
                if (t instanceof AzureCmdException) {
                    throw (AzureCmdException) t;
                } else if (t instanceof ExecutionException) {
                    throw new AzureCmdException(t.getCause().getMessage(), t.getCause());
                }

                throw new AzureCmdException(t.getMessage(), t);
            }
        } else if (hasAccessToken()) {
            V client = null;
            try {
                client = clientProvider.getAADClient(subscriptionId, accessToken);
                return requestCallback.execute(client);
            } catch (Throwable throwable) {
                throw new AzureCmdException(throwable.getMessage(), throwable);
            } finally {
                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                    throw new AzureCmdException(e.getMessage(), e);
                }
            }
        } else {
            final UserInfo userInfo = getUserInfo(subscriptionId);
            PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

            com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                    new com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T>() {
                        @NotNull
                        @Override
                        public T execute(@NotNull String accessToken) throws Throwable {
                            if (!hasAccessToken(userInfo) ||
                                    !accessToken.equals(getAccessToken(userInfo))) {
                                ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                                userLock.writeLock().lock();

                                try {
                                    if (!hasAccessToken(userInfo) ||
                                            !accessToken.equals(getAccessToken(userInfo))) {
                                        setAccessToken(userInfo, accessToken);
                                    }
                                } finally {
                                    userLock.writeLock().unlock();
                                }
                            }

                            V client = clientProvider.getAADClient(subscriptionId, accessToken);

                            try {
                                return requestCallback.execute(client);
                            } finally {
                                client.close();
                            }
                        }
                    };

            return aadManager.request(userInfo,
                    settings.getAzureServiceManagementUri(),
                    "Sign in to your Azure account",
                    aadRequestCB);
        }
    }

    @NotNull
    public <T> T requestWithToken(@NotNull final UserInfo userInfo, @NotNull final RequestCallback<T> requestCallback)
            throws AzureCmdException {
        PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();

        com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
                new com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T>() {
                    @NotNull
                    @Override
                    public T execute(@NotNull String accessToken) throws Throwable {
                        if (!hasAccessToken(userInfo) || !accessToken.equals(getAccessToken(userInfo))) {
                            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                            userLock.writeLock().lock();

                            try {
                                if (!hasAccessToken(userInfo) || !accessToken.equals(getAccessToken(userInfo))) {
                                    setAccessToken(userInfo, accessToken);
                                }
                            } finally {
                                userLock.writeLock().unlock();
                            }
                        }

                        return requestCallback.execute();
                    }
                };

        return aadManager.request(userInfo,
                settings.getAzureServiceManagementUri(),
                "Sign in to your Azure account",
                aadRequestCB);
    }

    @NotNull
    private static String readFile(@NotNull String filePath)
            throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

        try {
            return CharStreams.toString(in);
        } finally {
            in.close();
        }
    }

    @NotNull
    private static String getAbsolutePath(@NotNull String dir) {
        return "/" + dir.trim().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }
    
    @Override
    public List<Resource> getApplicationInsightsResources(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getApplicationInsightsResources(subscriptionId));
    }
    
    @Override
    public List<String> getLocationsForApplicationInsights(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getLocationsForApplicationInsights());
    }
    
    @Override
    public Resource createApplicationInsightsResource(@NotNull String subscriptionId,
    		@NotNull String resourceGroupName,
    		@NotNull String resourceName,
    		@NotNull String location) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.createApplicationInsightsResource(subscriptionId,
    			resourceGroupName, resourceName, location));
    }
    
    @NotNull
    @Override
    public Void enableWebSockets(@NotNull String subscriptionId,
    		@NotNull String webSpaceName,
    		@NotNull String webSiteName,
    		@NotNull String location,
    		@NotNull boolean enableSocket) throws AzureCmdException {
    	return requestWebSiteSDK(subscriptionId, AzureSDKHelper.enableWebSockets(webSpaceName, webSiteName, location, enableSocket));
    }
}