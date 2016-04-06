package com.microsoft.azure.hdinsight.sdk.common;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RestServiceManagerBaseImpl implements RestServiceManager {

    public String executeRequest(String managementUrl,
                                 String path,
                                 ContentType contentType,
                                 String method,
                                 String postData,
                                 HttpsURLConnectionProvider sslConnectionProvider) throws IOException {

            HttpsURLConnection sslConnection = sslConnectionProvider.getSSLConnection(managementUrl, path, contentType);
            HttpResponse response = getResponse(method, postData, sslConnection);
            return response.getContent();
    }

    public HttpsURLConnection getSSLConnection(String managementUrl,
                                               String path,
                                               ContentType contentType) throws IOException  {

        String connectionUrl = String.format("%s/%s", managementUrl.replaceAll("/+$", ""), path.replaceAll("^/+", ""));
        URL myUrl = new URL(connectionUrl);
        HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
        return conn;
    }

    protected static HttpResponse getResponse(final String method,
                                              final String postData,
                                              HttpsURLConnection sslConnection)
            throws IOException {
        try {
            sslConnection.setRequestMethod(method);
            sslConnection.setDoOutput(postData != null);

            if (postData != null) {
                try(DataOutputStream wr = new DataOutputStream(sslConnection.getOutputStream())){
                    wr.writeBytes(postData);
                    wr.flush();
                }
            }

            return getResponse(sslConnection);
        } finally {
            sslConnection.disconnect();
        }
    }

    private static HttpResponse getResponse(HttpsURLConnection sslConnection)
            throws IOException {
        int code = sslConnection.getResponseCode();
        String message = Strings.nullToEmpty(sslConnection.getResponseMessage());
        Map<String, List<String>> headers = sslConnection.getHeaderFields();

        if (headers == null) {
            headers = new HashMap<>();
        }

        InputStream is;
        String content;

        try {
            is = sslConnection.getInputStream();
        } catch (IOException e) {
            is = sslConnection.getErrorStream();
        }

        if (is != null) {
            content = readStream(is);
        } else {
            content = "";
        }

        return new HttpResponse(code, message, headers, content);
    }

    private static String readStream(InputStream is) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))){
            return CharStreams.toString(in);
        }
    }
}