/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.tooling.msservices.model.ws;

import java.io.Serializable;

import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public class WebSite implements ServiceTreeItem, Serializable {
	private static final long serialVersionUID = -5687551495552719808L;
    private boolean loading;
    private String name;
    private String webSpaceName;
    private String status = "";
    private String url = "";
    private String subscriptionId;
    private String location;
    private String serverFarm;

    // this field is here for lazy loading and caching; is null unless set explicitely
    private WebSitePublishSettings webSitePublishSettings;

    public WebSite(@NotNull String name, @NotNull String webSpaceName, @NotNull String subscriptionId, @NotNull String location) {
        this.name = name;
        this.webSpaceName = webSpaceName;
        this.subscriptionId = subscriptionId;
        this.location = location;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getWebSpaceName() {
        return webSpaceName;
    }

    @NotNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NotNull String status) {
        this.status = status;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    public void setUrl(@NotNull String url) {
        this.url = url;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @NotNull
    public String getLocation() {
    	return location;
    }

    public WebSitePublishSettings getWebSitePublishSettings() {
        return webSitePublishSettings;
    }

    public void setWebSitePublishSettings(WebSitePublishSettings webSitePublishSettings) {
        this.webSitePublishSettings = webSitePublishSettings;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }

	public String getServerFarm() {
		return serverFarm;
	}

	public void setServerFarm(String serverFarm) {
		this.serverFarm = serverFarm;
	}
}