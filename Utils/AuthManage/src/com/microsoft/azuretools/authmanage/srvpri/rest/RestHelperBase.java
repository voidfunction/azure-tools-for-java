package com.microsoft.azuretools.authmanage.srvpri.rest;


import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by vlashch on 8/29/16.
 */
public abstract class RestHelperBase {

    private IRequestFactory requestFactory = null;
    protected IRequestFactory getRequestFactory() {
        return requestFactory;
    }
    protected void setRequestFactory(IRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

//    private static final String urlPrefix = Constants.resourceGraph + Constants.tenant + "/";
//    private static final String apiVersion = "api-version=1.6";
//
//    /*
//        https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/servicePrincipals?$filter=servicePrincipalNames/any(c:c%20eq%20'425817b6-1cd8-48ea-9cd7-1b9472912bee')&api-version=1.6
//        %s?%s& -> servicePrincipals?$filter=servicePrincipalNames/any(c:c%20eq%20'425817b6-1cd8-48ea-9cd7-1b9472912bee')&
//        */
//    private static final String urlPattern = urlPrefix + "%s?%s&" + apiVersion;
//
//    /*
//        https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/applications?api-version=1.6
//        %s? -> applications?
//
//        https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/servicePrincipals/f144ba9d-f4af-48b8-a992-3e328f2710b9?api-version=1.6
//        %s? -> servicePrincipals/f144ba9d-f4af-48b8-a992-3e328f2710b9?
//        */
//    private static final String urlPatternParamless = urlPrefix + "%s?" + apiVersion;


    protected static String action(IRequestFactory factory, String verb, String request, String params, String body) throws Exception {
        String urlSrtring = (params == null)
                ? String.format(factory.getUrlPatternParamless(), request)
                : String.format(factory.getUrlPattern(), request, params);

        URL url = new URL(urlSrtring);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.addRequestProperty("User-Agent", "shch");
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Authorization", "Bearer " + factory.getAccessToken());

        conn.setRequestMethod(verb);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        if(body != null) {
            conn.addRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream output = conn.getOutputStream();
            try {
                output.write(body.getBytes());
            } finally {
                output.close();
            }
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
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
            } finally {
                if(errorStream != null) {
                    errorStream.close();
                }
            }
            String errString = err.toString();
            // sometimes error stream starts wiht unacceptable symbols and jackson json fails to parse it.
            int start = errString.indexOf("{");
            if (start != 0) {
                errString = errString.substring(start);
            }
            System.out.println("Response: " + errString);
            throw factory.newAzureException(errString);
        }

        InputStream resposeBodyStream = null;

        try {
            resposeBodyStream = conn.getInputStream();
            if(resposeBodyStream == null) return null;

            java.io.BufferedReader br = new java.io.BufferedReader(new InputStreamReader(resposeBodyStream));
            StringBuilder resposeBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                resposeBody.append(line);
            }
            System.out.println("Response: " + resposeBody.toString());

            return resposeBody.length() == 0 ? null : resposeBody.toString();

        } finally {
            resposeBodyStream.close();
        }
    }

    public String doGet(String request, String params) throws Exception{
        return action(getRequestFactory(), "GET", request, params, null);
    }

    public String doPost(String request, String params, String body) throws Exception {
        return action(getRequestFactory(), "POST", request, params, body);
    }

    public String doPut(String request, String params, String body) throws Exception {
        return action(getRequestFactory(), "PUT", request, params, body);
    }

    public String doDelete(String request, String params, String body) throws Exception {
        return action(getRequestFactory(), "DELETE", request, params, body);
    }
}