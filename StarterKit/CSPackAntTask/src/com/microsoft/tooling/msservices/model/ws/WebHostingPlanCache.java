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

import com.microsoft.azure.management.websites.models.SkuOptions;
import com.microsoft.azure.management.websites.models.WorkerSizeOptions;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public class WebHostingPlanCache implements ServiceTreeItem {
    private boolean loading;
    private String name;
    private String resGrpName;
    private String subscriptionId;
    private String location;
    private SkuOptions sku;
    private WorkerSizeOptions workerSize;

    public WebHostingPlanCache(@NotNull String name, @NotNull String resGrpName,
    		@NotNull String subscriptionId, @NotNull String location,
    		@NotNull SkuOptions sku, @NotNull WorkerSizeOptions workerSize) {
    	this.name = name;
    	this.resGrpName = resGrpName;
    	this.subscriptionId = subscriptionId;
    	this.location = location;
    	this.sku = sku;
    	this.workerSize = workerSize;
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
    public String getSubscriptionId() {
        return subscriptionId;
    }
    
    @NotNull
	public String getLocation() {
		return location;
	}
	
    @NotNull
	public String getResGrpName() {
		return resGrpName;
	}
    
    @NotNull
	public SkuOptions getSku() {
		return sku;
	}
    
    @NotNull
	public WorkerSizeOptions getWorkerSize() {
		return workerSize;
	}

	@Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}