/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.adauth;

final class TokenCacheItem {
    /// <summary>
    /// Gets the Authority.
    /// </summary>
    String authority;

    /// <summary>
    /// Gets the ClientId.
    /// </summary>
    String clientId;

    /// <summary>
    /// Gets the Expiration.
    /// </summary>
    long expiresOn;

    /// <summary>
    /// Gets the FamilyName.
    /// </summary>
    String familyName;

    /// <summary>
    /// Gets the GivenName.
    /// </summary>
    String givenName;

    /// <summary>
    /// Gets the IdentityProviderName.
    /// </summary>
    String identityProvider;

    /// <summary>
    /// Gets a value indicating whether the RefreshToken applies to multiple resources.
    /// </summary>
    boolean isMultipleResourceRefreshToken;

    /// <summary>
    /// Gets the Resource.
    /// </summary>
    String resource;

    /// <summary>
    /// Gets the TenantId.
    /// </summary>
    String tenantId;

    /// <summary>
    /// Gets the user's unique Id.
    /// </summary>
    String uniqueId;

    /// <summary>
    /// Gets the user's displayable Id.
    /// </summary>
    String displayableId;

    /// <summary>
    /// Gets the Access Token requested.
    /// </summary>
    String accessToken;

    /// <summary>
    /// Gets the Refresh Token associated with the requested Access Token. Note: not all operations will return a Refresh Token.
    /// </summary>
    String refreshToken;

    /// <summary>
    /// Gets the entire Id Token if returned by the service or null if no Id Token is returned.
    /// </summary>
    String idToken;

    TokenSubjectType tokenSubjectType;

    boolean match(TokenCacheKey key) {
        return (key.authority == this.authority && key.equals(this.resource) && key.clientId.equals(this.clientId)
                && key.tokenSubjectType == this.tokenSubjectType && key.uniqueId == this.uniqueId && key.displayableId.equals(this.displayableId));
    }
    
    TokenCacheItem(TokenCacheKey key, AuthenticationResult result) {
        this.authority = key.authority;
        this.resource = key.resource;
        this.clientId = key.clientId;
        this.tokenSubjectType = key.tokenSubjectType;
        this.uniqueId = key.uniqueId;
        this.displayableId = key.displayableId;
        this.tenantId = result.tenantId;
        this.expiresOn = result.expiresOn;
        this.isMultipleResourceRefreshToken = result.isMultipleResourceRefreshToken;
        this.accessToken = result.accessToken;
        this.refreshToken = result.refreshToken;
        this.idToken = result.idToken;

        if (result.userInfo != null) {
            this.familyName = result.userInfo.familyName;
            this.givenName = result.userInfo.givenName;
            this.identityProvider = result.userInfo.identityProvider;
        }
    }
}
