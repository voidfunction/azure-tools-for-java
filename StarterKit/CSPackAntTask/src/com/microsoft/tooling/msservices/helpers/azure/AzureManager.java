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
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import com.microsoft.tooling.msservices.components.AppSettingsNames;
import com.microsoft.tooling.msservices.helpers.OpenSSLHelper;
import com.microsoft.tooling.msservices.helpers.StringHelper;
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
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.XmlHelper;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.SDKRequestCallback;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.*;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
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
import sun.misc.BASE64Decoder;

import java.util.logging.Logger;

public abstract class AzureManager {
    public static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";
    Logger logger = Logger.getLogger(AzureManager.class.getName());
    Map<String, Subscription> subscriptions = new HashMap<String, Subscription>();
    Map<String, ReentrantReadWriteLock> lockBySubscriptionId = new HashMap<String, ReentrantReadWriteLock>();
    Map<String, SSLSocketFactory> sslSocketFactoryBySubscriptionId;
    ReentrantReadWriteLock subscriptionMapLock = new ReentrantReadWriteLock(false);
    Map<UserInfo, ReentrantReadWriteLock> lockByUser;
    ReentrantReadWriteLock authDataLock = new ReentrantReadWriteLock(false);
    Object projectObject;

    protected static Map<Object, AzureManager> instances = new HashMap<>();

    @NotNull
    protected Subscription getSubscription(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                return subscriptions.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    ReentrantReadWriteLock getSubscriptionLock(@NotNull String subscriptionId, boolean createOnMissing)
            throws AzureCmdException {
        Lock lock = createOnMissing ? subscriptionMapLock.writeLock() : subscriptionMapLock.readLock();
        lock.lock();

        try {
            if (!lockBySubscriptionId.containsKey(subscriptionId)) {
                if (createOnMissing) {
                    lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
                } else {
                    throw new AzureCmdException("No authentication information for the specified Subscription Id");
                }
            }

            return lockBySubscriptionId.get(subscriptionId);
        } finally {
            lock.unlock();
        }
    }

    protected abstract void loadSubscriptions();

    protected void loadSSLSocketFactory() {
        sslSocketFactoryBySubscriptionId = new HashMap<String, SSLSocketFactory>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();
            String managementCertificate = subscription.getManagementCertificate();

            if (!StringHelper.isNullOrWhiteSpace(managementCertificate)) {
                try {
                    SSLSocketFactory sslSocketFactory = initSSLSocketFactory(managementCertificate);
                    sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
                } catch (Exception e) {
                    subscription.setManagementCertificate(null);
                }
            }
        }
    }

