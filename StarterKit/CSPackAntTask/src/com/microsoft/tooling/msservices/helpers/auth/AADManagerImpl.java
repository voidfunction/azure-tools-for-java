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

import com.microsoft.auth.AuthContext;
import com.microsoft.auth.AuthenticationResult;
import com.microsoft.auth.TokenCache;
import com.microsoft.auth.IWebUi;
import com.microsoft.auth.TokenFileStorage;
import com.microsoft.auth.AuthenticationResult;
import com.microsoft.auth.PromptBehavior;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

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

    final private TokenCache tokenCache;

    public AADManagerImpl() {
        tokenCache = new TokenCache();
        
        IWebUi webUi = DefaultLoader.getIdeHelper().getWebUi();
        if(webUi != null) {
            AuthContext.setUserDefinedWebUi(webUi);
        }

        try {
            String psPath = DefaultLoader.getIdeHelper().getProjectSettingsPath();
            System.out.println(String.format("\n\n========> Project settings path: %s \n\n", psPath));
            final TokenFileStorage tokenFileStorage = new TokenFileStorage(psPath);
            byte[] data = tokenFileStorage.read();
            tokenCache.deserialize(data);

            tokenCache.setOnAfterAccessCallback(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(tokenCache.getHasStateChanged()) {
                            tokenFileStorage.write(tokenCache.serialize());;
                            tokenCache.setHasStateChanged(false);
                        }
                    } catch (Exception e) {
                        logger.warning (e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.warning (e.getMessage());
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

        AuthenticationResult res = auth(userInfo.getTenantId(), PromptBehavior.Auto);
        try {
            return requestCallback.execute(res.getAccessToken());
        } catch (Throwable throwable) {
            logger.warning(throwable.getMessage());
            throw new AzureCmdException(throwable.getMessage(), throwable);
        }
    }

    public AuthenticationResult auth(String tenantName, PromptBehavior pb) throws AzureCmdException {
        if (tenantName == null) {
            tenantName = COMMON_TENANT;
        }
        try {
            AuthContext authContext = new AuthContext(String.format("%s/%s", AUTHORITY, tenantName), tokenCache);
            AuthenticationResult result = authContext.acquireToken(RESOURCE, CLIENT_ID, REDIRECT_URI, pb, null);
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