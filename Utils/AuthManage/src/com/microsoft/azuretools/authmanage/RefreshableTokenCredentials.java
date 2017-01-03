package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentialsInterceptor;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.rest.credentials.TokenCredentials;
import okhttp3.OkHttpClient;

import java.io.IOException;

/**
 * Created by vlashch on 11/9/16.
 */
//public class RefreshableTokenCredentials extends TokenCredentials implements AzureTokenCredentials {
public class RefreshableTokenCredentials implements AzureTokenCredentials {

    private AdAuthManager authManager;
    private String tid;

    /**
     * Initializes a new instance of the TokenCredentials.
     *
     * @param authManager authz/auth manager
     * @param tid  tenant ID
     */
    public RefreshableTokenCredentials(AdAuthManager authManager, String tid) {
//        super(null, null);
        this.authManager = authManager;
        this.tid = tid;
    }

//    @Override
//    public String getToken() {
//        try {
//            System.out.println("RefreshableTokenCredentials: getToken()");
//            return authManager.getAccessToken(tid);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return null;
//    }

    @Override
    public String getToken(String s) throws IOException {
        try {
            //System.out.println("RefreshableTokenCredentials: getToken()");
            return authManager.getAccessToken(tid, s, PromptBehavior.Auto);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public String getDomain() {
        return tid;
    }

    @Override
    public AzureEnvironment getEnvironment() {
        return AzureEnvironment.AZURE;
    }

    @Override
    public void applyCredentialsFilter(OkHttpClient.Builder clientBuilder) {
        clientBuilder.addInterceptor(new AzureTokenCredentialsInterceptor(this));
    }
}
