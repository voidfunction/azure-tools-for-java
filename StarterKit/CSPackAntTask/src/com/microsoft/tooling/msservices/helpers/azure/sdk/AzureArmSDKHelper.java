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

import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.rest.credentials.TokenCredentials;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AzureArmSDKHelper {

    @NotNull
    public static com.microsoft.azure.management.compute.ComputeManagementClient getArmComputeManagementClient(@NotNull String subscriptionId,
                                                                                                               @NotNull String accessToken) throws IOException, URISyntaxException, AzureCmdException {
        TokenCredentials credentials = new TokenCredentials(null, accessToken);
        com.microsoft.azure.management.compute.ComputeManagementClient client = new com.microsoft.azure.management.compute.ComputeManagementClientImpl(credentials);
        client.setSubscriptionId(subscriptionId);
//        UserAgentInterceptor
//        client.withRequestFilterFirst(new AzureToolkitFilter());
        // add a request filter for tacking on the A/D auth token if the current authentication
        // mode is active directory
//        AuthTokenRequestFilter requestFilter = new AuthTokenRequestFilter(accessToken);
//        return client.withRequestFilterFirst(requestFilter);
        return client;
    }

    @NotNull
    public static SDKRequestCallback<List<VirtualMachine>, com.microsoft.azure.management.compute.ComputeManagementClient> getArmVirtualMachines(@NotNull final String subscriptionId) {
        return new SDKRequestCallback<List<com.microsoft.azure.management.compute.models.VirtualMachine>, com.microsoft.azure.management.compute.ComputeManagementClient>() {
            @NotNull
            @Override
            public List<com.microsoft.azure.management.compute.models.VirtualMachine> execute(@NotNull com.microsoft.azure.management.compute.ComputeManagementClient client)
                    throws Throwable {
                List<com.microsoft.azure.management.compute.models.VirtualMachine> virtualMachines = Arrays.asList(client.getVirtualMachinesOperations().listAll().getBody().toArray(new VirtualMachine[0]));
                if (virtualMachines == null) {
                    return new ArrayList<VirtualMachine>();
                }
                return virtualMachines;
            }
        };
    }

    public static SDKRequestCallback<Void, com.microsoft.azure.management.compute.ComputeManagementClient> restartVirtualMachine(@NotNull final VirtualMachine virtualMachine) {
        return new SDKRequestCallback<Void, com.microsoft.azure.management.compute.ComputeManagementClient>() {
            @Override
            public Void execute(@NotNull com.microsoft.azure.management.compute.ComputeManagementClient client) throws Throwable {
                return client.getVirtualMachinesOperations().restart("", virtualMachine.getName()).getBody();
            }
        };
    }
}
