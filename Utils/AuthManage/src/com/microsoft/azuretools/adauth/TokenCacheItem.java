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
