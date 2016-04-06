package com.microsoft.azure.hdinsight.serverexplore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.common.*;

import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoftopentechnologies.auth.AuthenticationContext;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import com.microsoftopentechnologies.auth.PromptValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AADManagerImpl implements AADManager {
    private static AADManager instance;
    private static Gson gson;

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

    private AADManagerImpl() {
        String json = DefaultLoader.getIdeHelper().getProperty(CommonConst.AAD_AUTHENTICATION_RESULTS);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type authResultsType = new TypeToken<HashMap<UserInfo, Map<String, AuthenticationResult>>>() {
                }.getType();
                authResultByUserResource = gson.fromJson(json, authResultsType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AAD_AUTHENTICATION_RESULTS);
                authResultByUserResource = new HashMap<>();
            }
        } else {
            authResultByUserResource = new HashMap<>();
        }

        refreshAuthResultByUser = new HashMap<>();
        authResultLockByUser = new HashMap<>();
        tempLockByUser = new HashMap<>();

        for (Map.Entry<UserInfo, Map<String, AuthenticationResult>> userAuthResults :
                authResultByUserResource.entrySet()) {
            UserInfo userInfo = userAuthResults.getKey();
            Map<String, AuthenticationResult> authResults = userAuthResults.getValue();

            AuthenticationResult refreshAuthResult = null;

            for (AuthenticationResult authResult : authResults.values()) {
                if (!StringHelper.isNullOrWhiteSpace(authResult.getRefreshToken()) &&
                        !StringHelper.isNullOrWhiteSpace(authResult.getResource()) &&
                        (refreshAuthResult == null || refreshAuthResult.getExpiresOn() < authResult.getExpiresOn())) {
                    refreshAuthResult = authResult;
                }
            }

            if (refreshAuthResult != null) {
                refreshAuthResultByUser.put(userInfo, refreshAuthResult);
            }

            authResultLockByUser.put(userInfo, new ReentrantReadWriteLock(false));
        }
    }

    @NotNull
    public static synchronized AADManager getManager() {
        if (instance == null) {
            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AADManagerImpl();
        }

        return instance;
    }

    @Override
    @NotNull
    public UserInfo authenticate(@NotNull String resource, @NotNull String title)
            throws HDExploreException {
        try {
            return authenticate(CommonConstant.commonTenantName, resource, title, PromptValue.login);
        } catch (ExecutionException e) {
            throw new HDExploreException("Error invoking the authentication process", e.getCause());
        } catch (Throwable e) {
            if (e instanceof HDExploreException) {
                throw (HDExploreException) e;
            }

            throw new HDExploreException("Error invoking the authentication process", e);
        }
    }

    @Override
    public void authenticate(@NotNull UserInfo userInfo,
                             @NotNull String resource,
                             @NotNull String title)
            throws HDExploreException {
        authenticateWithInteractiveToken(userInfo, resource, title);
    }

    @Override
    @NotNull
    public <T> T request(@NotNull UserInfo userInfo,
                         @NotNull String resource,
                         @NotNull String title,
                         @NotNull AADManagerRequestCallback<T> AADManagerRequestCallback)
            throws HDExploreException {
        if (isAuthenticated(userInfo, resource)) {
            return authenticatedRequest(userInfo, resource, title, AADManagerRequestCallback);
        } else {
            return unauthenticatedRequest(userInfo, resource, title, AADManagerRequestCallback);
        }
    }

    private <T> T authenticatedRequest(@NotNull final UserInfo userInfo,
                                       @NotNull final String resource,
                                       @NotNull final String title,
                                       @NotNull final AADManagerRequestCallback<T> AADManagerRequestCallback)
            throws HDExploreException {
        AuthenticationResult authenticationResult = getAuthenticationResult(userInfo, resource);

        try {
            return AADManagerRequestCallback.execute(authenticationResult.getAccessToken());
        } catch (Throwable throwable) {
            if (isErrorResourceUnauthorized(throwable)) {
                return unauthenticatedRequest(userInfo, resource, title, AADManagerRequestCallback);
            } else if (throwable instanceof HDExploreException) {
                throw (HDExploreException) throwable;
            } else if (throwable instanceof ExecutionException) {
                throw new HDExploreException(throwable.getCause().getMessage(), throwable.getCause());
            }

            throw new HDExploreException(throwable.getMessage(), throwable);
        }
    }

    private <T> T unauthenticatedRequest(@NotNull UserInfo userInfo,
                                         @NotNull String resource,
                                         @NotNull String title,
                                         @NotNull AADManagerRequestCallback<T> AADManagerRequestCallback)
            throws HDExploreException {
        if (hasRefreshAuthResult(userInfo)) {
            authenticateWithRefreshToken(userInfo, resource, title);
        } else {
            authenticateWithInteractiveToken(userInfo, resource, title);
        }

        return requestWithAuthenticationResult(userInfo, resource, AADManagerRequestCallback);
    }

    private <T> T requestWithAuthenticationResult(@NotNull UserInfo userInfo,
                                                  @NotNull String resource,
                                                  @NotNull AADManagerRequestCallback<T> AADManagerRequestCallback)
            throws HDExploreException {
        AuthenticationResult authResult = getAuthenticationResult(userInfo, resource);

        return requestWithAuthenticationResult(AADManagerRequestCallback, authResult);
    }

    private <T> T requestWithAuthenticationResult(@NotNull AADManagerRequestCallback<T> AADManagerRequestCallback,
                                                  @NotNull AuthenticationResult authResult)
            throws HDExploreException {


        try {
            return AADManagerRequestCallback.execute(authResult.getAccessToken());
        } catch (Throwable throwable) {
            if (throwable instanceof HDExploreException) {
                throw (HDExploreException) throwable;
            } else if (throwable instanceof ExecutionException) {
                throw new HDExploreException(throwable.getCause().getMessage(), throwable.getCause());
            }

            throw new HDExploreException(throwable.getMessage(), throwable);
        }
    }

    private void authenticateWithRefreshToken(
            @NotNull UserInfo userInfo,
            @NotNull String resource,
            @NotNull String title) throws HDExploreException {
        // acquire token via refresh token

        // Now there might be multiple concurrent requests all of which are likely
        // to end up needing a new access token all at the same time. In this case
        // we don't want to issue multiple requests redeeming refresh tokens as that
        // is wasteful. To prevent that we serialize the code block below using a
        // re-entrant lock. We do this by first acquiring the current authentication
        // token before acquiring the lock which may or may not have a value. After the
        // lock has been acquired, the following are the possibilities:
        //    +-------------------+------------------+--------+----------------------+
        //    | Value before lock | Value after lock | Equal? | Issue Token Request? |
        //    +-------------------+------------------+--------+----------------------+
        //    | not null          | not null         | no     | no                   |
        //    | not null          | not null         | yes    | yes                  |
        //    +-------------------+------------------+--------+----------------------+
        AuthenticationResult refreshAuthResult = getRefreshAuthResult(userInfo);

        ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
        userLock.writeLock().lock();

        try {
            if (AuthenticationResult.equals(refreshAuthResult, getRefreshAuthResult(userInfo))) {
                AuthenticationContext context = null;

                try {
                    context = new AuthenticationContext(CommonConstant.authority);
                    AuthenticationResult authResult = context.acquireTokenByRefreshToken(
                            refreshAuthResult,
                            userInfo.getTenantId(),
                            resource,
                            CommonConstant.clientID);
                    validateAuthenticationResult(authResult);

                    UserInfo userInfo2 = new UserInfo(authResult.getUserInfo().getTenantId(),
                            authResult.getUserInfo().getUniqueName());

                    if (!userInfo.equals(userInfo2)) {
                        throw new HDExploreException("Invalid User Information retrieved");
                    }

                    setAuthenticationResult(userInfo, resource, authResult);
                } finally {
                    if (context != null) {
                        context.dispose();
                    }
                }
            }
        } catch (Throwable t) {
            if (isErrorTokenUnauthorized(t)) {
                authenticateWithInteractiveToken(userInfo, resource, title);
            } else if (t instanceof HDExploreException) {
                throw (HDExploreException) t;
            } else if (t instanceof ExecutionException) {
                throw new HDExploreException("Error invoking the authentication process with refresh token", t.getCause());
            } else {
                throw new HDExploreException("Error invoking the authentication process with refresh token", t);
            }
        } finally {
            // we release the lock before we do anything else as
            // we don't want a lock leaking in case the statements
            // following throw
            userLock.writeLock().unlock();
        }
    }

    private void authenticateWithInteractiveToken(@NotNull UserInfo userInfo,
                                                  @NotNull String resource,
                                                  @NotNull String title)
            throws HDExploreException {
        // acquire token via interactive token

        // Now there might be multiple concurrent requests all of which are likely
        // to end up needing a new access token all at the same time. In this case
        // we don't want to pop up multiple login windows for the same UserInfo.
        // To prevent that we serialize the code block below using a re-entrant lock;
        // since this might be the first time logging in with this UserInfo we create an
        // ad hoc lock.
        // We acquire the current authentication and refresh token before acquiring the lock
        // (any of them might be null.) After the lock has been acquired, we validate if any
        // of this values have changed, including null to not null transitions.
        AuthenticationResult authenticationResult = isAuthenticated(userInfo, resource) ?
                getAuthenticationResult(userInfo, resource) :
                null;
        AuthenticationResult refreshAuthResult = hasRefreshAuthResult(userInfo) ?
                getRefreshAuthResult(userInfo) :
                null;
        boolean useRenewedRefresh = false;

        ReentrantReadWriteLock userLock = getTempUserLock(userInfo);
        userLock.writeLock().lock();

        try {
            AuthenticationResult renewedAuth = isAuthenticated(userInfo, resource) ?
                    getAuthenticationResult(userInfo, resource) :
                    null;
            AuthenticationResult renewedRefresh = hasRefreshAuthResult(userInfo) ?
                    getRefreshAuthResult(userInfo) :
                    null;
            boolean useRenewedAuth = renewedAuth != null && !renewedAuth.equals(authenticationResult);
            useRenewedRefresh = !useRenewedAuth && renewedRefresh != null && !renewedRefresh.equals(refreshAuthResult);

            if (!useRenewedAuth && !useRenewedRefresh) {
                try {
                    UserInfo interactiveUserInfo = authenticate(userInfo.getTenantId(), resource, title,
                            PromptValue.attemptNone);

                    if (!userInfo.equals(interactiveUserInfo)) {
                        //browser cached credentials don't match
                        interactiveUserInfo = authenticate(userInfo.getTenantId(), resource, title,
                                PromptValue.login);

                        if (!userInfo.equals(interactiveUserInfo)) {
                            //User could change the selected credentials, but we shouldn't allow this
                            throw new HDExploreException("Invalid User Information retrieved");
                        }
                    }
                } catch (Throwable t) {
                    if (t instanceof HDExploreException) {
                        throw (HDExploreException) t;
                    }

                    try {
                        //There might be multiple credentials cached or none
                        UserInfo interactiveUserInfo = authenticate(userInfo.getTenantId(), resource, title,
                                PromptValue.refreshSession);

                        if (!userInfo.equals(interactiveUserInfo)) {
                            //User could change the selected credentials, but we shouldn't allow this
                            throw new HDExploreException("Invalid User Information retrieved");
                        }
                    } catch (Throwable e) {
                        if (e instanceof HDExploreException) {
                            throw (HDExploreException) e;
                        } else if (e instanceof ExecutionException) {
                            throw new HDExploreException("Error invoking the authentication process with interactive token", e.getCause());
                        }

                        throw new HDExploreException("Error invoking the authentication process with interactive token", e);
                    }
                }
            }
        } finally {
            // we release the lock before we do anything else as
            // we don't want a lock leaking in case the statements
            // following throw
            userLock.writeLock().unlock();
        }

        if (useRenewedRefresh) {
            authenticateWithRefreshToken(userInfo, resource, title);
        }
    }

    @NotNull
    private UserInfo authenticate(@NotNull String tenantId,
                                  @NotNull String resource,
                                  @NotNull String title,
                                  @NotNull String promptValue)
            throws IOException, ExecutionException, InterruptedException, HDExploreException {
        AuthenticationContext context = null;

        try {
            context = new AuthenticationContext(CommonConstant.authority);

            // Temporary Fix: Cannot login to Azure and get cluster on Ubuntu15/Fedora23, should be delete when login problem fix
            // Login page crashed when using the origin browser launcher
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                context.setBrowserLauncher(new DefaultLinuxBrowserLauncher());
            }

            AuthenticationResult authResult = context.acquireTokenInteractiveAsync(
                    tenantId,
                    resource,
                    CommonConstant.clientID,
                    CommonConstant.redirectURI,
                    title,
                    promptValue).get();
            validateAuthenticationResult(authResult);

            UserInfo userInfo = new UserInfo(authResult.getUserInfo().getTenantId(),
                    authResult.getUserInfo().getUniqueName());
            setAuthenticationResult(userInfo, resource, authResult);

            return userInfo;
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
    }

    private static void validateAuthenticationResult(AuthenticationResult authResult) throws HDExploreException {
        if (authResult == null) {
            throw new HDExploreException("Invalid Authentication data retrieved", "");
        }

        String accessToken = authResult.getAccessToken();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new HDExploreException("Invalid access token retrieved", "");
        }

        com.microsoftopentechnologies.auth.UserInfo userInfo = authResult.getUserInfo();

        if (userInfo == null) {
            throw new HDExploreException("Invalid User Information retrieved", "");
        }

        String uniqueName = userInfo.getUniqueName();
        String tenantId = userInfo.getTenantId();

        if (uniqueName == null || uniqueName.trim().isEmpty() ||
                tenantId == null || tenantId.trim().isEmpty()) {
            throw new HDExploreException("Invalid User Information retrieved", "");
        }
    }

    private boolean isAuthenticated(@NotNull UserInfo userInfo, @NotNull String resource) {
        ReentrantReadWriteLock userLock;
        authResultLock.readLock().lock();

        try {
            if (authResultLockByUser.containsKey(userInfo)) {
                userLock = authResultLockByUser.get(userInfo);
            } else {
                return false;
            }
        } finally {
            authResultLock.readLock().unlock();
        }

        userLock.readLock().lock();

        try {
            return authResultByUserResource.containsKey(userInfo) &&
                    authResultByUserResource.get(userInfo).containsKey(resource);
        } finally {
            userLock.readLock().unlock();
        }
    }

    @NotNull
    private AuthenticationResult getAuthenticationResult(@NotNull UserInfo userInfo,
                                                         @NotNull String resource)
            throws HDExploreException {
        ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
        userLock.readLock().lock();

        try {
            if (!authResultByUserResource.containsKey(userInfo)) {
                throw new HDExploreException("No Authentication data for the specified User Information", "");
            }

            Map<String, AuthenticationResult> authResults = authResultByUserResource.get(userInfo);

            if (!authResults.containsKey(resource)) {
                throw new HDExploreException("No Authentication data for the specified resource", "");
            }

            return authResults.get(resource);
        } finally {
            userLock.readLock().unlock();
        }
    }

    private void setAuthenticationResult(@NotNull UserInfo userInfo,
                                         @NotNull String resource,
                                         @NotNull AuthenticationResult authResult)
            throws HDExploreException {
        ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
        userLock.writeLock().lock();

        try {
            Map<String, AuthenticationResult> authResults;

            if (authResultByUserResource.containsKey(userInfo)) {
                authResults = authResultByUserResource.get(userInfo);
            } else {
                authResults = new HashMap<>();
            }

            authResults.put(resource, authResult);
            authResultByUserResource.put(userInfo, authResults);

            AuthenticationResult refreshAuthResult = refreshAuthResultByUser.containsKey(userInfo) ?
                    refreshAuthResultByUser.get(userInfo) :
                    null;

            if (!StringHelper.isNullOrWhiteSpace(authResult.getRefreshToken()) &&
                    !StringHelper.isNullOrWhiteSpace(authResult.getResource()) &&
                    (refreshAuthResult == null || refreshAuthResult.getExpiresOn() < authResult.getExpiresOn())) {
                refreshAuthResultByUser.put(userInfo, authResult);
            }

            Type authResultsType = new TypeToken<HashMap<UserInfo, Map<String, AuthenticationResult>>>() {
            }.getType();
            String json = gson.toJson(authResultByUserResource, authResultsType);
            DefaultLoader.getIdeHelper().setProperty(CommonConst.AAD_AUTHENTICATION_RESULTS, json);
        } finally {
            userLock.writeLock().unlock();
        }
    }

    private boolean hasRefreshAuthResult(@NotNull UserInfo userInfo) {
        ReentrantReadWriteLock userLock;
        authResultLock.readLock().lock();

        try {
            if (authResultLockByUser.containsKey(userInfo)) {
                userLock = authResultLockByUser.get(userInfo);
            } else {
                return false;
            }
        } finally {
            authResultLock.readLock().unlock();
        }

        userLock.readLock().lock();

        try {
            return refreshAuthResultByUser.containsKey(userInfo);
        } finally {
            userLock.readLock().unlock();
        }
    }

    @NotNull
    private AuthenticationResult getRefreshAuthResult(@NotNull UserInfo userInfo)
            throws HDExploreException {
        ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
        userLock.readLock().lock();

        try {
            if (!refreshAuthResultByUser.containsKey(userInfo)) {
                throw new HDExploreException("No refresh Authentication data for the specified User Information", "");
            }

            return refreshAuthResultByUser.get(userInfo);
        } finally {
            userLock.readLock().unlock();
        }
    }

    @NotNull
    private ReentrantReadWriteLock getUserLock(@NotNull UserInfo userInfo, boolean createOnMissing)
            throws HDExploreException {
        Lock lock = createOnMissing ? authResultLock.writeLock() : authResultLock.readLock();
        lock.lock();

        try {
            if (!authResultLockByUser.containsKey(userInfo)) {
                if (createOnMissing) {
                    authResultLockByUser.put(userInfo, new ReentrantReadWriteLock(false));
                } else {
                    throw new HDExploreException("No Authentication data for the specified User Information", "");
                }
            }

            return authResultLockByUser.get(userInfo);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    private ReentrantReadWriteLock getTempUserLock(@NotNull UserInfo userInfo) {
        Lock lock = authResultLock.writeLock();
        lock.lock();

        try {
            if (!tempLockByUser.containsKey(userInfo)) {
                tempLockByUser.put(userInfo, new ReentrantReadWriteLock(false));
            }

            return tempLockByUser.get(userInfo);
        } finally {
            lock.unlock();
        }
    }

    private boolean isErrorResourceUnauthorized(Throwable throwable) {
        return ((HDIException) throwable).getErrorCode() == 401;
    }

    private boolean isErrorTokenUnauthorized(Throwable throwable) {
        String errorMessage = throwable.getMessage();
        return errorMessage.contains("invalid_grant") ||
                errorMessage.contains("400");
    }
}