package com.microsoft.auth;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;

public abstract class AcquireTokenHandlerBase {
	final static Logger log = Logger.getLogger(AcquireTokenHandlerBase.class.getName());
	protected final ExecutorService service = Executors.newWorkStealingPool();
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
    
    protected AcquireTokenHandlerBase(Authenticator authenticator, TokenCache tokenCache, String resource, ClientKey clientKey, TokenSubjectType subjectType) {
        this.authenticator = authenticator;
        this.callState = createCallState(this.authenticator.correlationId);
        this.tokenCache = tokenCache;
        if (resource == null || resource.isEmpty()) {
        	throw new IllegalArgumentException("resource");
        }
        this.resource = (resource != NullResource) ? resource : null;
        this.clientKey = clientKey;
        this.tokenSubjectType = subjectType;
        this.loadFromCache = (tokenCache != null);
        this.storeToCache = (tokenCache != null);
        this.supportADFS = false;
    }

    public Future<AuthenticationResult> runAsync() throws Exception {
    	return service.submit(new Callable<AuthenticationResult>() {
			@Override
			public AuthenticationResult call() throws Exception {
				boolean notifiedBeforeAccessCache = false;
				try {
					preRun();
					AuthenticationResult result = null;
					if (loadFromCache) {
		                notifyBeforeAccessCache();
		                notifiedBeforeAccessCache = true;
		                log.info(String.format("=== Token Acquisition started:\n\tAuthority: %s\n\tResource: %s\n\tClientId: %s\n\tCacheType: %s\n\tAuthentication Target: %s\n\t",
		                        authenticator.authority, resource, clientKey.clientId,
		                        (tokenCache != null) ? tokenCache.getClass().getName() + String.format(" (%d items)", tokenCache.getCount()) : "null", tokenSubjectType));
		                result = tokenCache.loadFromCache(authenticator.authority, resource,
		                    clientKey.clientId, tokenSubjectType, uniqueId, displayableId,
		                    callState);
		                result = validateResult(result);
		                if (result != null && result.accessToken == null 
		                	&& result.refreshToken != null) {
		                    result = refreshAccessTokenAsync(result).get();
		                    if (result != null) {
		                        tokenCache.storeToCache(result, authenticator.authority, resource, clientKey.clientId, tokenSubjectType, callState);
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
		                    tokenCache.storeToCache(result, authenticator.authority, resource, clientKey.clientId, tokenSubjectType, callState);
						}
					}
					postRunAsync(result);
					return result;
				}
				finally {
					if (notifiedBeforeAccessCache) {
						notifyAfterAccessCache();
					}
				}
			}
    	});
    }

    protected void preRun() throws Exception {
    	this.authenticator.updateFromTemplate(this.callState);
    	this.validateAuthorityType();
    }

    protected abstract void preTokenRequest() throws Exception;
    
    protected Future<AuthenticationResult> acquireTokenAsync() throws Exception {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(OAuthParameter.Resource, this.resource);
        requestParameters.put(OAuthParameter.ClientId, this.clientKey.clientId);
        addAditionalRequestParameters(requestParameters);
        return sendHttpMessageAsync(requestParameters);    	
    }

    protected void postTokenRequest(AuthenticationResult result) throws Exception {
    	authenticator.updateTenantId(result.tenantId);
    }

    protected void postRunAsync(AuthenticationResult result) {
    	service.submit(new Runnable() {
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

    private Future<AuthenticationResult> refreshAccessTokenAsync(AuthenticationResult result) {
    	return service.submit(new Callable<AuthenticationResult>() {
			@Override
			public AuthenticationResult call() throws Exception {
		        AuthenticationResult newResult = null;
		        if (resource != null) {
		        	log.info("Refreshing access token...");
		            newResult = sendTokenRequestByRefreshTokenAsync(result.refreshToken).get();
		                authenticator.updateTenantId(result.tenantId);
		            if (newResult.idToken == null) {
		                // If Id token is not returned by token endpoint when refresh token is redeemed, we should copy tenant and user information from the cached token.
		            	newResult.updateTenantAndUserInfo(result.tenantId, result.idToken, result.userInfo);
		            }
		        }
		        return newResult;
		    };
		});
    }
    
    protected Future<AuthenticationResult> sendTokenRequestByRefreshTokenAsync(String refreshToken) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(OAuthParameter.Resource, this.resource);
        requestParameters.put(OAuthParameter.ClientId, this.clientKey.clientId);
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.RefreshToken);
        requestParameters.put(OAuthParameter.RefreshToken, refreshToken);
        return sendHttpMessageAsync(requestParameters);
    }

    private Future<AuthenticationResult> sendHttpMessageAsync(Map<String, String> requestParameters) {
    	return service.submit(new Callable<AuthenticationResult>(){
			@Override
			public AuthenticationResult call() throws Exception {
				String uri = authenticator.tokenUri;
		    	TokenResponse tokenResponse = HttpHelper.sendPostRequestAndDeserializeJsonResponseAsync(uri, requestParameters, callState, TokenResponse.class).get();
		        AuthenticationResult result = ResponseUtils.parseTokenResponse(tokenResponse, callState);
		        if (result.refreshToken == null && requestParameters.containsKey(OAuthParameter.RefreshToken)) {
		            result.refreshToken = requestParameters.get(OAuthParameter.RefreshToken);
		            log.info("Refresh token was missing from the token refresh response, so the refresh token in the request is returned instead");
		        }
		        result.isMultipleResourceRefreshToken = (!StringUtils.isNullOrWhiteSpace(result.refreshToken) && !StringUtils.isNullOrWhiteSpace(tokenResponse.resource));
		        return result;
			}
    	});
    }

    private void notifyBeforeAccessCache() throws Exception {
    	tokenCache.onBeforeAccess();
    }

    private void notifyAfterAccessCache() throws Exception {
    	tokenCache.onAfterAccess();
    }

    private void logReturnedToken(AuthenticationResult result) {
        if (result.accessToken != null) {
        	Instant instant = Instant.ofEpochSecond(result.expiresOn);
        	LocalDateTime expiresOn = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            log.info("=== Token Acquisition finished successfully. An access token was retuned:\n\tExpiration Time UTC: " + expiresOn);
        }
    }

    private void validateAuthorityType() throws Exception {
        if (!this.supportADFS && this.authenticator.authorityType == AuthorityType.ADFS) {
            throw new AuthException(AuthError.InvalidAuthorityType + ": " + this.authenticator.authority);
        }
    }
}
    
