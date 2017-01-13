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
 *//*

package com.microsoft.tooling.msservices.helpers.azure;

import com.microsoft.azure.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureArmSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureRequestCallback;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    public void deleteVirtualMachine(String subscriptionId, @NotNull VirtualMachine vm)
            throws AzureCmdException {
        requestAzureSDK(subscriptionId, AzureArmSDKHelper.deleteVirtualMachine(vm));
    }

    public VirtualMachine createVirtualMachine(@NotNull String subscriptionId, @NotNull com.microsoft.tooling.msservices.model.vm.VirtualMachine virtualMachine,
                                               @NotNull VirtualMachineImage vmImage,
                                               @NotNull ArmStorageAccount storageAccount,
                                               @NotNull Network network, @NotNull String subnet,
                                               @Nullable PublicIpAddress pip, boolean withNewPip,
                                               @Nullable AvailabilitySet availabilitySet, boolean withNewAvailabilitySet,
                                               @NotNull String username, @Nullable String password, @Nullable String publicKey)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.createVirtualMachine(virtualMachine,
                vmImage, storageAccount, network, subnet, pip, withNewPip, availabilitySet, withNewAvailabilitySet, username, password, publicKey));
    }

    @NotNull
    public List<com.microsoft.azure.management.compute.VirtualMachineImage> getVirtualMachineImages(@NotNull String subscriptionId, @NotNull Region region) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualMachineImages(region));
    }

    public List<VirtualMachinePublisher> getVirtualMachinePublishers(@NotNull String subscriptionId, @NotNull Region region) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualMachinePublishers(region));
    }

    public List<com.microsoft.tooling.msservices.model.vm.VirtualMachineSize> getVirtualMachineSizes(@NotNull String subscriptionId, @NotNull Region region)
            throws AzureCmdException{
        List<VirtualMachineSize> sizes = requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualMachineSizes(region));
        return sizes.stream()
                .map(p1 -> new com.microsoft.tooling.msservices.model.vm.VirtualMachineSize(p1.name(), p1.name(), p1.numberOfCores(), p1.memoryInMB()))
                .collect(Collectors.toList());
    }

    public Network createVirtualNetwork(@NotNull String subscriptionId, @NotNull String networkName, @NotNull Region region,  String addressSpace,
                                        @NotNull String groupName, boolean isNewGroup)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.createVirtualNetwork(networkName, region, addressSpace, groupName, isNewGroup));
    }

    @NotNull
    public List<ArmStorageAccount> getStorageAccounts(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getStorageAccounts(subscriptionId));
    }

    public void deleteStorageAccount(@NotNull String subscriptionId, @NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount) throws AzureCmdException {
        requestAzureSDK(subscriptionId, AzureArmSDKHelper.deleteStorageAccount(storageAccount));
    }

    public ArmStorageAccount createStorageAccount(@NotNull ArmStorageAccount storageAccount) throws AzureCmdException {
        StorageAccount inner = requestAzureSDK(storageAccount.getSubscriptionId(), AzureArmSDKHelper.createStorageAccount(storageAccount));
        storageAccount.setInner(inner);
        return storageAccount;
    }

    @NotNull
    public List<Network> getVirtualNetworks(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getVirtualNetworks());
    }

    @NotNull
    public List<PublicIpAddress> getPublicIpAddresses(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getPublicIpAddresses());
    }

    @NotNull
    public List<NetworkSecurityGroup> getNetworkSecurityGroups(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getNetworkSecurityGroups());
    }

    @NotNull
    public List<AvailabilitySet> getAvailabilitySets(@NotNull String subscriptionId) throws AzureCmdException {
        return requestAzureSDK(subscriptionId, AzureArmSDKHelper.getAvailabilitySets());
    }
}
*/
