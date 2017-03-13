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

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Encryption;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;

public class AzureSDKManager {
    public static Azure createAzure(String subscriptionId) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        return azureManager.getAzure(subscriptionId);
    }

    public static StorageAccount createStorageAccount(String subscriptionId, String name, String region, boolean newResourceGroup, String resourceGroup,
                                                      Kind kind, AccessTier accessTier, boolean enableEncription, String skuName) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(subscriptionId);
        StorageAccount.DefinitionStages.WithGroup newStorageAccountBlank = azure.storageAccounts().define(name).withRegion(region);
        StorageAccount.DefinitionStages.WithCreate newStorageAccountWithGroup;
        if (newResourceGroup) {
            newStorageAccountWithGroup = newStorageAccountBlank.withNewResourceGroup(resourceGroup);
        } else {
            newStorageAccountWithGroup = newStorageAccountBlank.withExistingResourceGroup(resourceGroup);
        }
        if (kind == Kind.BLOB_STORAGE) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withBlobStorageAccountKind().withAccessTier(accessTier);
        } else {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withGeneralPurposeAccountKind();
        }

        if (enableEncription) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withEncryption(new Encryption());
        }

        return newStorageAccountWithGroup.withSku(SkuName.fromString(skuName)).create();
    }

    public static VirtualMachine createVirtualMachine(String subscriptionId, @NotNull String name, @NotNull String resourceGroup, boolean withNewResourceGroup,
                                                      @NotNull String size, @NotNull String region, final VirtualMachineImage vmImage, Object knownImage, boolean isKnownImage,
                                                      @NotNull final StorageAccount storageAccount, @NotNull final Network network, VirtualNetwork newNetwork, boolean withNewNetwork,
                                                      @NotNull String subnet, @Nullable PublicIpAddress pip, boolean withNewPip,
                                                      @Nullable AvailabilitySet availabilitySet, boolean withNewAvailabilitySet,
                                                      @NotNull final String username, @Nullable final String password, @Nullable String publicKey) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(subscriptionId);
        boolean isWindows;
        if (isKnownImage) {
            isWindows = knownImage instanceof KnownWindowsVirtualMachineImage;
        } else {
            isWindows = vmImage.osDiskImage().operatingSystem().equals(OperatingSystemTypes.WINDOWS);
        }
        VirtualMachine.DefinitionStages.WithGroup withGroup = azure.virtualMachines().define(name)
                .withRegion(region);
        Creatable<ResourceGroup> newResourceGroup = null;
        VirtualMachine.DefinitionStages.WithNetwork withNetwork;
        if (withNewResourceGroup) {
            newResourceGroup = azure.resourceGroups().define(resourceGroup).withRegion(region);
            withNetwork = withGroup.withNewResourceGroup(newResourceGroup);
        } else {
            withNetwork = withGroup.withExistingResourceGroup(resourceGroup);
        }
        VirtualMachine.DefinitionStages.WithPublicIpAddress withPublicIpAddress;
        if (withNewNetwork) {
            Network.DefinitionStages.WithGroup networkWithGroup = azure.networks().define(newNetwork.name).withRegion(region);
            Creatable<Network> newVirtualNetwork;
            if (withNewResourceGroup) {
                newVirtualNetwork = networkWithGroup.withNewResourceGroup(newResourceGroup)
                        .withAddressSpace(newNetwork.addressSpace)
                        .withSubnet(newNetwork.subnet.name, newNetwork.subnet.addressSpace);
            } else {
                newVirtualNetwork = networkWithGroup.withExistingResourceGroup(resourceGroup)
                        .withAddressSpace(newNetwork.addressSpace)
                        .withSubnet(newNetwork.subnet.name, newNetwork.subnet.addressSpace);
            }
            withPublicIpAddress = withNetwork.withNewPrimaryNetwork(newVirtualNetwork).withPrimaryPrivateIpAddressDynamic();
//            withPublicIpAddress = withNetwork.withNewPrimaryNetwork("10.0.0.0/28").
//                    .withPrimaryPrivateIpAddressDynamic();
        } else {
            withPublicIpAddress = withNetwork.withExistingPrimaryNetwork(network)
                    .withSubnet(subnet)
                    .withPrimaryPrivateIpAddressDynamic();
        }
        VirtualMachine.DefinitionStages.WithOS withOS;
        if (pip == null) {
            if (withNewPip) {
                withOS = withPublicIpAddress.withNewPrimaryPublicIpAddress(name + "pip");
            } else {
                withOS = withPublicIpAddress.withoutPrimaryPublicIpAddress();
            }
        } else {
            withOS = withPublicIpAddress.withExistingPrimaryPublicIpAddress(pip);
        }
        VirtualMachine.DefinitionStages.WithCreate withCreate;
        if (isWindows) {
            VirtualMachine.DefinitionStages.WithWindowsAdminUsername withWindowsAdminUsername;
            if (isKnownImage) {
                withWindowsAdminUsername = withOS.withPopularWindowsImage((KnownWindowsVirtualMachineImage) knownImage);
            } else {
                withWindowsAdminUsername = withOS.withSpecificWindowsImageVersion(vmImage.imageReference());
            }
            withCreate = withWindowsAdminUsername.withAdminUsername(username).withAdminPassword(password);
        } else {
            VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKey withLinuxRootPasswordOrPublicKey;
            if (isKnownImage) {
                withLinuxRootPasswordOrPublicKey = withOS.withPopularLinuxImage((KnownLinuxVirtualMachineImage) knownImage).withRootUsername(username);
            } else {
                withLinuxRootPasswordOrPublicKey = withOS.withSpecificLinuxImageVersion(vmImage.imageReference()).withRootUsername(username);
            }
            VirtualMachine.DefinitionStages.WithLinuxCreate withLinuxCreate;
            // we assume either password or public key is not empty
            if (password != null) {
                withLinuxCreate = withLinuxRootPasswordOrPublicKey.withRootPassword(password);
                if (publicKey != null) {
                    withCreate = withLinuxCreate.withSsh(publicKey);
                } else {
                    withCreate = withLinuxCreate;
                }
            } else {
                withCreate = withLinuxRootPasswordOrPublicKey.withSsh(publicKey);
            }
        }
        withCreate = withCreate.withSize(size).withExistingStorageAccount(storageAccount);
        if (withNewAvailabilitySet) {
            withCreate = withCreate.withNewAvailabilitySet(name + "as");
        } else if (availabilitySet != null) {
            withCreate = withCreate.withExistingAvailabilitySet(availabilitySet);
        }
        return withCreate.create();
    }
}
