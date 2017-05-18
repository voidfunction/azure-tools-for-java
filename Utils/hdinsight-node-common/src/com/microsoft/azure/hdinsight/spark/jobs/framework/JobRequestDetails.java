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
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JobRequestDetails implements IRequest {

    private final Map<String, String> myQueriesMap;
    private final IClusterDetail myClusterDetail;
    private final String myRequestUrl;
    private final HttpRequestType myHttpRequestType;
    private final String myAppId;
    private final String myQueryString;

    private static final String HTTP_HEADER = "http-type";
    private static final String CLUSTER_NAME_HEADER = "cluster-name";
    private static final String APP_ID_QUERY_KEY = "appId";

    private JobRequestDetails (@NotNull String requestUrl, @Nullable String queryString,
                               @NotNull String clusterName, @NotNull HttpRequestType requestType) {
        this.myClusterDetail = JobViewManager.getCluster(clusterName);
        this.myRequestUrl = requestUrl;
        this.myQueryString = queryString;
        this.myQueriesMap = new HashMap<>();
        this.myHttpRequestType = requestType;

        String[] queries = StringHelper.isNullOrWhiteSpace(queryString) ? new String[0] : queryString.split("&");
        Arrays.stream(queries).forEach(str -> {
            String[] query = str.split("=");
            if (query.length == 2) {
                myQueriesMap.put(query[0], query[1]);
            }
        });
        this.myAppId = myQueriesMap.getOrDefault(APP_ID_QUERY_KEY, "0");
    }

    public static JobRequestDetails getJobRequestDetail(@NotNull HttpExchange httpExchange) {
        final Headers headers = httpExchange.getRequestHeaders();
        final String httpType = headers.getFirst(HTTP_HEADER);
        final String clusterName = headers.getFirst(CLUSTER_NAME_HEADER);
        HttpRequestType requestType = HttpRequestType.fromString(httpType);

        final URI myUri = httpExchange.getRequestURI();

        final String path = myUri.getPath();

        return new JobRequestDetails(path, myUri.getQuery(), clusterName, requestType);
    }

    @Override
    public String getRequestUrl() {
        String url = myClusterDetail.getConnectionUrl();

        switch (myHttpRequestType) {
            case SparkRest:
                url += "/sparkhistory/api/v1";
                break;
            case YarnRest:
                url += "/yarnui/ws/v1";
                break;
            case YarnHistory:
                url += "/yarnui";
                break;
            case LivyBatchesRest:
                url += "/livy";
                break;
        }
        url += myRequestUrl;
        return StringHelper.isNullOrWhiteSpace(myQueryString) ? url : url + "?" + myQueryString;
    }

    @Override
    public IClusterDetail getCluster() {
        return myClusterDetail;
    }

    @Override
    public HttpRequestType getRestType() {
        return myHttpRequestType;
    }
}
