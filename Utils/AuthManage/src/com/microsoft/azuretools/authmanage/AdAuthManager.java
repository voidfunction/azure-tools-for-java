package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azuretools.adauth.*;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.models.AdAuthDetails;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AdAuthManager {

    private final TokenCache cache;
    private TokenFileStorage tokenFileStorage;
    private static AdAuthManager instance = null;
    //private static String adAuthSettingsFileName = "AdAuthDetails.json";
    private static AdAuthDetails adAuthDetails = new AdAuthDetails();
    //private static Map<String, List<String>> tidToSidsMap = new HashMap<>();

    public static AdAuthManager getInstance() throws Exception {
        if( instance == null) {
            AuthContext.setUserDefinedWebUi(CommonSettings.uiFactory.getWebUi());
            instance = new AdAuthManager(false);
            // load accountEmail
//            instance.loadSettings();
        }
        return instance;
    }

    public String getAccessToken(String tid) throws Exception {
        AuthContext ac = new AuthContext(String.format("%s/%s", Constants.authority, tid), cache);
        AuthenticationResult result = ac.acquireToken(Constants.resourceARM, Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, null);
        return result.getAccessToken();
    }

    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws Exception {
        AuthContext ac = new AuthContext(String.format("%s/%s", Constants.authority, tid), cache);
        AuthenticationResult result = ac.acquireToken(resource, Constants.clientId, Constants.redirectUri, promptBehavior, null);
        return result.getAccessToken();
    }

    public AuthenticationResult signIn() throws Exception {

        // build token cache for azure and graph api
        // using azure sdk directly

        cleanCache();
        String commonTid = "common";
        AuthContext ac = new AuthContext(String.format("%s/%s", Constants.authority, commonTid), cache);

        AuthenticationResult result = ac.acquireToken(Constants.resourceARM, Constants.clientId, Constants.redirectUri, PromptBehavior.Always, null);
        String displayableId = result.getUserInfo().getDisplayableId();
        UserIdentifier uid = new UserIdentifier(displayableId, UserIdentifierType.RequiredDisplayableId);

        Map<String, List<String>> tidToSidsMap = new HashMap<>();
//        List<Tenant> tenants = AccessTokenAzureManager.authTid(commonTid).tenants().list();
        List<Tenant> tenants = AccessTokenAzureManager.getTenants(commonTid);
        for (Tenant t : tenants) {
            String tid = t.tenantId();
            try {
                AuthContext ac1 = new AuthContext(String.format("%s/%s", Constants.authority, tid), cache);
//                ac1.acquireToken(Constants.resourceARM, Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
                ac1.acquireToken(AzureEnvironment.AZURE.getResourceManagerEndpoint(), Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
//                ac1.acquireToken(Constants.resourceGraph, Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
                ac1.acquireToken(AzureEnvironment.AZURE.getGraphEndpoint(), Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
                List<String> sids = new LinkedList<>();
                for (Subscription s : AccessTokenAzureManager.getSubscriptions(tid)) {
                    sids.add(s.subscriptionId());
                }
                tidToSidsMap.put(t.tenantId(), sids);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // save account email
        String accountEmail = displayableId;
        if (accountEmail == null) {
            throw new IllegalArgumentException("accountEmail is null");
        }

        adAuthDetails.setAccountEmail(accountEmail);
        adAuthDetails.setTidToSidsMap(tidToSidsMap);
//        saveSettings();

        return result;
    }

    public Map<String, List<String>>  getAccountTenantsAndSubscriptions() {
        return adAuthDetails.getTidToSidsMap();
    }

    public void signOut() throws Exception {
        cleanCache();
        adAuthDetails.setAccountEmail(null);
        adAuthDetails.setTidToSidsMap(null);
//        saveSettings();
    }

    public boolean isSignedIn() {
        return adAuthDetails.getAccountEmail() != null;
    }

    public String getAccountEmail() { return adAuthDetails.getAccountEmail(); }

    // logout
    public void cleanCache() throws Exception {
        cache.clear();
    }

    private AdAuthManager(boolean useFileCache) throws Exception {
        cache = new TokenCache();
        if (useFileCache) {
            tokenFileStorage = new TokenFileStorage(CommonSettings.settingsBaseDir);
            byte[] data = tokenFileStorage.read();
            cache.deserialize(data);

            cache.setOnAfterAccessCallback(new Runnable() {
                public void run() {
                    try {
                        if(cache.getHasStateChanged()) {
                            tokenFileStorage.write(cache.serialize());
                            cache.setHasStateChanged(false);
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            });
        }
    }

//    private void loadSettings() throws Exception {
//        System.out.println("loadSettings()");
//        FileStorage fs = new FileStorage(adAuthSettingsFileName, settingsBaseDir);
//        byte[] data = fs.read();
//        String json = new String(data);
//        if (json.isEmpty()) {
//            adAuthDetails = new AdAuthDetails();
//            System.out.println(adAuthSettingsFileName + "file is empty");
//            return;
//        }
//        adAuthDetails = JsonHelper.deserialize(AdAuthDetails.class, json);
//    }

//    private void saveSettings() throws Exception {
//        System.out.println("saveSettings()");
//        String sd = JsonHelper.serialize(adAuthDetails);
//        FileStorage fs = new FileStorage(adAuthSettingsFileName, settingsBaseDir);
//        fs.write(sd.getBytes(Charset.forName("utf-8")));
//    }
}
