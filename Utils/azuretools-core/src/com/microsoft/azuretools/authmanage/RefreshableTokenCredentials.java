/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentialsInterceptor;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.adauth.PromptBehavior;

import okhttp3.OkHttpClient;

/**
 * Created by vlashch on 11/9/16.
 */
//public class RefreshableTokenCredentials extends TokenCredentials implements AzureTokenCredentials {
public class RefreshableTokenCredentials implements AzureTokenCredentials {
    private final static Logger LOGGER = Logger.getLogger(RefreshableTokenCredentials.class.getName());
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
       } catch (URISyntaxException | InterruptedException | ExecutionException | AuthException e) {
            System.out.println("=== getToken@RefreshableTokenCredentials exception: " + e.getMessage());
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new IOException(e);
        }
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
