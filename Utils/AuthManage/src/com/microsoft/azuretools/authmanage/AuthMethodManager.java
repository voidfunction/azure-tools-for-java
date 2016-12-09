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

import static com.microsoft.azuretools.authmanage.CommonSettings.*;

/**
 * Created by shch on 10/9/2016.
 */
public class AuthMethodManager {

    private AuthMethodDetails authMethodDetails = null;
    private static AuthMethodManager instance = null;
    private AccessTokenAzureManager accessTokenAzureManager;
    private ServicePrincipalAzureManager servicePrincipalAzureManager;

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
        switch (authMethod) {
            case AD:
                if (StringUtils.isNullOrEmpty(authMethodDetails.getAccountEmail())) {
                    return null;
                }
                if (accessTokenAzureManager == null) {
                    accessTokenAzureManager = new AccessTokenAzureManager();
                }
                return accessTokenAzureManager;
            case SP:
                String credFilePath = authMethodDetails.getCredFilePath();
                if (StringUtils.isNullOrEmpty(credFilePath)) {
                    return null;
                }
                Path filePath = Paths.get(credFilePath);
                if (!Files.exists(filePath)) {
                    INotification nw = uiFactory.getNotificationWindow();
                    nw.deliver("Auth method is not set", "File doesn't exist: " + filePath.toString());
                    return null;
                }
                if (servicePrincipalAzureManager == null) {
                    servicePrincipalAzureManager = new ServicePrincipalAzureManager(new File(credFilePath));
                }
                return servicePrincipalAzureManager;
        }
        return null;
    }
/*
    private void cleanAzureManager() throws Exception {
        System.out.println("AuthMethodManager.cleanAzureManager()");
        cleanAzureManager(getAuthMethod());
    }

    private void cleanAzureManager(AuthMethod authMethod) throws Exception {

        switch (authMethod) {
            case AD:
                accessTokenAzureManager = null;
                authMethodDetails.setAuthMethod(null);
//                if (accessTokenAzureManager != null) {
//                    accessTokenAzureManager.drop();
//                    accessTokenAzureManager = null;
//                }
            case SP:
                servicePrincipalAzureManager = null;
                ServicePrincipalAzureManager.cleanPersist();
                authMethodDetails.setCredFilePath(null);
//                if (servicePrincipalAzureManager != null) {
//                    System.out.println("dropping servicePrincipalAzureManager...");
//                    servicePrincipalAzureManager.drop();
//                    servicePrincipalAzureManager = null;
//                };
        }
    }
*/
    public void cleanAll() throws Exception {
        accessTokenAzureManager = null;
        servicePrincipalAzureManager = null;
        // AD sign out should be done outside if needed
        ServicePrincipalAzureManager.cleanPersist();
        authMethodDetails.setAccountEmail(null);
        authMethodDetails.setCredFilePath(null);
    }

    public boolean isSignedIn() {
        return getAzureManager() != null;
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails.getAuthMethod();
    }

    public void setAuthMethodDetails(AuthMethodDetails authMethodDetails) throws Exception {
        //dropAzureManager();
        cleanAll();
        this.authMethodDetails = authMethodDetails;
        // clean previous manager of the same type
        saveSettings();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    private AuthMethodManager() throws Exception {
        loadSettings();
    }

    private void loadSettings() throws Exception {
        System.out.println("loading authMethodDetails...");
        FileStorage fs = new FileStorage(authMethodDetailsFileName, settingsBaseDir);
        byte[] data = fs.read();
        String json = new String(data);
        if (json.isEmpty()) {
            System.out.println(authMethodDetailsFileName + "file is empty");
            authMethodDetails = new AuthMethodDetails();
            return;
        }
        authMethodDetails = JsonHelper.deserialize(AuthMethodDetails.class, json);
    }

    private void saveSettings() throws Exception {
        System.out.println("saving authMethodDetails...");
        String sd = JsonHelper.serialize(authMethodDetails);
        FileStorage fs = new FileStorage(authMethodDetailsFileName, settingsBaseDir);
        fs.write(sd.getBytes(Charset.forName("utf-8")));
    }
}
