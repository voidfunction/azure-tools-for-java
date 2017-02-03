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

import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public class WebappsModule extends RefreshableNode {
	private static final String WEBAPPS_MODULE_ID = WebappsModule.class.getName();
	private static final String WEB_RUN_ICON = "website.png";
	private static final String WEB_STOP_ICON = "stopWebsite.png";
	private static final String BASE_MODULE_NAME = "Web Apps";
	String runStatus = "Running";

	public WebappsModule(Node parent) {
		super(WEBAPPS_MODULE_ID, BASE_MODULE_NAME, parent, WEB_RUN_ICON);
	}

	@Override
	protected void onNodeClick(NodeActionEvent e) {
		this.load();
	}

	@Override
	protected void refreshItems()
			throws AzureCmdException {
		removeAllChildNodes();
		//todo
//		AzureManager manager = AzureManagerImpl.getManager(getProject());
//		List<Subscription> subscriptionList = manager.getSubscriptionList();
//		for (Subscription subscription : subscriptionList) {
//			Map<WebSite, WebSiteConfiguration> webSiteConfigMapTemp = new HashMap<WebSite, WebSiteConfiguration>();
//			if (AzureModule.webSiteConfigMap == null) {
//				// map null means load data and don't use cached data
//				for (final String webSpace : manager.getResourceGroupNames(subscription.getId())) {
//					List<WebSite> webapps = manager.getWebSites(subscription.getId(), webSpace);
//					for (WebSite webapp : webapps) {
//						WebSiteConfiguration webSiteConfiguration = manager.
//								getWebSiteConfiguration(webapp.getSubscriptionId(),
//										webapp.getWebSpaceName(), webapp.getName());
//						webSiteConfigMapTemp.put(webapp, webSiteConfiguration);
//					}
//				}
//				// save preferences
//				DefaultLoader.getUIHelper().saveWebAppPreferences(getProject(), webSiteConfigMapTemp);
//			} else {
//				webSiteConfigMapTemp = AzureModule.webSiteConfigMap;
//			}
//
//			if (webSiteConfigMapTemp != null && !webSiteConfigMapTemp.isEmpty()) {
//				List<WebSite> webSiteList = new ArrayList<WebSite>(webSiteConfigMapTemp.keySet());
//				Collections.sort(webSiteList, new Comparator<WebSite>() {
//					@Override
//					public int compare(WebSite ws1, WebSite ws2) {
//						return ws1.getName().compareTo(ws2.getName());
//					}
//				});
//				for (WebSite webSite : webSiteList) {
//					if (webSite.getStatus().equalsIgnoreCase(runStatus)) {
//						addChildNode(new WebappNode(this, webSite, WEB_RUN_ICON));
//					} else {
//						addChildNode(new WebappNode(this, webSite, WEB_STOP_ICON));
//					}
//				}
//			}
//		}
//		AzureModule.webSiteConfigMap = null;
	}
}