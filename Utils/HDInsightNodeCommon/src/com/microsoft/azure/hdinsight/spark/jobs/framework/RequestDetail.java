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
package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RequestDetail {
    private final String clusterFormatId;
    private final String restUrl;
    private final APIType apiType;
    private IClusterDetail clusterDetail;

    private Map<String, String> queriesMap = new HashMap<String, String>();

    public enum APIType {
        SparkRest,
        YarnRest,
        YarnHistory,
        LivyBatchesRest
    }

    public RequestDetail(@NotNull String clusterFormatId, @NotNull String restUrl, @Nullable String[] queries) {
        this.clusterFormatId = clusterFormatId;
        clusterDetail = JobViewManager.getCluster(clusterFormatId);

        this.restUrl = restUrl;
        if (queries != null) {
            for (String str : queries) {
                String[] conditions = str.split("=");
                if (conditions.length == 2) {
                    this.queriesMap.put(conditions[0], conditions[1]);
                }
            }
        }

        if (queriesMap.containsKey("restType")) {
            String type = queriesMap.get("restType");
            if (type.equalsIgnoreCase("yarn")) {
                apiType = APIType.YarnRest;
            } else if (type.equalsIgnoreCase("yarnhistory")) {
                apiType = APIType.YarnHistory;
            } else if (type.equalsIgnoreCase("livy")) {
                apiType = APIType.LivyBatchesRest;
            } else {
                apiType = APIType.SparkRest;
            }
        } else {
            apiType = APIType.SparkRest;
        }
    }

    public IClusterDetail getClusterDetail() {
        return JobViewManager.getCluster(clusterFormatId);
    }

    public String getClusterFormatId() {
        return clusterFormatId;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public APIType getApiType() {
        return apiType;
    }

    @Nullable
    public String getProperty(@NotNull String key) {
        return queriesMap.get(key);
    }
}