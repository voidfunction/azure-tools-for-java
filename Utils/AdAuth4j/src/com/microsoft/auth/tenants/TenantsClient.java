package com.microsoft.auth.tenants;

import com.microsoft.auth.JsonHelper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by vlashch on 5/13/16.
 */
public class TenantsClient {
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String X_MS_VERSION_HEADER = "x-ms-version";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String USER_AGENT = "auth4j";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String managementUrl = "https://management.azure.com/tenants?api-version=2015-11-01";

    public static List<Tenant> getByToken(String accessToken) throws Exception {
        URL url = new URL(managementUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.addRequestProperty(USER_AGENT_HEADER, USER_AGENT);
        conn.addRequestProperty(ACCEPT_HEADER, CONTENT_TYPE_JSON);
        conn.addRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken);

        conn.setRequestMethod("GET");
        conn.setDoOutput(false);

        int statusCode = conn.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = null;
            InputStreamReader errorReader = null;
            StringBuilder err = new StringBuilder();
            try {
                errorStream = conn.getErrorStream();
                errorReader = new InputStreamReader(errorStream);

                int data;
                while((data = errorReader.read()) != -1) {
                    err.append((char)data);
                }
            }
            finally {
                if(errorStream != null) {
                    errorStream.close();
                }
                if(errorReader != null) {
                    errorReader.close();
                }
            }

            throw new IOException("Azure returned HTTP status code " +
                    Integer.toString(statusCode) + ". Error info: " + err.toString());
        }

        InputStream resposeBodyStream = null;

        try {
            resposeBodyStream = conn.getInputStream();
            Tenants t = JsonHelper.deserialize(Tenants.class, resposeBodyStream);
            return t.getTenants();

        }   finally {
            resposeBodyStream.close();
        }
    }


}
