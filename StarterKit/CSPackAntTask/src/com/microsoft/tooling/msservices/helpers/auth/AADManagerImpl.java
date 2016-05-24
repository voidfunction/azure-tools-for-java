/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.helpers.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.auth.AuthContext;
import com.microsoft.auth.UserIdentifier;
import com.microsoft.auth.UserIdentifierType;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class AADManagerImpl implements AADManager {
    private static AADManager instance;
//    private static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    Logger logger = Logger.getLogger(AADManagerImpl.class.getName());

    private static final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
    private static final String RESOURCE = settings.getAzureServiceManagementUri();
    private final String AUTHORITY = settings.getAdAuthority();
    private final String CLIENT_ID = settings.getClientId();
    private final String REDIRECT_URI = settings.getRedirectUri();
    private final String COMMON_TENANT = settings.getTenantName();

//    private ReentrantReadWriteLock authResultLock = new ReentrantReadWriteLock(false);
//    private Map<UserInfo, ReentrantReadWriteLock> authResultLockByUser;
//    private Map<UserInfo, ReentrantReadWriteLock> tempLockByUser;

    final private com.microsoft.auth.TokenCache tokenCache;

    public AADManagerImpl() {
        tokenCache = new com.microsoft.auth.TokenCache();
        
        com.microsoft.auth.IWebUi webUi = DefaultLoader.getIdeHelper().getWebUi();
        if(webUi != null)
        	com.microsoft.auth.AuthContext.setUserDefinedWebUi(webUi);

        try {
            final com.microsoft.auth.
                    TokenFileStorage tokenFileStorage = new com.microsoft.auth.TokenFileStorage();
            byte[] data = tokenFileStorage.read();
            tokenCache.deserialize(data);

            tokenCache.setOnAfterAccessCallback(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(tokenCache.getHasStateChanged()) {
                            tokenFileStorage.write(tokenCache.serialize());
                            //DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AAD_TOKEN_CACHE, new String(tokenCache.serialize()), projectObject);
                            tokenCache.setHasStateChanged(false);
                        }
                    } catch (Exception e) {
                        logger.warning (e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @NotNull
    public static synchronized AADManager getManager() throws Exception {
        if (instance == null) {
//            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AADManagerImpl();
        }

        return instance;
    }

    @Override
    @NotNull
    public <T> T request(@NotNull UserInfo userInfo,
                         @NotNull String resource,
                         @NotNull String title,
                         @NotNull RequestCallback<T> requestCallback)
            throws AzureCmdException {

        com.microsoft.auth.
                UserIdentifier userIdentifier = new UserIdentifier(userInfo.getUniqueName(), UserIdentifierType.UniqueId);
        com.microsoft.auth.AuthenticationResult res = auth(userIdentifier, userInfo.getTenantId());
        try {
            return requestCallback.execute(res.getAccessToken());
        } catch (Throwable throwable) {
            logger.warning(throwable.getMessage());
            throw new AzureCmdException(throwable.getMessage(), throwable);
        }
    }

    public com.microsoft.auth.AuthenticationResult auth(com.microsoft.auth.UserIdentifier userIdentifier, String tenantName) throws AzureCmdException {
        if (tenantName == null) {
            tenantName = COMMON_TENANT;
        }
        try {
            AuthContext authContext = new com.microsoft.auth.AuthContext(String.format("%s/%s", AUTHORITY, tenantName), tokenCache);
            com.microsoft.auth.
                    AuthenticationResult result = authContext.acquireToken(RESOURCE, CLIENT_ID, REDIRECT_URI,
                    com.microsoft.auth.PromptBehavior.Auto, userIdentifier);
            return result;
        } catch (Throwable throwable) {
            logger.warning(throwable.getMessage());
            throw new AzureCmdException(throwable.getMessage(), throwable);
        }
    }

    public void clearTokenCache() throws Exception {
        tokenCache.clear();
    }
}