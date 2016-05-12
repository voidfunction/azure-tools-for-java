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
import com.microsoft.auth.UserIdentifier;
import com.microsoft.auth.UserIdentifierType;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoftopentechnologies.auth.AuthenticationResult;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class AADManagerImpl implements AADManager {
    private static AADManager instance;
    private static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    Logger logger = Logger.getLogger(AADManagerImpl.class.getName());
    /**
     * This structure stores AthenticationResults associated to each particular UserInfo (tenant+user) and Resource
     * The outer map provides a way to access all AutenticationResults for each particular UserInfo, and the inner map
     * gives access to the AuthenticationResult for a particular result
     */
    private Map<UserInfo, Map<String, AuthenticationResult>> authResultByUserResource;

    /**
     * This structure stores AthenticationResults associated to each particular UserInfo (tenant+user), in order to be
     * used as refresh tokens for new or expired resources
     */
    private Map<UserInfo, AuthenticationResult> refreshAuthResultByUser;

    private ReentrantReadWriteLock authResultLock = new ReentrantReadWriteLock(false);
    private Map<UserInfo, ReentrantReadWriteLock> authResultLockByUser;
    private Map<UserInfo, ReentrantReadWriteLock> tempLockByUser;

    private Object projectObject;

    final private com.microsoft.auth.TokenCache tokenCache;
    private com.microsoft.auth.AuthContext authContext = null;

    public AADManagerImpl(final Object projectObject) {
        this.projectObject = projectObject;

        final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        final String tenantName = settings.getTenantName();
        final String authority = settings.getAdAuthority();

        tokenCache = new com.microsoft.auth.TokenCache();

        try {
            final com.microsoft.auth.
                    TokenFileStorage tokenFileStorage = new com.microsoft.auth.TokenFileStorage();
            byte[] data = tokenFileStorage.read();
            System.out.println("======> tokenCache loading from file ==========");
            tokenCache.deserialize(data);

            authContext = new com.microsoft.auth.AuthContext(String.format("%s/%s", authority, tenantName), tokenCache);

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
                        e.printStackTrace();
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
            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AADManagerImpl(AzureManagerImpl.DEFAULT_PROJECT);
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
        com.microsoft.auth.AuthenticationResult res = auth(userIdentifier);
        try {
            return requestCallback.execute(res.getAccessToken());
        } catch (Throwable throwable) {
            logger.warning(throwable.getMessage());
            throw new AzureCmdException(throwable.getMessage(), throwable);
        }
    }
    public com.microsoft.auth.AuthenticationResult auth(com.microsoft.auth.UserIdentifier userIdentifier) throws AzureCmdException {

        final PluginSettings settings = DefaultLoader.getPluginComponent().getSettings();
        final String resource = settings.getAzureServiceManagementUri();
        String clientId = settings.getClientId();
        String redirectUri = settings.getRedirectUri();

        try {
            com.microsoft.auth.
                    AuthenticationResult result = authContext.acquireTokenAsync(resource, clientId, redirectUri,
                    com.microsoft.auth.PromptBehavior.Auto, userIdentifier).get();
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