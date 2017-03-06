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
package com.microsoft.azure.hdinsight.sdk.storage.adls;

import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WebHDFSUtils {
    private static String getUserAgent() {
        final String installID = HDInsightLoader.getHDInsightHelper().getInstallationId();
        final String userAgentSource = WebHDFSUtils.class.getClassLoader().getClass().getName().toLowerCase().contains("intellij")
                ? "Azure Toolkit for IntelliJ" : "Azure Toolkit for Eclipse";
        return userAgentSource + installID;
    }

    public static final String ADLS_REST_API_PATH = "https://{store.name}.azuredatalakestore.net/webhdfs/v1/{store.path}?op={store.operator}";

    public static final HttpClient ADLS_CLIENT = HttpClients.custom().setUserAgent(getUserAgent()).build();

    public static void uploadFileToADLS(@NotNull IHDIStorageAccount storageAccount, @NotNull File localFile, @NotNull String remotePath, boolean overWrite) throws Exception {

        String commonRestUrl = ADLS_REST_API_PATH.replace(UrlConfEnum.STORENAME.toString(), storageAccount.getName())
                .replace(UrlConfEnum.STOREPATH.toString(), remotePath);

        HttpResponse createResponse = createFileOnADLS(getAccessToken(storageAccount), getUrlWithOperatorConf(commonRestUrl, RestOperatorEnum.CREATE));
        int createResponseCode = createResponse.getStatusLine().getStatusCode();
        if(createResponseCode == 403) {
            throw new HDIException("Forbidden. Attached Azure DataLake Store is not supported in Automated login model. Please logout first and try Interactive login model", createResponseCode);
        }

        if(createResponseCode != 200 && createResponseCode != 201) {
            throw new HDIException(createResponse.getStatusLine().getReasonPhrase(), createResponseCode);
        }

        HttpResponse appendResponse = appendFileToADLS(getAccessToken(storageAccount), getUrlWithOperatorConf(commonRestUrl, RestOperatorEnum.APPEND), localFile);
        int appendResponseCode = appendResponse.getStatusLine().getStatusCode();
        if(appendResponseCode != 200 && appendResponseCode != 201) {
            throw new HDIException(appendResponse.getStatusLine().getReasonPhrase(), appendResponseCode);
        }
    }

    private static String getUrlWithOperatorConf(String restUrl, RestOperatorEnum operatorEnum) {
        if(operatorEnum == RestOperatorEnum.CREATE) {
            restUrl = restUrl.replace(UrlConfEnum.STOREOPERATOR.toString(), RestOperatorEnum.CREATE.toString());
            restUrl = restUrl + "&overwrite=true&write=true";
        } else {
            restUrl = restUrl.replace(UrlConfEnum.STOREOPERATOR.toString(), RestOperatorEnum.APPEND.toString());
            restUrl = restUrl + "&append=true";
        }
        return restUrl;
    }

    private static HttpResponse createFileOnADLS(@NotNull String accessToken, String url) throws IOException {
        HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader("Content-Type ", "application/json");
        httpPut.addHeader("Authorization", "Bearer " + accessToken);
        return ADLS_CLIENT.execute(httpPut);
    }

    private static HttpResponse appendFileToADLS(@NotNull String accessToken, String url, File localFile) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Authorization", "Bearer " + accessToken);
        httpPost.setEntity(new InputStreamEntity(new FileInputStream(localFile), ContentType.APPLICATION_OCTET_STREAM));
        return ADLS_CLIENT.execute(httpPost);
    }

    private static String getAccessToken(@NotNull IHDIStorageAccount storageAccount) throws Exception {
        com.microsoft.azuretools.sdkmanage.AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
        String tid = manager.getSubscriptionManager().getSubscriptionTenant(storageAccount.getSubscriptionId());
        return manager.getAccessToken(tid);
    }

     enum UrlConfEnum {
        STORENAME("{store.name}"),
        STOREPATH("{store.path}"),
        STOREOPERATOR("{store.operator}");

        private final String name;
        UrlConfEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
