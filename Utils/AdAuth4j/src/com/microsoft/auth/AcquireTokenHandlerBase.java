package com.microsoft.auth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

public abstract class AcquireTokenHandlerBase {
    final static Logger log = Logger.getLogger(AcquireTokenHandlerBase.class.getName());
    protected static ExecutorService service = Executors.newSingleThreadExecutor();
    CallState callState;
    protected boolean supportADFS;
    protected Authenticator authenticator;
    protected String resource;
    protected ClientKey clientKey;
    protected TokenSubjectType tokenSubjectType;
    protected String uniqueId;
    protected String displayableId;
    protected UserIdentifierType userIdentifierType;
    protected boolean loadFromCache;
    protected boolean storeToCache;
    protected final static String NullResource = "null_resource_as_optional";
    private final TokenCache tokenCache;
    private static Object cacheLock = new Object();
    
    protected AcquireTokenHandlerBase(Authenticator authenticator, TokenCache tokenCache, String resource, ClientKey clientKey, TokenSubjectType subjectType) {
        this.authenticator = authenticator;
        this.callState = createCallState(this.authenticator.correlationId);
        this.tokenCache = tokenCache;
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("resource");
        }
        this.resource = (!resource.equals(NullResource)) ? resource : null;
        this.clientKey = clientKey;
        this.tokenSubjectType = subjectType;
        this.loadFromCache = (tokenCache != null);
        this.storeToCache = (tokenCache != null);
        this.supportADFS = false;
    }

    AuthenticationResult run() throws Exception {
        boolean notifiedBeforeAccessCache = false;
        try {
            synchronized (cacheLock) {
                preRun();
                AuthenticationResult result = null;
                long start = System.currentTimeMillis();
                if (loadFromCache) {
                    notifyBeforeAccessCache();
                    notifiedBeforeAccessCache = true;
                    log.info(String.format("\n=== Token Acquisition started:\n\tAuthority: %s\n\tResource: %s\n\tClientId: %s\n\tCacheType: %s\n\tAuthentication Target: %s\n\tthread name: %s\n\t",
                            authenticator.getAuthority(), resource, clientKey.clientId,
                            (tokenCache != null) ? tokenCache.getClass().getName() + String.format(" (%d items)", tokenCache.getCount()) : "null", tokenSubjectType, Thread.currentThread().getName() ));
                    result = tokenCache.loadFromCache(authenticator.getAuthority(), resource,
                            clientKey.clientId, tokenSubjectType, uniqueId, displayableId);
                    result = validateResult(result);
                    if (result != null && result.accessToken == null
                            && result.refreshToken != null) {
                        result = refreshAccessTokenAsync(result).get();
                        if (result != null) {
                            tokenCache.storeToCache(result, authenticator.getAuthority(), resource, clientKey.clientId, tokenSubjectType);
                        }
                    }
                }
                if (result == null) {
                    preTokenRequest();
                    result = acquireTokenAsync().get();
                    postTokenRequest(result);
                    if (storeToCache) {
                        if (!notifiedBeforeAccessCache) {
                            notifyBeforeAccessCache();
                            notifiedBeforeAccessCache = true;
                        }
                        tokenCache.storeToCache(result, authenticator.getAuthority(), resource, clientKey.clientId, tokenSubjectType);
                    }
                }
                postRunAsync(result);
                long end = System.currentTimeMillis();
                log.info(String.format("====> %s: %d ms to get access token =========", Thread.currentThread().getName(), end-start));
                return result;
            }
        }
        finally {
            if (notifiedBeforeAccessCache) {
                notifyAfterAccessCache();
            }
        }
    }
    
    Future<AuthenticationResult> runAsync() throws Exception {
        return service.submit(new Callable<AuthenticationResult>() {
            @Override
            public AuthenticationResult call() throws Exception {
                return run();
            }
        });
    }

    protected void preRun() throws Exception {
        this.authenticator.updateFromTemplate(this.callState);
        this.validateAuthorityType();
    }

    protected abstract void preTokenRequest() throws Exception;
    
    protected Future<AuthenticationResult> acquireTokenAsync() throws Exception {
        return Executors.newSingleThreadExecutor().submit(new Callable<AuthenticationResult>() {
            @Override
            public AuthenticationResult call() throws Exception {
                Map<String, String> requestParameters = new HashMap<>();
                requestParameters.put(OAuthParameter.Resource, resource);
                requestParameters.put(OAuthParameter.ClientId, clientKey.clientId);
                addAditionalRequestParameters(requestParameters);
                return sendHttpMessage(requestParameters);  
            }
        });
    }

    protected void postTokenRequest(AuthenticationResult result) throws Exception {
        authenticator.updateTenantId(result.tenantId);
    }

    protected void postRunAsync(final AuthenticationResult result) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                logReturnedToken(result);
            }
        });
    }
    
    protected AuthenticationResult validateResult(AuthenticationResult result) {
        return result;
    }

    public static CallState createCallState(UUID correlationId) {
        correlationId = (correlationId != null) ? correlationId : UUID.randomUUID();
        return new CallState(correlationId);
    }

    protected abstract void addAditionalRequestParameters(Map<String, String> requestParameters);

    private Future<AuthenticationResult> refreshAccessTokenAsync(final AuthenticationResult result) {
        return Executors.newSingleThreadExecutor().submit(new Callable<AuthenticationResult>() {
            @Override
            public AuthenticationResult call() throws Exception {
                AuthenticationResult newResult = null;
                if (resource != null) {
                    log.info("Refreshing access token...");
                    try {
                        newResult = sendTokenRequestByRefreshToken(result.refreshToken);
                       
                    } catch (AuthException e) {
                        log.info("Error getting token - need to re-loggin.");
                        return null;
                    }
                    authenticator.updateTenantId(result.tenantId);
                    if (newResult != null && newResult.idToken == null) {
                        // If Id token is not returned by token endpoint when refresh token is redeemed, we should copy tenant and user information from the cached token.
                        newResult.updateTenantAndUserInfo(result.tenantId, result.idToken, result.userInfo);
                    }
                }
                return newResult;
            }
        });
    }
    
    protected AuthenticationResult sendTokenRequestByRefreshToken(String refreshToken) throws AuthException, Exception {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(OAuthParameter.Resource, this.resource);
        requestParameters.put(OAuthParameter.ClientId, this.clientKey.clientId);
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.RefreshToken);
        requestParameters.put(OAuthParameter.RefreshToken, refreshToken);
        return sendHttpMessage(requestParameters);
    }

    private AuthenticationResult sendHttpMessage(final Map<String, String> requestParameters) throws AuthException, Exception {
        String uri = authenticator.tokenUri;
        TokenResponse tokenResponse = HttpHelper.sendPostRequestAndDeserializeJsonResponse(uri, requestParameters, callState, TokenResponse.class);
        AuthenticationResult result = ResponseUtils.parseTokenResponse(tokenResponse);
        if (result.refreshToken == null && requestParameters.containsKey(OAuthParameter.RefreshToken)) {
            result.refreshToken = requestParameters.get(OAuthParameter.RefreshToken);
            log.info("Refresh token was missing from the token refresh response, so the refresh token in the request is returned instead");
        }
        result.isMultipleResourceRefreshToken = (!StringUtils.isNullOrWhiteSpace(result.refreshToken) && !StringUtils.isNullOrWhiteSpace(tokenResponse.resource));
        return result;
    }

    private void notifyBeforeAccessCache() throws Exception {
        tokenCache.onBeforeAccess();
    }

    private void notifyAfterAccessCache() throws Exception {
        tokenCache.onAfterAccess();
    }

    private void logReturnedToken(AuthenticationResult result) {
        if (result.accessToken != null) {
            Calendar exp = new GregorianCalendar();
            exp.setTimeInMillis(TimeUnit.SECONDS.toMillis(result.expiresOn));
            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            log.info("=== Token Acquisition finished successfully. An access token was retuned:\n\tExpiration Time: " + df.format(exp.getTime()));
        }
    }

    private void validateAuthorityType() throws Exception {
        if (!this.supportADFS && this.authenticator.authorityType == AuthorityType.ADFS) {
            String message = AuthError.InvalidAuthorityType + ": " + this.authenticator.getAuthority();
            throw new AuthException(message);
        }
    }
}
    