    protected void removeUnusedSubscriptions() {
        List<String> invalidSubscriptionIds = new ArrayList<String>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();

            if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
                subscription.setManagementCertificate(null);
                subscription.setServiceManagementUrl(null);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            lockBySubscriptionId.remove(invalidSubscriptionId);
            subscriptions.remove(invalidSubscriptionId);
        }
    }

    protected abstract void storeSubscriptions();

    protected SSLSocketFactory initSSLSocketFactory(@NotNull String managementCertificate)
            throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        byte[] decodeBuffer = new BASE64Decoder().decodeBuffer(managementCertificate);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

        InputStream is = new ByteArrayInputStream(decodeBuffer);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, OpenSSLHelper.PASSWORD.toCharArray());
        keyManagerFactory.init(ks, OpenSSLHelper.PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return sslContext.getSocketFactory();
    }

    private interface AzureSDKClientProvider<V extends Closeable> {
        @NotNull
        V getSSLClient(@NotNull Subscription subscription)
                throws Throwable;
    }

    protected AzureManager(Object projectObject) {
        this.projectObject = projectObject;
        authDataLock.writeLock().lock();

        try {
            loadSubscriptions();
            loadSSLSocketFactory(); // todo????

            removeUnusedSubscriptions();

            storeSubscriptions();

            lockByUser = new HashMap<UserInfo, ReentrantReadWriteLock>();
        } catch (Exception e) {
            // TODO.shch: handle the exception
            logger.warning(e.getMessage());
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    public static synchronized AzureManager getManager() {
        return getManager(DEFAULT_PROJECT);
    }

    @NotNull
    public static synchronized AzureManager getManager(Object currentProject) {
        if (currentProject == null) {
            currentProject = DEFAULT_PROJECT;
        }
        if (instances.get(currentProject) == null) {
            AzureManager instance = new AzureManager(currentProject) {
                @Override
                protected void loadSubscriptions() {
                }
                @Override
                protected void storeSubscriptions() {
                }
                @Override
                public void executeOnPooledThread(@NotNull Runnable runnable) {
                }
            };
            instances.put(currentProject, instance);
        }
        return instances.get(currentProject);
    }

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

    public boolean usingCertificate() {
        authDataLock.readLock().lock();

        try {
            return sslSocketFactoryBySubscriptionId.size() > 0;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    public boolean usingCertificate(@NotNull String subscriptionId) {
        return hasSSLSocketFactory(subscriptionId);
    }

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

    @NotNull
    public List<CloudService> getCloudServices(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCloudServices(subscriptionId));
    }

    @NotNull
    public List<StorageAccount> getStorageAccounts(@NotNull String subscriptionId, boolean detailed)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.getStorageAccounts(subscriptionId, detailed));
    }

    @NotNull
    public Boolean checkStorageNameAvailability(@NotNull final String subscriptionId, final String storageAccountName)
            throws AzureCmdException {
        return requestStorageSDK(subscriptionId, AzureSDKHelper.checkStorageNameAvailability(storageAccountName));
    }

    @NotNull
    public List<Location> getLocations(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getLocations());
    }
    
    @NotNull
    public SubscriptionGetResponse getSubscription(@NotNull Configuration config) throws AzureCmdException {
    	return AzureSDKHelper.getSubscription(config);
    }

    @NotNull
    public List<AffinityGroup> getAffinityGroups(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestManagementSDK(subscriptionId, AzureSDKHelper.getAffinityGroups());
    }

    @NotNull
    public List<VirtualNetwork> getVirtualNetworks(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestNetworkSDK(subscriptionId, AzureSDKHelper.getVirtualNetworks(subscriptionId));
    }

    public OperationStatusResponse createStorageAccount(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.createStorageAccount(storageAccount));
    }

    public void createCloudService(@NotNull CloudService cloudService)
            throws AzureCmdException {
        requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.createCloudService(cloudService));
    }

    public CloudService getCloudServiceDetailed(@NotNull CloudService cloudService) throws AzureCmdException {
        return requestComputeSDK(cloudService.getSubscriptionId(), AzureSDKHelper.getCloudServiceDetailed(cloudService.getSubscriptionId(), cloudService.getName()));
    }

    @NotNull
    public Boolean checkHostedServiceNameAvailability(@NotNull final String subscriptionId, final String hostedServiceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.checkHostedServiceNameAvailability(hostedServiceName));
    }

    public OperationStatusResponse createDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String slotName, @NotNull DeploymentCreateParameters parameters,
                                                    @NotNull String unpublish)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createDeployment(serviceName, slotName, parameters, unpublish));
    }

    public OperationStatusResponse deleteDeployment(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull String deploymentName, boolean deleteFromStorage)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.deleteDeployment(serviceName, deploymentName, deleteFromStorage));
    }

    public DeploymentGetResponse getDeploymentBySlot(@NotNull String subscriptionId, @NotNull String serviceName, @NotNull DeploymentSlot deploymentSlot)
    		throws AzureCmdException {
    	return requestComputeSDK(subscriptionId, AzureSDKHelper.getDeploymentBySlot(serviceName, deploymentSlot));
    }

    public OperationStatusResponse waitForStatus(@NotNull String subscriptionId, @NotNull OperationStatusResponse operationStatusResponse)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.waitForStatus(operationStatusResponse));
    }

    @NotNull
    public StorageAccount refreshStorageAccountInformation(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        return requestStorageSDK(storageAccount.getSubscriptionId(),
                AzureSDKHelper.refreshStorageAccountInformation(storageAccount));
    }

    public String createServiceCertificate(@NotNull String subscriptionId, @NotNull String serviceName,
                                           @NotNull byte[] data, @NotNull String password, boolean needThumbprint)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.createServiceCertificate(serviceName, data, password, needThumbprint));
    }

    public List<ServiceCertificateListResponse.Certificate> getCertificates(@NotNull String subscriptionId, @NotNull String serviceName)
            throws AzureCmdException {
        return requestComputeSDK(subscriptionId, AzureSDKHelper.getCertificates(serviceName));
    }

    public void deleteStorageAccount(@NotNull ClientStorageAccount storageAccount)
            throws AzureCmdException {
        requestStorageSDK(storageAccount.getSubscriptionId(), AzureSDKHelper.deleteStorageAccount(storageAccount));
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
    	} else {
    		// remove the empty directory
    		ftpClient.removeDirectory(dirToList);
    	}
    	ftpClient.removeDirectory(dirToList);
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

//    private void updateSubscription(@NotNull Subscription subscription, @NotNull UserInfo userInfo)
//            throws AzureCmdException {
//        authDataLock.readLock().lock();
//
//        try {
//            String subscriptionId = subscription.getId();
//            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
//            subscriptionLock.writeLock().lock();
//
//            try {
//                if (subscriptions.containsKey(subscriptionId)) {
//                    subscriptions.get(subscriptionId).setTenantId(subscription.getTenantId());
//                } else {
//                    subscriptions.put(subscriptionId, subscription);
//                }
//
//                setUserInfo(subscriptionId, userInfo);
//                storeSubscriptions();
//            } finally {
//                subscriptionLock.writeLock().unlock();
//            }
//        } finally {
//            authDataLock.readLock().unlock();
//        }
//    }

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
        });
    }

    @NotNull
    private <T, V extends Closeable> T requestAzureSDK(@NotNull final String subscriptionId,
                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
                                                       @NotNull final AzureSDKClientProvider<V> clientProvider)
            throws AzureCmdException {
//        if (hasSSLSocketFactory(subscriptionId)) {
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
//        }
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

    public List<Resource> getApplicationInsightsResources(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getApplicationInsightsResources(subscriptionId));
    }

    public List<String> getLocationsForApplicationInsights(@NotNull String subscriptionId) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.getLocationsForApplicationInsights());
    }

    public Resource createApplicationInsightsResource(@NotNull String subscriptionId,
    		@NotNull String resourceGroupName,
    		@NotNull String resourceName,
    		@NotNull String location) throws AzureCmdException {
    	return requestApplicationInsightsSDK(subscriptionId, AzureSDKHelper.createApplicationInsightsResource(subscriptionId,
    			resourceGroupName, resourceName, location));
    }

    public abstract void executeOnPooledThread(@NotNull Runnable runnable);
}