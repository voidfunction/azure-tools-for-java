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


import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.Endpoint;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMNode extends AzureRefreshableNode {
    private static final String WAIT_ICON_PATH = "virtualmachinewait.png";
    private static final String STOP_ICON_PATH = "virtualmachinestop.png";
    private static final String RUN_ICON_PATH = "virtualmachinerun.png";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_DOWNLOAD_RDP_FILE = "Connect Remote Desktop";
    public static final String ACTION_SHUTDOWN = "Shutdown";
    public static final String ACTION_START = "Start";
    public static final String ACTION_RESTART = "Restart";
    public static final int REMOTE_DESKTOP_PORT = 3389;

    protected VirtualMachine virtualMachine;

    public VMNode(Node parent, VirtualMachine virtualMachine)
            throws AzureCmdException {
        super(virtualMachine.getName(), virtualMachine.getName(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;

        loadActions();

        // update vm icon based on vm status
//        refreshItemsInternal();
    }

//    private String getVMIconPath() {
//        switch (virtualMachine.getStatus()) {
//            case Ready:
//                return RUN_ICON_PATH;
//            case Stopped:
//            case StoppedDeallocated:
//                return STOP_ICON_PATH;
//            default:
//                return WAIT_ICON_PATH;
//        }
//    }

    @Override
    protected void refresh(@NotNull EventStateHandle eventState)
            throws AzureCmdException {
//        virtualMachine = AzureManagerImpl.getManager().refreshVirtualMachineInformation(virtualMachine);

        if (eventState.isEventTriggered()) {
            return;
        }

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
//        // update vm name and status icon
//        setName(virtualMachine.getName());
//        setIconPath(getVMIconPath());
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

//        actionMap.put(ACTION_DELETE, DeleteVMAction.class);
//        actionMap.put(ACTION_DOWNLOAD_RDP_FILE, DownloadRDPAction.class);
//        actionMap.put(ACTION_SHUTDOWN, ShutdownVMAction.class);
//        actionMap.put(ACTION_START, StartVMAction.class);
//        actionMap.put(ACTION_RESTART, RestartVMAction.class);

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