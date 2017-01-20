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
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.PublicIpAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Encryption;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;

public class AzureSDKManager {
    public static Azure createAzure(String subscriptionId) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        return azureManager.getAzure(subscriptionId);
    }

    public static StorageAccount createStorageAccount(String subscriptionId, String name, Region region, boolean newResourceGroup, String resourceGroup,
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

    public static VirtualMachine createVirtualMachine(String subscriptionId, @NotNull final com.microsoft.tooling.msservices.model.vm.VirtualMachine vm, @NotNull final VirtualMachineImage vmImage,
                                                      @NotNull final StorageAccount storageAccount, @NotNull final Network network,
                                                      @NotNull String subnet, @Nullable PublicIpAddress pip, boolean withNewPip,
                                                      @Nullable AvailabilitySet availabilitySet, boolean withNewAvailabilitySet,
                                                      @NotNull final String username, @Nullable final String password, @Nullable String publicKey) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(subscriptionId);
        boolean isWindows = vmImage.osDiskImage().operatingSystem().equals(OperatingSystemTypes.WINDOWS);
        VirtualMachine.DefinitionStages.WithPublicIpAddress withPublicIpAddress = azure.virtualMachines().define(vm.getName())
                .withRegion(vmImage.location())
                .withExistingResourceGroup(vm.getResourceGroup())
                .withExistingPrimaryNetwork(network)
                .withSubnet(subnet)
                .withPrimaryPrivateIpAddressDynamic();
        VirtualMachine.DefinitionStages.WithOS withOS;
        if (pip == null) {
            if (withNewPip) {
                withOS = withPublicIpAddress.withNewPrimaryPublicIpAddress(vm.getName() + "pip");
            } else {
                withOS = withPublicIpAddress.withoutPrimaryPublicIpAddress();
            }
        } else {
            withOS = withPublicIpAddress.withExistingPrimaryPublicIpAddress(pip);
        }
        VirtualMachine.DefinitionStages.WithCreate withCreate;
        if (isWindows) {
            withCreate = withOS.withSpecificWindowsImageVersion(vmImage.imageReference())
                    .withAdminUsername(username)
                    .withAdminPassword(password);
        } else {
            VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKey withLinuxRootPasswordOrPublicKey = withOS.withSpecificLinuxImageVersion(vmImage.imageReference())
                    .withRootUsername(username);
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
        withCreate = withCreate.withSize(vm.getSize()).withExistingStorageAccount(storageAccount);
        if (withNewAvailabilitySet) {
            withCreate = withCreate.withNewAvailabilitySet(vm.getName() + "as");
        } else if (availabilitySet != null) {
            withCreate = withCreate.withExistingAvailabilitySet(availabilitySet);
        }
        return withCreate.create();
    }
}
