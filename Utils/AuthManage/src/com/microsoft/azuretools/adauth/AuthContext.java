package com.microsoft.azuretools.adauth;

import java.util.concurrent.Future;

enum AuthorityValidationType {
    True,
    False,
    NotProvided
}

public class AuthContext {
    
    Authenticator authenticator;
    TokenCache tokenCache;
    private static IWebUi userDefinedWebUi = null;

    public static void setUserDefinedWebUi(IWebUi userDefinedWebUi) {
    	AuthContext.userDefinedWebUi = userDefinedWebUi;
    }

    public AuthContext(String authority, TokenCache tokenCache) throws Exception {
        this(authority, AuthorityValidationType.NotProvided, tokenCache);
    }

    private AuthContext(String authority, AuthorityValidationType validateAuthority, TokenCache tokenCache) throws Exception {
        // If authorityType is not provided (via first constructor), we validate by default (except for ASG and Office tenants).
        this.authenticator = new Authenticator(authority, (validateAuthority != AuthorityValidationType.False));
        this.tokenCache = tokenCache;
    }

    public AuthenticationResult acquireToken(String resource, String clientId, String redirectUri, PromptBehavior promptBehavior, UserIdentifier userIdentifier) throws Exception {
        AcquireTokenInteractiveHandler handler = new AcquireTokenInteractiveHandler(this.authenticator, this.tokenCache,
                resource, clientId, redirectUri, promptBehavior, (userIdentifier != null) ? userIdentifier : UserIdentifier.anyUser,
                this.createWebAuthenticationDialog(promptBehavior));
        return handler.run();
    }

    public Future<AuthenticationResult> acquireTokenAsync(String resource, String clientId, String redirectUri, PromptBehavior promptBehavior, UserIdentifier userIdentifier) throws Exception {
    	AcquireTokenInteractiveHandler handler = new AcquireTokenInteractiveHandler(this.authenticator, this.tokenCache,
    			resource, clientId, redirectUri, promptBehavior, (userIdentifier != null) ? userIdentifier : UserIdentifier.anyUser,
    					this.createWebAuthenticationDialog(promptBehavior));
    	return handler.runAsync();
    }
    
    private IWebUi createWebAuthenticationDialog(PromptBehavior promptBehavior) throws Exception {
        if(userDefinedWebUi != null) {
        	return userDefinedWebUi;
        }
        return null;
    }

}
