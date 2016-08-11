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
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.rest.credentials.TokenCredentials;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.storage.ArmStorageAccount;

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

    public static AzureRequestCallback<Void> deleteVirtualMachine(@NotNull final VirtualMachine virtualMachine) {
        return new AzureRequestCallback<Void>() {
            @Override
            public Void execute(@NotNull Azure azure) throws Throwable {
                azure.virtualMachines().delete(virtualMachine.resourceGroupName(), virtualMachine.name());
                return null;
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<VirtualMachine> createVirtualMachine(@NotNull final com.microsoft.tooling.msservices.model.vm.VirtualMachine vm, @NotNull final VirtualMachineImage vmImage,
                                                                                         @NotNull final ArmStorageAccount storageAccount, @NotNull final Network network,
                                                                                         @NotNull String subnet, @NotNull final String username, @NotNull final String password, @NotNull final byte[] certificate) {
        return new AzureRequestCallback<VirtualMachine>() {
            @NotNull
            @Override
            public VirtualMachine execute(@NotNull Azure azure) throws Throwable {
                boolean isWindows = vmImage.osDiskImage().operatingSystem().equals(OperatingSystemTypes.WINDOWS);
                if (isWindows) {
                    return azure.virtualMachines().define(vm.getName())
                            .withRegion(vmImage.location())
                            .withExistingResourceGroup(vm.getResourceGroup())
                            .withExistingPrimaryNetwork(network)
                            .withSubnet(subnet)
                            .withPrimaryPrivateIpAddressDynamic()
                            .withoutPrimaryPublicIpAddress()
                            .withSpecificWindowsImageVersion(vmImage.imageReference())
                            .withAdminUserName(username)
                            .withPassword(password)
                            .withSize(vm.getSize())
                            .withExistingStorageAccount(storageAccount.getStorageAccount())
                            .create();
                } else {
                    return azure.virtualMachines().define(vm.getName())
                            .withRegion(vmImage.location())
                            .withExistingResourceGroup(vm.getResourceGroup())
                            .withExistingPrimaryNetwork(network)
                            .withSubnet(subnet)
                            .withPrimaryPrivateIpAddressDynamic()
                            .withoutPrimaryPublicIpAddress()
                            .withSpecificLinuxImageVersion(vmImage.imageReference())
                            .withRootUserName(username)
                            .withPassword(password)
                            .withSize(vm.getSize())
                            .withExistingStorageAccount(storageAccount.getStorageAccount())
                            .create();
                }
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<VirtualMachineImage>> getVirtualMachineImages(@NotNull Region region) {
        return new AzureRequestCallback<List<VirtualMachineImage>>() {
            @NotNull
            @Override
            public List<VirtualMachineImage> execute(@NotNull Azure azure) throws Throwable {
                return azure.virtualMachineImages().listByRegion(region);
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<VirtualMachinePublisher>> getVirtualMachinePublishers(@NotNull Region region) {
        return new AzureRequestCallback<List<VirtualMachinePublisher>>() {
            @NotNull
            @Override
            public List<VirtualMachinePublisher> execute(@NotNull Azure azure) throws Throwable {
                return azure.virtualMachineImages().publishers().listByRegion(region);
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<Network> createVirtualNetwork(@NotNull String name, @NotNull Region region,  String addressSpace,
                                                                     @NotNull String groupName, boolean isNewGroup) {
        return new AzureRequestCallback<Network>() {
            @NotNull
            @Override
            public Network execute(@NotNull Azure azure) throws Throwable {
                if (isNewGroup) {
                    return azure.networks().define(name)
                            .withRegion(region)
                            .withNewResourceGroup(groupName)
                            .withAddressSpace(addressSpace)
                            .create();
                } else {
                    return azure.networks().define(name)
                            .withRegion(region)
                            .withExistingResourceGroup(groupName)
                            .withAddressSpace(addressSpace)
                            .create();
                }
            }
        };
    }

    @NotNull
    public static AzureRequestCallback<List<ArmStorageAccount>> getStorageAccounts(@NotNull final String subscriptionId) {
        return new AzureRequestCallback<List<ArmStorageAccount>>() {
            @NotNull
            @Override
            public List<ArmStorageAccount> execute(@NotNull Azure azure) throws Throwable {
                List<ArmStorageAccount> storageAccounts = new ArrayList<>();
                for (StorageAccount storageAccount : azure.storageAccounts().list()){
                    ArmStorageAccount sa = new ArmStorageAccount(storageAccount.name(), subscriptionId, storageAccount);

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
    public static AzureRequestCallback<StorageAccount> createStorageAccount(@NotNull com.microsoft.tooling.msservices.model.storage.ArmStorageAccount storageAccount) {
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
                if (storageAccount.getKind() == Kind.BLOB_STORAGE) {
                    newStorageAccountWithGroup.withBlobStorageAccountKind();
                } else {
                    newStorageAccountWithGroup.withGeneralPurposeAccountKind();
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
