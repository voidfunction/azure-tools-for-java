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


import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.ServicePrincipalAzureManager;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by shch on 10/9/2016.
 */
public class AuthMethodManager {

    private AuthMethodDetails authMethodDetails = null;
    private static AuthMethodManager instance = null;
    private AzureManager azureManager;
    
    private Set<Runnable> signInEventListeners = new HashSet<>();
    
    public void addSignInEventListener(Runnable l) {
        if (!signInEventListeners.contains(l)) {
        	signInEventListeners.add(l);
        }
    }

    private void notifySignInEventListener() {
        for (Runnable l : signInEventListeners) {
            l.run();
        }
    }

    public static AuthMethodManager getInstance() throws Exception {
        if( instance == null) {
            instance = new AuthMethodManager();
        }
        return instance;
    }

    public AzureManager getAzureManager() {
        return getAzureManager(getAuthMethod());
    }

    private AzureManager getAzureManager(AuthMethod authMethod) {
    	if (azureManager != null) return azureManager;
        switch (authMethod) {
            case AD:
                if (StringUtils.isNullOrEmpty(authMethodDetails.getAccountEmail())) {
                    return null;
                }
                azureManager = new AccessTokenAzureManager();
            case SP:
                String credFilePath = authMethodDetails.getCredFilePath();
                if (StringUtils.isNullOrEmpty(credFilePath)) {
                    return null;
                }
                Path filePath = Paths.get(credFilePath);
                if (!Files.exists(filePath)) {
                    INotification nw = CommonSettings.getUiFactory().getNotificationWindow();
                    nw.deliver("Credential File Error", "File doesn't exist: " + filePath.toString());
                    cleanAll();
                    return null;
                }
                azureManager = new ServicePrincipalAzureManager(new File(credFilePath));
        }
        return azureManager;
    }

    public void cleanAll() {
        try {
            if (azureManager != null) {
                azureManager.getSubscriptionManager().cleanSubscriptions();
                azureManager = null;
            }
            ServicePrincipalAzureManager.cleanPersist();
            authMethodDetails.setAccountEmail(null);
            authMethodDetails.setCredFilePath(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isSignedIn() {
        return getAzureManager() != null;
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails.getAuthMethod();
    }

    public void setAuthMethodDetails(AuthMethodDetails authMethodDetails) throws Exception {
        cleanAll();
        this.authMethodDetails = authMethodDetails;
        saveSettings();
        if (isSignedIn()) notifySignInEventListener();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    private AuthMethodManager() throws Exception {
        loadSettings();
    }

    private void loadSettings() throws Exception {
        System.out.println("loading authMethodDetails...");
        FileStorage fs = new FileStorage(CommonSettings.authMethodDetailsFileName, CommonSettings.settingsBaseDir);
        byte[] data = fs.read();
        String json = new String(data);
        if (json.isEmpty()) {
            System.out.println(CommonSettings.authMethodDetailsFileName + "file is empty");
            authMethodDetails = new AuthMethodDetails();
            return;
        }
        authMethodDetails = JsonHelper.deserialize(AuthMethodDetails.class, json);
    }

    private void saveSettings() throws Exception {
        System.out.println("saving authMethodDetails...");
        String sd = JsonHelper.serialize(authMethodDetails);
        FileStorage fs = new FileStorage(CommonSettings.authMethodDetailsFileName, CommonSettings.settingsBaseDir);
        fs.write(sd.getBytes(Charset.forName("utf-8")));
    }
}
