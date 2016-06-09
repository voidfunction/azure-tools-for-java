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
package com.microsoft.tooling.msservices.serviceexplorer.azure.vm;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.vm.VirtualMachine;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class VMServiceModule extends AzureRefreshableNode {
    private static final String VM_SERVICE_MODULE_ID = VMServiceModule.class.getName();
    private static final String ICON_PATH = "virtualmachines.png";
    private static final String BASE_MODULE_NAME = "Virtual Machines (Classic)";

    public VMServiceModule(Node parent) {
        super(VM_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
        // remove all child nodes
        removeAllChildNodes();
        AzureManager azureManager = AzureManagerImpl.getManager(getProject());
        // load all VMs
        List<Subscription> subscriptionList = azureManager.getSubscriptionList();
        List<Pair<String, String>> failedSubscriptions = new ArrayList<>();
        for (Subscription subscription : subscriptionList) {
            try {
                List<VirtualMachine> virtualMachines = azureManager.getVirtualMachines(subscription.getId());
                for (VirtualMachine vm : virtualMachines) {
                    addChildNode(new VMNode(this, vm));
                }
                if (eventState.isEventTriggered()) {
                    return;
                }
            } catch (Exception ex) {
                failedSubscriptions.add(new ImmutablePair<>(subscription.getName(), ex.getMessage()));
                continue;
            }
        }
        if (!failedSubscriptions.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("An error occurred when trying to load VMs for the subscriptions:\n\n");
            for (Pair error : failedSubscriptions) {
                errorMessage.append(error.getKey()).append(": ").append(error.getValue()).append("\n");
            }
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load VMs\n\n" + errorMessage.toString(), null);
        }
    }
}