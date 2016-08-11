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
package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.spark.jobs.SparkJobInfo;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobViewPanel;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class JobViewManager {
    private static Map<String, Pair<IClusterDetail, JobViewPanel>> jobViewPanelMap = new HashMap<String, Pair<IClusterDetail, JobViewPanel>>();
    private static Map<String, SparkJobInfo> jobInfoMap = new HashMap<String, SparkJobInfo>();

    public static void registerJobViewInfo(@NotNull String clusterName, @NotNull SparkJobInfo info) {
        synchronized (jobInfoMap) {
            jobInfoMap.put(clusterName, info);
        }
    }

    @Nullable
    public static SparkJobInfo getJobViewInfo(@NotNull String clusterName) {
        return jobInfoMap.get(clusterName);
    }

    public static void registerJovViewNode(@NotNull String uuid, @NotNull IClusterDetail clusterDetail) {
        synchronized(jobViewPanelMap) {
            jobViewPanelMap.put(uuid, new Pair<IClusterDetail, JobViewPanel>(clusterDetail,null));
        }
    }

    @Nullable
    public static IClusterDetail getCluster(@NotNull String uuid) {
        if(!jobViewPanelMap.containsKey(uuid)) {
            return null;
        }
        return jobViewPanelMap.get(uuid).getKey();
    }

    public static void unRegisterJobView(@NotNull String uuid) {
        synchronized(jobViewPanelMap) {
            jobViewPanelMap.remove(uuid);
        }
    }

}
