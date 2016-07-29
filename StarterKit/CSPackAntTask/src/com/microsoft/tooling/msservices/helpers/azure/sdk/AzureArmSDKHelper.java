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
import com.microsoft.azure.Azure;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.rest.credentials.TokenCredentials;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class AzureArmSDKHelper {

    @NotNull
    public static Azure getAzure(@NotNull String subscriptionId, @NotNull String accessToken)
            throws IOException, URISyntaxException, AzureCmdException {
        TokenCredentials credentials = new TokenCredentials(null, accessToken);
        return Azure.configure().authenticate(credentials).withSubscription(subscriptionId);
    }

    @NotNull
    public static AzureRequestCallback<List<ResourceGroup>> getResourceGroups() {
        return new AzureRequestCallback<List<ResourceGroup>>() {
            @NotNull
            @Override
            public List<ResourceGroup> execute(@NotNull Azure azure) throws Throwable {
                return azure.resourceGroups().list();
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<VirtualMachine>> getVirtualMachines() {
        return new AzureRequestCallback<List<VirtualMachine>>() {
            @NotNull
            @Override
            public List<VirtualMachine> execute(@NotNull Azure azure) throws Throwable {
                List<VirtualMachine> virtualMachines = azure.virtualMachines().list();
                if (virtualMachines == null) {
                    return new ArrayList<VirtualMachine>();
                }
                return virtualMachines;
            }
        };
    }

    public static AzureRequestCallback<Void> restartVirtualMachine(@NotNull final VirtualMachine virtualMachine) {
        return new AzureRequestCallback<Void>() {
            @Override
            public Void execute(@NotNull Azure azure) throws Throwable {
                azure.virtualMachines().restart(virtualMachine.resourceGroupName(), virtualMachine.name());
//                virtualMachine.refreshInstanceView();
                return null;
            }
        };
    }

    public static AzureRequestCallback<Void> shutdownVirtualMachine(@NotNull final VirtualMachine virtualMachine) {
        return new AzureRequestCallback<Void>() {
            @Override
            public Void execute(@NotNull Azure azure) throws Throwable {
                azure.virtualMachines().powerOff(virtualMachine.resourceGroupName(), virtualMachine.name());
                virtualMachine.refreshInstanceView();
                return null;
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<VirtualMachine> createVirtualMachine(@NotNull final com.microsoft.tooling.msservices.model.vm.VirtualMachine vm, @NotNull final com.microsoft.tooling.msservices.model.vm.VirtualMachineImage vmImage,
                                                                                         @NotNull final com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount, @NotNull final String virtualNetwork,
                                                                                         @NotNull final String username, @NotNull final String password, @NotNull final byte[] certificate) {
        return new AzureRequestCallback<VirtualMachine>() {
            @NotNull
            @Override
            public VirtualMachine execute(@NotNull Azure azure) throws Throwable {
                return azure.virtualMachines().define(vm.getName())
                        .withRegion(Region.US_WEST)
                        .withNewResourceGroup()
                        .withNewPrimaryNetwork("10.0.0.0/28")
                        .withPrimaryPrivateIpAddressDynamic()
                        .withoutPrimaryPublicIpAddress()
                        .withPopularWindowsImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER)
//                        .withStoredWindowsImage("")
                        .withAdminUserName(username)
                        .withPassword(password)
                        .withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)
                        .create();
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<com.microsoft.tooling.msservices.model.storage.StorageAccount>> getStorageAccounts(@NotNull final String subscriptionId) {
        return new AzureRequestCallback<List<com.microsoft.tooling.msservices.model.storage.StorageAccount>>() {
            @NotNull
            @Override
            public List<com.microsoft.tooling.msservices.model.storage.StorageAccount> execute(@NotNull Azure azure) throws Throwable {
                List<com.microsoft.tooling.msservices.model.storage.StorageAccount> storageAccounts = new ArrayList<>();
                for (StorageAccount storageAccount : azure.storageAccounts().list()){
                    com.microsoft.tooling.msservices.model.storage.StorageAccount sa =
                            new com.microsoft.tooling.msservices.model.storage.StorageAccount(storageAccount.name(), subscriptionId);

                    sa.setProtocol("https");
                    sa.setType(storageAccount.sku().name().toString());
                    sa.setLocation(Strings.nullToEmpty(storageAccount.regionName()));
                    List<StorageAccountKey> keys = storageAccount.keys();
                    if (!(keys == null || keys.isEmpty())) {
                        sa.setPrimaryKey(keys.get(0).value());
                        if (keys.size() > 1) {
                            sa.setSecondaryKey(keys.get(1).value());
                        }
                    }
                    sa.setResourceGroupName(storageAccount.resourceGroupName());
                    storageAccounts.add(sa);
                }
                return storageAccounts;
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<Void> deleteStorageAccount(@NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount) {
        return new AzureRequestCallback<Void>() {
            @NotNull
            @Override
            public Void execute(@NotNull Azure azure) throws Throwable {
                azure.storageAccounts().delete(storageAccount.getResourceGroupName(), storageAccount.getName());
                return null;
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<StorageAccount> createStorageAccount(@NotNull com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount) {
        return new AzureRequestCallback<StorageAccount>() {
            @NotNull
            @Override
            public StorageAccount execute(@NotNull Azure azure) throws Throwable {
                StorageAccount.DefinitionStages.WithGroup newStorageAccountBlank = azure.storageAccounts().define(storageAccount.getName())
                        .withRegion(storageAccount.getLocation());
                StorageAccount.DefinitionStages.WithCreate newStorageAccountWithGroup;
                if (storageAccount.isNewResourceGroup()) {
                    newStorageAccountWithGroup = newStorageAccountBlank.withNewResourceGroup(storageAccount.getResourceGroupName());
                } else {
                    newStorageAccountWithGroup = newStorageAccountBlank.withExistingResourceGroup(storageAccount.getResourceGroupName());
                }
                return newStorageAccountWithGroup.withSku(SkuName.fromString(storageAccount.getType())).create();
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<Network>> getVirtualNetworks() {
        return new AzureRequestCallback<List<Network>>() {
            @NotNull
            @Override
            public List<Network> execute(@NotNull Azure azure) throws Throwable {
                return azure.networks().list();
            }
        };
    }
}
