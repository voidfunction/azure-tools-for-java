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
package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public class ADLSStorageAccount implements IHDIStorageAccount, ServiceTreeItem {
    private String name;
    private boolean isDefaultStorageAccount;
    private String defaultRootFolderPath;
    private IClusterDetail clusterDetail;

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void setLoading(boolean loading) {

    }

    @Override
    public String getSubscriptionId() {
        return this.clusterDetail.getSubscription().getSubscriptionId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StorageAccountTypeEnum getAccountType() {
        return StorageAccountTypeEnum.ADLS;
    }

    @Override
    public String getDefaultContainerOrRootPath() {
        return defaultRootFolderPath;
    }

    public ADLSStorageAccount(IClusterDetail clusterDetail, String name, boolean isDefault, String defaultRootPath) {
//        super(name.replace(".blob.core.windows.net", ""));
        this.name = name;
        this.isDefaultStorageAccount = isDefault;
        this.defaultRootFolderPath = defaultRootPath;
        this.clusterDetail = clusterDetail;
    }
}
