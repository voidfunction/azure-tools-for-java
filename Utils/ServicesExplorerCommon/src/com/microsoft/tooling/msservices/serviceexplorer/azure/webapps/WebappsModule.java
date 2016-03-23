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

import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureRefreshableNode;

import java.util.List;

public class WebappsModule extends AzureRefreshableNode {
	private static final String WEBAPPS_MODULE_ID = WebappsModule.class.getName();
	private static final String WEB_RUN_ICON = "website.png";
	private static final String WEB_STOP_ICON = "stopWebsite.png";
	private static final String BASE_MODULE_NAME = "Web Apps";
	String runStatus = "Running";

	public WebappsModule(Node parent) {
		super(WEBAPPS_MODULE_ID, BASE_MODULE_NAME, parent, WEB_RUN_ICON);
	}

	@Override
	protected void refresh(@NotNull EventHelper.EventStateHandle eventState)
			throws AzureCmdException {
		if (eventState.isEventTriggered()) {
			return;
		}

		removeAllChildNodes();

		List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();

		for (Subscription subscription : subscriptionList) {
			for (final String webSpace : AzureManagerImpl.getManager().getResourceGroupNames(subscription.getId())) {
				List<WebSite> webapps = AzureManagerImpl.getManager().getWebSites(subscription.getId(), webSpace);
				for (WebSite webapp : webapps) {
					if (webapp.getStatus().equalsIgnoreCase(runStatus)) {
						addChildNode(new WebappNode(this, webapp, WEB_RUN_ICON));
					} else {
						addChildNode(new WebappNode(this, webapp, WEB_STOP_ICON));
					}
				}
			}
		}
	}
}