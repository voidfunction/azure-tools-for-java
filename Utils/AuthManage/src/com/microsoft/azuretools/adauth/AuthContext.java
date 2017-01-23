/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

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
