package com.microsoft.auth;

import java.util.concurrent.Future;

import com.microsoft.auth.ui.WebUi;

enum AuthorityValidationType {
    True,
    False,
    NotProvided
}

public class AuthContext {
    
    Authenticator authenticator;
    TokenCache tokenCache;
    
    public AuthContext(String authority, TokenCache tokenCache) throws Exception {
        this(authority, AuthorityValidationType.NotProvided, tokenCache);
    }

    private AuthContext(String authority, AuthorityValidationType validateAuthority, TokenCache tokenCache) throws Exception {
        // If authorityType is not provided (via first constructor), we validate by default (except for ASG and Office tenants).
        this.authenticator = new Authenticator(authority, (validateAuthority != AuthorityValidationType.False));
        this.tokenCache = tokenCache;
    }
    
    public Future<AuthenticationResult> acquireTokenAsync(String resource, String clientId, String redirectUri, PromptBehavior promptBehavior) throws Exception {
        AcquireTokenInteractiveHandler handler = new AcquireTokenInteractiveHandler(this.authenticator, this.tokenCache, resource, clientId, redirectUri, promptBehavior, UserIdentifier.anyUser, this.createWebAuthenticationDialog(promptBehavior));
        return handler.runAsync();
    }
    
    private IWebUi createWebAuthenticationDialog(PromptBehavior promptBehavior) throws Exception {
//        return new WebUi(promptBehavior);
        WebUi webUi = WebUi.getInstance();
        if(promptBehavior != PromptBehavior.Always) {
            webUi.setUseCookie(true);
        }
        return webUi;
    }

}
