package com.microsoft.azure.hdinsight.sdk.common;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;

public interface RestServiceManager {
    interface HttpsURLConnectionProvider {

        HttpsURLConnection getSSLConnection(String managementUrl,
                                            String path,
                                            ContentType contentType) throws IOException;
    }

    enum ContentType {

        Json("application/json"),
        Xml("application/xml"),
        Text("text/plain");


        private final String value;

        ContentType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    String executeRequest(String managementUrl,
                          String path,
                          ContentType contentType,
                          String method,
                          String postData,
                          HttpsURLConnectionProvider sslConnectionProvider) throws IOException;


    HttpsURLConnection getSSLConnection(String managementUrl,
                                        String path,
                                        ContentType contentType) throws IOException;
}