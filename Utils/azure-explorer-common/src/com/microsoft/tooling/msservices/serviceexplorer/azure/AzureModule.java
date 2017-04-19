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
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.ISubscriptionSelectionListener;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappsModule;

import java.util.List;
import java.util.stream.Collectors;

public class AzureModule extends AzureRefreshableNode {
    private static final String AZURE_SERVICE_MODULE_ID = AzureModule.class.getName();
    private static final String ICON_PATH = "AzureExplorer_16.png";
    private static final String BASE_MODULE_NAME = "Azure";

    private Object project;
    private VMArmModule vmArmServiceModule;
    private RedisCacheModule redisCacheModule;
    private StorageModule storageModule;
    private WebappsModule webappsModule;
    private HDInsightRootModule hdInsightModule;
    private DockerHostModule dockerHostModule;

    public AzureModule(Object project) {
        this(null, ICON_PATH, null);
        this.project = project;
        storageModule = new StorageModule(this);
        webappsModule = new WebappsModule(this);
        //hdInsightModule = new HDInsightRootModule(this);
        vmArmServiceModule = new VMArmModule(this);
        redisCacheModule = new RedisCacheModule(this);
        dockerHostModule = new DockerHostModule(this);
        try {
            SignInOutListener signInOutListener = new SignInOutListener();
            AuthMethodManager.getInstance().addSignInEventListener(signInOutListener);
            AuthMethodManager.getInstance().addSignOutEventListener(signInOutListener);
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
        // in case we already signed in with service principal between restarts, sign in event was not fired
        addSubscriptionSelectionListener();
    }

    public AzureModule(Node parent, String iconPath, Object data) {
        super(AZURE_SERVICE_MODULE_ID, composeName(), parent, iconPath);
    }

    public void setHdInsightModule(@NotNull HDInsightRootModule rootModule) {
        this.hdInsightModule = rootModule;
    }

    private static String composeName() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return BASE_MODULE_NAME + " (Not Signed In)";
            }
            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
            if (selectedSubscriptions.size() > 0) {
                return String.format("%s (%s)", BASE_MODULE_NAME, selectedSubscriptions.size() > 1
                        ? String.format("%s subscriptions", selectedSubscriptions.size())
                        : selectedSubscriptions.get(0).getSubscriptionName());
            }
        } catch (Exception e) {
        	String msg = "An error occurred while getting the subscription list." + "\n" + "(Message from Azure:" + e.getMessage() + ")";
        	DefaultLoader.getUIHelper().showException(msg, e,
        			"MS Services - Error Getting Subscriptions", false, true);
        }
        return BASE_MODULE_NAME;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // add the module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item
        if (!isDirectChild(vmArmServiceModule)) {
            addChildNode(vmArmServiceModule);
        }
        if (!isDirectChild(redisCacheModule)) {
            addChildNode(redisCacheModule);
        }
        if (!isDirectChild(storageModule)) {
            addChildNode(storageModule);
        }
        if (!isDirectChild(webappsModule)) {
            addChildNode(webappsModule);
        }
        if (hdInsightModule != null && !isDirectChild(hdInsightModule)) {
            addChildNode(hdInsightModule);
        }
        if (!isDirectChild(dockerHostModule)) {
            addChildNode(dockerHostModule);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
    }

    @Override
    protected void refreshFromAzure() throws AzureCmdException {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                vmArmServiceModule.load(true);
                redisCacheModule.load(true);
                storageModule.load(true);
                webappsModule.load(true);
                hdInsightModule.load(true);
                dockerHostModule.load(true);
            }
        } catch (Exception e) {
            throw new AzureCmdException("Error loading Azure Explorer modules", e);
        }
    }

    @Override
    public Object getProject() {
        return project;
    }

    private class SignInOutListener implements Runnable {
        @Override
        public void run() {
            handleSubscriptionChange();
            addSubscriptionSelectionListener();
        }
    }

    private void addSubscriptionSelectionListener() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }
            azureManager.getSubscriptionManager().addListener(new ISubscriptionSelectionListener() {
                @Override
                public void update(boolean isRefresh) {
                    if (!isRefresh) {
                        handleSubscriptionChange();
                    }
                }
            });
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }

    private void handleSubscriptionChange() {
        setName(composeName());
        for (Node child : getChildNodes()) {
            child.removeAllChildNodes();
        }
//        AzureUIRefreshCore.removeAll();
        if (AzureUIRefreshCore.publisher != null) {
            // trigger a force update/reload
            AzureUIRefreshCore.publisher.onNext(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.UPDATE, null));
            AzureUIRefreshCore.publisher.onCompleted();
        }
    }
}
