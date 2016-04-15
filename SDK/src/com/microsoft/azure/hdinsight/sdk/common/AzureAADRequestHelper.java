package com.microsoft.azure.hdinsight.sdk.common;

import javax.net.ssl.HttpsURLConnection;
import com.microsoft.azure.hdinsight.sdk.common.RestServiceManager.*;
import java.io.IOException;

public class AzureAADRequestHelper {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public static String executeRequest(String managementUrl,
                                        String path,
                                        ContentType contentType,
                                        String method,
                                        String postData,
                                        String accessToken,
                                        RestServiceManager manager) throws IOException{
        HttpsURLConnectionProvider sslConnectionProvider = getHttpsURLConnectionProvider(accessToken, manager);

        return manager.executeRequest(managementUrl, path, contentType, method, postData, sslConnectionProvider);
    }

    private static HttpsURLConnectionProvider getHttpsURLConnectionProvider(
            final String accessToken,
            final RestServiceManager manager) throws IOException{
        return new HttpsURLConnectionProvider() {
            @Override
            public HttpsURLConnection getSSLConnection(String managementUrl,
                                                       String path,
                                                       ContentType contentType) throws IOException{
                HttpsURLConnection sslConnection = manager.getSSLConnection(managementUrl, path, contentType);
                sslConnection.addRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken);

                return sslConnection;
            }
        };
    }
}