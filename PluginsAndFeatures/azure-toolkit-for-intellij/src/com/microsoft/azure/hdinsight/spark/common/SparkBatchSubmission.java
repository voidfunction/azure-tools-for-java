package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class SparkBatchSubmission {

    private static final String UserAgentName = "HDInsight Tools for IntelliJ 1.0";

    // Singleton Instance
    private static SparkBatchSubmission instance = null;

    public static SparkBatchSubmission getInstance() {
        if(instance == null){
            synchronized (SparkBatchSubmission.class){
                if(instance == null){
                    instance = new SparkBatchSubmission();
                }
            }
        }

        return instance;
    }

    private CredentialsProvider credentialsProvider =  new BasicCredentialsProvider();

    /**
     * Set http request credential using username and password
     * @param username : username
     * @param password : password
     */
    public void setCredentialsProvider(String username, String password){
        credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(username, password));
    }

    /**
     * get all batches spark jobs
     * @param connectUrl : eg http://localhost:8998/batches
     * @return response result
     * @throws IOException
     */
    public HttpResponse getAllBatchesSparkJobs(String connectUrl)throws IOException{
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();

        HttpGet httpGet = new HttpGet(connectUrl);
        httpGet.addHeader("Content-Type", "application/json");
        try(CloseableHttpResponse response = httpclient.execute(httpGet)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * create batch sp  ark job
     * @param connectUrl : eg http://localhost:8998/batches
     * @param submissionParameter : spark submission parameter
     * @return response result
     */
    public HttpResponse createBatchSparkJob(String connectUrl, SparkSubmissionParameter submissionParameter)throws IOException{
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        HttpPost httpPost = new HttpPost(connectUrl);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("User-Agent", UserAgentName);
        StringEntity postingString =new StringEntity(submissionParameter.serializeToJson());
        httpPost.setEntity(postingString);
        try(CloseableHttpResponse response = httpclient.execute(httpPost)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * get batch spark job status
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse getBatchSparkJobStatus(String connectUrl, int batchId)throws IOException{
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        HttpGet httpGet = new HttpGet(connectUrl + "/" + batchId);
        httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("User-Agent", UserAgentName);
        try(CloseableHttpResponse response = httpclient.execute(httpGet)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * kill batch job
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse killBatchJob(String connectUrl, int batchId)throws IOException {
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        HttpDelete httpDelete = new HttpDelete(connectUrl +  "/" + batchId);
        httpDelete.addHeader("User-Agent", UserAgentName);
        httpDelete.addHeader("Content-Type", "application/json");

        try(CloseableHttpResponse response = httpclient.execute(httpDelete)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }

    /**
     * get batch job full log
     * @param connectUrl : eg http://localhost:8998/batches
     * @param batchId : batch Id
     * @return response result
     * @throws IOException
     */
    public HttpResponse getBatchJobFullLog(String connectUrl, int batchId)throws IOException{
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        HttpGet httpGet = new HttpGet(String.format("%s/%d/log?from=%d&size=%d", connectUrl,batchId,0,Integer.MAX_VALUE));
        httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("User-Agent", UserAgentName);
        try(CloseableHttpResponse response = httpclient.execute(httpGet)) {
            return StreamUtil.getResultFromHttpResponse(response);
        }
    }
}
