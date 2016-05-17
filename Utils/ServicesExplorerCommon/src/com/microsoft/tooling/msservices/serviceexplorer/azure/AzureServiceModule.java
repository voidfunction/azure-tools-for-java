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
package com.microsoft.tooling.msservices.serviceexplorer.azure;

import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper.EventWaitHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vm.VMServiceModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmServiceModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappsModule;

import java.util.List;
import java.util.Map;

public class AzureServiceModule extends RefreshableNode {
    private static final String AZURE_SERVICE_MODULE_ID = AzureServiceModule.class.getName();
    private static final String ICON_PATH = "azure_explorer.png";
    private static final String BASE_MODULE_NAME = "Azure";

    private Object project;
    private VMServiceModule vmServiceModule;
    private VMArmServiceModule vmArmServiceModule;
    private StorageModule storageServiceModule;
    private WebappsModule webappsModule;
    private HDInsightRootModule hdInsightModule;
    private boolean storageModuleOnly;
    private EventWaitHandle subscriptionsChanged;
    private boolean registeredSubscriptionsChanged;
    private final Object subscriptionsChangedSync = new Object();
    // by default its null which means load data and don't use cached data
    public static Map<WebSite, WebSiteConfiguration> webSiteConfigMap = null;

    public AzureServiceModule(Object project, boolean storageModuleOnly) {
        this(null, ICON_PATH, null);
        this.project = project;
        this.storageModuleOnly = storageModuleOnly;
        storageServiceModule = new StorageModule(this);
        webappsModule = new WebappsModule(this);
        //hdInsightModule = new HDInsightRootModule(this);
        if (!storageModuleOnly) {
            vmServiceModule = new VMServiceModule(this);
        }
        vmArmServiceModule = new VMArmServiceModule(this);
    }

    public AzureServiceModule(Node parent, String iconPath, Object data) {
        super(AZURE_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, iconPath);
    }

    public void setHdInsightModule(@NotNull HDInsightRootModule rootModule) {
        this.hdInsightModule = rootModule;
    }

    @Override
    public String getName() {
//        try {
            List<Subscription> subscriptionList = AzureManagerImpl.getManager(getProject()).getSubscriptionList();
            if (subscriptionList.size() > 0) {
                return String.format("%s (%s)", BASE_MODULE_NAME, subscriptionList.size() > 1
                        ? String.format("%s subscriptions", subscriptionList.size())
                        : subscriptionList.get(0).getName());
            }
//        } catch (AzureCmdException e) {
//        	String msg = "An error occurred while getting the subscription list." + "\n" + "(Message from Azure:" + e.getMessage() + ")";
//        	DefaultLoader.getUIHelper().showException(msg, e,
//        			"MS Services - Error Getting Subscriptions", false, true);
//        }
        return BASE_MODULE_NAME;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // add the module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item
        if (!storageModuleOnly) {
            if (!vmServiceModule.isLoading()) {
                if (!isDirectChild(vmServiceModule)) {
                    addChildNode(vmServiceModule);
                }

                vmServiceModule.load();
            }
        }

        if (!vmArmServiceModule.isLoading()) {
            if (!isDirectChild(vmArmServiceModule)) {
                addChildNode(vmArmServiceModule);
            }
            vmArmServiceModule.load();
        }

        if (!storageServiceModule.isLoading()) {
            if (!isDirectChild(storageServiceModule)) {
                addChildNode(storageServiceModule);
            }

            storageServiceModule.load();
        }

        if (!webappsModule.isLoading()) {
            if (!isDirectChild(webappsModule)) {
                addChildNode(webappsModule);
            }

            webappsModule.load();
        }
        if (!vmArmServiceModule.isLoading()) {
            if (!isDirectChild(vmArmServiceModule)) {
                addChildNode(vmArmServiceModule);
            }
            vmArmServiceModule.load();
        }
        if (hdInsightModule != null && !hdInsightModule.isLoading()) {
            if (!isDirectChild(hdInsightModule)) {
                addChildNode(hdInsightModule);
            }
            hdInsightModule.load();
        }
    }

    @Override
    public Object getProject() {
        return project;
    }

    public void registerSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            if (subscriptionsChanged == null) {
                subscriptionsChanged = AzureManagerImpl.getManager(getProject()).registerSubscriptionsChanged();
            }

            registeredSubscriptionsChanged = true;

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    while (registeredSubscriptionsChanged) {
                        try {
                            subscriptionsChanged.waitEvent(new Runnable() {
                                @Override
                                public void run() {
                                    if (registeredSubscriptionsChanged) {
                                        removeAllChildNodes();
                                        if (!storageModuleOnly) {
                                            vmServiceModule = new VMServiceModule(AzureServiceModule.this);
                                        }
                                        storageServiceModule = new StorageModule(AzureServiceModule.this);
                                        hdInsightModule = hdInsightModule.getNewNode(AzureServiceModule.this);

                                        load();
                                    }
                                }
                            });
                        } catch (AzureCmdException ignored) {
                            break;
                        }
                    }
                }
            });
        }
    }

    public void unregisterSubscriptionsChanged()
            throws AzureCmdException {
        synchronized (subscriptionsChangedSync) {
            registeredSubscriptionsChanged = false;

            if (subscriptionsChanged != null) {
                AzureManagerImpl.getManager(getProject()).unregisterSubscriptionsChanged(subscriptionsChanged);
                subscriptionsChanged = null;
            }
        }
    }
}
