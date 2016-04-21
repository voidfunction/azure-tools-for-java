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

import com.google.gson.GsonBuilder;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.auth.AADManagerImpl;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureArmSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.SDKRequestCallback;
import com.microsoft.tooling.msservices.model.Subscription;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AzureArmManagerImpl extends AzureManagerBaseImpl {
    private static AzureArmManagerImpl instance;

    public AzureArmManagerImpl() {
        authDataLock.writeLock().lock();

        try {
            aadManager = AADManagerImpl.getManager();

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
    public static synchronized AzureArmManagerImpl getManager() {
        if (instance == null) {
            instance = new AzureArmManagerImpl();
        }
        return instance;
    }

    private interface AzureSDKArmClientProvider<V> {
        @NotNull
        V getClient(@NotNull String subscriptionId, @NotNull String accessToken) throws Throwable;
    }


    @NotNull
    private <T, V> T requestAzureSDK(@NotNull final String subscriptionId,
                                                       @NotNull final SDKRequestCallback<T, V> requestCallback,
                                                       @NotNull final AzureSDKArmClientProvider<V> clientProvider)
            throws AzureCmdException {

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
                            V client = clientProvider.getClient(subscriptionId, accessToken);

                            return requestCallback.execute(client);
                        }
                    };

            return aadManager.request(userInfo,
                    settings.getAzureServiceManagementUri(),
                    "Sign in to your Azure account",
                    aadRequestCB);
    }

    @NotNull
    private <T> T requestArmComputeSDK(@NotNull final String subscriptionId,
                                       @NotNull final SDKRequestCallback<T, com.microsoft.azure.management.compute.ComputeManagementClient> requestCallback)
            throws AzureCmdException {
        return requestAzureSDK(subscriptionId, requestCallback, new AzureSDKArmClientProvider<com.microsoft.azure.management.compute.ComputeManagementClient>() {
            @NotNull
            @Override
            public com.microsoft.azure.management.compute.ComputeManagementClient getClient(@NotNull String subscriptionId, @NotNull String accessToken)
                    throws Throwable {
                return AzureArmSDKHelper.getArmComputeManagementClient(subscriptionId, accessToken);
            }
        });
    }

    @NotNull
    public List<VirtualMachine> getVirtualMachines(@NotNull String subscriptionId)
            throws AzureCmdException {
        return requestArmComputeSDK(subscriptionId, AzureArmSDKHelper.getArmVirtualMachines(subscriptionId));
    }

    public void restartVirtualMachine(String subscriptionId, @NotNull VirtualMachine virtualMachine) throws AzureCmdException {
        requestArmComputeSDK(subscriptionId, AzureArmSDKHelper.restartVirtualMachine(virtualMachine));
    }
}