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
package com.microsoft.azure.hdinsight.sdk.jobs;

import com.microsoft.azure.hdinsight.sdk.jobs.spark.Application;
import com.microsoft.azure.hdinsight.sdk.jobs.spark.executor.Executor;
import com.microsoft.azure.hdinsight.sdk.jobs.spark.job.Job;
import com.microsoft.azure.hdinsight.sdk.jobs.spark.stage.Stage;
import com.microsoft.azure.hdinsight.sdk.jobs.yarn.ClusterInfo;
import com.microsoft.azure.hdinsight.sdk.jobs.yarn.MyClusterInfo;
import com.microsoft.azure.hdinsight.sdk.jobs.yarn.YarnApplications;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by ltian on 5/6/2017.
 */
public class MyHttpClient {
    private CredentialsProvider provider;
    private HttpClient client;
    private Cluster cluster;

    public MyHttpClient(Cluster cluster) {
        this.cluster = cluster;
        provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "Pa$$w0rd1234"));
        client = HttpClients.custom().setDefaultCredentialsProvider(provider).build();
    }

    public Optional<Executor[]> getExecutors(AttemptWithAppId attemptWithAppId) {
        return getInformationFromSpark(attemptWithAppId, Executor[].class, "executors");
    }

    private <T> Optional<T> getFromYarn(String path, Class<T> tClass) {
        String url = String.format("https://%s.azurehdinsight.net/yarnui/ws/v1/%s", cluster.getClusterName(), path);

        return getInformation(url, tClass);
    }

    public Optional<ClusterInfo> getClusterInfo() {
        Optional<MyClusterInfo> myClusterInfo = getFromYarn("cluster", MyClusterInfo.class);
        return myClusterInfo.isPresent() ? Optional.ofNullable(myClusterInfo.get().getClusterInfo()) : Optional.empty();
    }

    public Optional<YarnApplications> getYarnApplication() {
        Optional<YarnApplications> apps = getFromYarn("cluster/apps", YarnApplications.class);
        return apps;
    }
    private <T> Optional<T> getInformation(String url, Class<T> tClass) {
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            int code = statusLine.getStatusCode();
            if(code == 200 || code == 201) {
                String json = EntityUtils.toString(response.getEntity());
                final String type = response.getHeaders("Content-Type")[0].getValue();
                return type.equalsIgnoreCase("application/xml") ? ObjectConvertUtils.convertXmlToObject(json, tClass) : ObjectConvertUtils.convertJsonToObject(json, tClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private <T> Optional<T> getInformationFromSpark(AttemptWithAppId attemptWithAppId, Class<T> tClass, String type) {
        final String url = String.format("https://%s.azurehdinsight.net/sparkhistory/api/v1/applications/%s/%s/%s",
                cluster.getClusterName(), attemptWithAppId.getAppId(), attemptWithAppId.getAttemptId(), type);
        return getInformation(url, tClass);
    }

    public Optional<Stage[]> getStages(AttemptWithAppId attemptWithAppId) {
        return getInformationFromSpark(attemptWithAppId, Stage[].class, "stages");
    }
    ///applications/[app-id]/stages
    public Optional<Job[]> getJobs(AttemptWithAppId attemptWithAppId) {
        return getInformationFromSpark(attemptWithAppId, Job[].class, "jobs");
    }

    public Optional<Application[]> getAllApplication() {
        String url = String.format("https://%s.azurehdinsight.net/sparkhistory/api/v1/applications", cluster.getClusterName());
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            int code = statusLine.getStatusCode();
            if(code == 200 || code == 201) {
                String json = EntityUtils.toString(response.getEntity());
                return ObjectConvertUtils.convertJsonToObject(json, Application[].class);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}