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

import com.microsoft.azure.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureArmSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureRequestCallback;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineImage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AzureArmManagerImpl extends AzureManagerBaseImpl {
    private static Map<Object, AzureArmManagerImpl> instances = new HashMap<>();

    public AzureArmManagerImpl(Object projectObject) {
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
            subscriptionsChangedHandles = new HashSet<AzureManagerImpl.EventWaitHandleImpl>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    public static synchronized AzureArmManagerImpl getManager(Object currentProject) {
        if (instances.get(currentProject) == null) {
            AzureArmManagerImpl instance = new AzureArmManagerImpl(currentProject);
            instances.put(currentProject, instance);
        }
        return instances.get(currentProject);
    }

//    private interface AzureSDKArmClientProvider<V> {
//        @NotNull
//        V getClient(@NotNull String subscriptionId, @NotNull String accessToken) throws Throwable;
//    }


//    @NotNull
//    private <T, V> T requestAzureSDK(@NotNull final String subscriptionId,
//                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
//                                                       @NotNull final AzureSDKArmClientProvider<V> clientProvider)
//            throws AzureCmdException {
//
//            final UserInfo userInfo = getUserInfo(subscriptionId);
//            PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
//
//            com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T> aadRequestCB =
//                    new com.microsoft.tooling.msservices.helpers.auth.RequestCallback<T>() {
//                        @NotNull
//                        @Override
//                        public T execute(@NotNull String accessToken) throws Throwable {
//                            if (!hasAccessToken(userInfo) ||
//                                    !accessToken.equals(getAccessToken(userInfo))) {
//                                ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
//                                userLock.writeLock().lock();
//
//                                try {
//                                    if (!hasAccessToken(userInfo) ||
//                                            !accessToken.equals(getAccessToken(userInfo))) {
//                                        setAccessToken(userInfo, accessToken);
//                                    }
//                                } finally {
//                                    userLock.writeLock().unlock();
//                                }
//                            }
//                            V client = clientProvider.getClient(subscriptionId, accessToken);
//
//                            return requestCallback.execute(client);
//                        }
//                    };
//
//            return aadManager.request(userInfo,
//                    settings.getAzureServiceManagementUri(),
//                    "Sign in to your Azure account",
//                    aadRequestCB);
//    }

    @NotNull
    private <T> T requestAzureSDK(@NotNull final String subscriptionId,
                                     @NotNull final AzureRequestCallback<T> requestCallback)
            throws AzureCmdException {

        final UserInfo userInfo = getUserInfo(subscriptionId);
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
                        Azure azure = AzureArmSDKHelper.getAzure(subscriptionId, accessToken);

                        return requestCallback.execute(azure);
                    }
                };

        return aadManager.request(userInfo,
                settings.getAzureServiceManagementUri(),
                "Sign in to your Azure account",
                aadRequestCB);
    }

//    @NotNull
//    private <T> T requestArmComputeSDK(@NotNull final String subscriptionId,
//                                       @NotNull final SDKRequestCallback<T, Azure> requestCallback)
//            throws AzureCmdException {
//        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKArmClientProvider<Azure>() {
//            @NotNull
//            @Override
//            public Azure getClient(@NotNull String subscriptionId, @NotNull String accessToken)
//                    throws Throwable {
//                return AzureArmSDKHelper.getArmComputeManagementClient(subscriptionId, accessToken);
//            }
//        });
//    }

    @NotNull
    public List<ResourceGroup> getResourceGroups(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getResourceGroups());
    }

    @NotNull
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualMachines());
    }

    public void restartVirtualMachine(String subscriptionId, @NotNull VirtualMachine virtualMachine) throws AzureCmdException {
        requestAzureSDK(subscriptionId, AzureArmSDKHelper.restartVirtualMachine(virtualMachine));
    }

    public void shutdownVirtualMachine(String subscriptionId, @NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestAzureSDK(subscriptionId, AzureArmSDKHelper.shutdownVirtualMachine(vm));
    }

    public VirtualMachine createVirtualMachine(@NotNull String subscriptionId, @NotNull com.microsoft.tooling.msservices.model.vm.VirtualMachine virtualMachine, @NotNull VirtualMachineImage vmImage,
                                     @NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount, @NotNull String virtualNetwork,
                                     @NotNull String username, @NotNull String password, @NotNull byte[] certificate)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, storageAccount, virtualNetwork, username, password, certificate));
    }

    public List<com.microsoft.tooling.msservices.model.storage.StorageAccount> getStorageAccounts(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getStorageAccounts(subscriptionId));
    }

    public void deleteStorageAccount(@NotNull String subscriptionId, @NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount) throws AzureCmdException {
        requestAzureSDK(subscriptionId, AzureArmSDKHelper.deleteStorageAccount(storageAccount));
    }

    public void createStorageAccount(@NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount) throws AzureCmdException {
        requestAzureSDK(storageAccount.getSubscriptionId(), AzureArmSDKHelper.createStorageAccount(storageAccount));
    }

    @NotNull
    public List<Network> getVirtualNetworks(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualNetworks());
    }
}
