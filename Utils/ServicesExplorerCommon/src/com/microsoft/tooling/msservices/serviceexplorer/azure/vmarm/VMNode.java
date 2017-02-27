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
package com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

public class VMNode extends RefreshableNode {
    private static String RUNNING_STATUS = "PowerState/running";

    public class DeleteVMAction extends AzureNodeActionPromptListener {
        public DeleteVMAction() {
            super(VMNode.this,
                    String.format("This operation will delete virtual machine %s. The associated disks will not be deleted " +
                            "from your storage account. Are you sure you want to continue?", virtualMachine.name()),
                    "Deleting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                azureManager.getAzure(subscriptionId).virtualMachines().deleteByGroup(virtualMachine.resourceGroupName(), virtualMachine.name());
            } catch (Exception ex) {

            }
            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    // instruct parent node to remove this node
                    getParent().removeDirectChildNode(VMNode.this);
                }
            });
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

    public class RestartVMAction extends AzureNodeActionPromptListener {
        public RestartVMAction() {
            super(VMNode.this,
                    String.format("Are you sure you want to restart the virtual machine %s?", virtualMachine.computerName()),
                    "Restarting VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            virtualMachine.restart();
//            refreshItemsInternal();
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

    public class ShutdownVMAction extends AzureNodeActionPromptListener {
        public ShutdownVMAction() {
            super(VMNode.this, String.format(
                    "This operation will result in losing the VIP that was assigned to this virtual machine. " +
                            "Are you sure that you want to shut down virtual machine %s?", virtualMachine.name()),
                    "Shutting down VM");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            virtualMachine.powerOff();
            refreshItemsInternal();
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }

//    public class RestartVMAction extends VMNodeActionPromptListener {
//        public RestartVMAction() {
//            super(VMNode.this,
//                    "Are you sure you want to restart the virtual machine %s?",
//                    "Restarting VM");
//        }
//
//        @Override
//        protected void azureNodeAction(NodeActionEvent e, @NotNull EventStateHandle stateHandle)
//                throws AzureCmdException {
//            AzureManagerImpl.getManager().restartVirtualMachineArm(virtualMachine);
//        }
//    }

    private static final String WAIT_ICON_PATH = "virtualmachinewait.png";
    private static final String STOP_ICON_PATH = "virtualmachinestop.png";
    private static final String RUN_ICON_PATH = "virtualmachinerun.png";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_DOWNLOAD_RDP_FILE = "Connect Remote Desktop";
    public static final String ACTION_SHUTDOWN = "Shutdown";
    public static final String ACTION_START = "Start";
    public static final String ACTION_RESTART = "Restart";
    public static final int REMOTE_DESKTOP_PORT = 3389;

    private VirtualMachine virtualMachine;
    private String subscriptionId;

    public VMNode(Node parent, String subscriptionId, VirtualMachine virtualMachine)
            throws AzureCmdException {
        super(virtualMachine.id(), virtualMachine.name(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;
        this.subscriptionId = subscriptionId;
        loadActions();

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        try {
            for (InstanceViewStatus status : virtualMachine.instanceView().statuses()) {
                if (RUNNING_STATUS.equalsIgnoreCase(status.code())) {
                    return RUN_ICON_PATH;
                }
                if (status.code().toLowerCase().contains("stopped")) {
                    return STOP_ICON_PATH;
                }
            }
        } catch (CloudException e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
        }
        return WAIT_ICON_PATH;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        virtualMachine.refresh();

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
        // update vm name and status icon
        setName(virtualMachine.name());
        setIconPath(getVMIconPath());
//
//        // load up the endpoint nodes
//        removeAllChildNodes();
//
//        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
//            VMEndpointNode vmEndPoint = new VMEndpointNode(this, endpoint);
//            addChildNode(vmEndPoint);
//        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        Map<String, Class<? extends NodeActionListener>> actionMap =
                new HashMap<String, Class<? extends NodeActionListener>>();

        actionMap.put(ACTION_DELETE, DeleteVMAction.class);
//        actionMap.put(ACTION_DOWNLOAD_RDP_FILE, DownloadRDPAction.class);
        actionMap.put(ACTION_SHUTDOWN, ShutdownVMAction.class);
//        actionMap.put(ACTION_START, StartVMAction.class);
        actionMap.put(ACTION_RESTART, RestartVMAction.class);

        return ImmutableMap.copyOf(actionMap);
    }

    @Override
    public List<NodeAction> getNodeActions() {
//        // enable/disable menu items according to VM status
//        boolean started = virtualMachine.getStatus().equals(VirtualMachine.Status.Ready);
//        boolean stopped = virtualMachine.getStatus().equals(VirtualMachine.Status.Stopped) ||
//                virtualMachine.getStatus().equals(VirtualMachine.Status.StoppedDeallocated);
//
//        getNodeActionByName(ACTION_DOWNLOAD_RDP_FILE).setEnabled(!stopped && hasRDPPort(virtualMachine));
//        getNodeActionByName(ACTION_SHUTDOWN).setEnabled(started);
//        getNodeActionByName(ACTION_START).setEnabled(stopped);
//        getNodeActionByName(ACTION_RESTART).setEnabled(started);

        return super.getNodeActions();
    }

    private boolean hasRDPPort(VirtualMachine virtualMachine) {
//        for (Endpoint endpoint : virtualMachine.getEndpoints()) {
//            if (endpoint.getPrivatePort() == REMOTE_DESKTOP_PORT) {
//                return true;
//            }
//        }

        return false;
    }
}