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
package com.microsoft.tooling.msservices.serviceexplorer.azure.webapps;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventStateHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.List;

public class WebappNode extends Node {
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";

    private static final String WAIT_ICON_PATH = "website.png";
    private WebSite webSite;

    public WebappNode(WebappsModule parent, WebSite webSite) {
        super(webSite.getName(), webSite.getName(), parent, WAIT_ICON_PATH, true);

        this.webSite = webSite;

        loadActions();
    }

    public WebSite getWebSite() {
        return webSite;
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = "Running".equals(webSite.getStatus());
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);

        return super.getNodeActions();
    }

    @Override
    protected void loadActions() {
        addAction("Stop", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false, true, "Stopping Web App...", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureManagerImpl.getManager().stopWebSite(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName());
                            webSite = AzureManagerImpl.getManager().getWebSite(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName());
                        } catch (AzureCmdException e) {
                            DefaultLoader.getUIHelper().showException("An error occurred while attempting to stop the Web App", e,
                                    "Azure Services Explorer - Error Stopping Web App", false, true);
                        }
                    }
                });
            }
        });
        addAction("Start", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false, true, "Stopping Web App...", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureManagerImpl.getManager().startWebSite(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName());
                        } catch (AzureCmdException e) {
                            DefaultLoader.getUIHelper().showException("An error occurred while attempting to start the Web App", e,
                                    "Azure Services Explorer - Error Starting Web App", false, true);
                        }
                    }
                });
            }
        });

        super.loadActions();
    }
}