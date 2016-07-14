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

import com.microsoft.azure.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
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
    public static AzureRequestCallback<List<VirtualMachine>> getVirtualMachines(@NotNull final String subscriptionId) {
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
}
