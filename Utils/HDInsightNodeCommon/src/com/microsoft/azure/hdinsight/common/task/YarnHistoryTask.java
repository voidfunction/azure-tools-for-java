package com.microsoft.azure.hdinsight.common.task;


import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.tooling.msservices.helpers.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class YarnHistoryTask extends Task<String> {

    protected final IClusterDetail clusterDetail;
    protected final String path;
    private final CredentialsProvider credentialsProvider =  new BasicCredentialsProvider();

    public YarnHistoryTask(@NotNull IClusterDetail clusterDetail, @NotNull String path, @NotNull FutureCallback<String> callback) {
        super(callback);
        this.clusterDetail = clusterDetail;
        this.path = path;
        try {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
        } catch (HDIException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String call() throws Exception {
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        HttpGet httpGet = new HttpGet(path);
        httpGet.addHeader("Content-Type", "text/html");

        CloseableHttpResponse response = httpclient.execute(httpGet);

        HttpEntity httpEntity = response.getEntity();

        return IOUtils.toString(httpEntity.getContent());
    }
}
