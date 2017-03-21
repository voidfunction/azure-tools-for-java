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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationResult {
    private final static String Oauth2AuthorizationHeader = "Bearer ";

    AuthenticationResult(){}
    
    /// <summary>
    /// Creates result returned from AcquireToken. Except in advanced scenarios related to token caching, you do not need to create any instance of AuthenticationResult.
    /// </summary>
    /// <param name="accessTokenType">Type of the Access Token returned</param>
    /// <param name="accessToken">The Access Token requested</param>
    /// <param name="refreshToken">The Refresh Token associated with the requested Access Token</param>
    /// <param name="expiresOn">The point in time in which the Access Token returned in the AccessToken property ceases to be valid</param>
    AuthenticationResult(String accessTokenType, String accessToken, String refreshToken, long expiresOn) {
        this.accessTokenType = accessTokenType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresOn =  expiresOn;
    }
    
    // This is only needed for AcquireTokenByAuthorizationCode in which parameter resource is optional and we need
    // to get it from the STS response.
    String resource;

    /// <summary>
    /// Gets the type of the Access Token returned. 
    /// </summary>
    @JsonProperty
    String accessTokenType;

    /// <summary>
    /// Gets the Access Token requested.
    /// </summary>
    @JsonProperty
    String accessToken;

    /// <summary>
    /// Gets the Refresh Token associated with the requested Access Token. Note: not all operations will return a Refresh Token.
    /// </summary>
    @JsonProperty
    String refreshToken;

    /// <summary>
    /// Gets the point in time in which the Access Token returned in the AccessToken property ceases to be valid.
    /// This value is calculated based on current UTC time measured locally and the value expiresIn received from the service.
    /// </summary>
    @JsonProperty
    long expiresOn;
    
//    OffsetDateTime expiresOn;

    /// <summary>
    /// Gets an identifier for the tenant the token was acquired from. This property will be null if tenant information is not returned by the service.
    /// </summary>
    @JsonProperty
    String tenantId;

    /// <summary>
    /// Gets user information including user Id. Some elements in UserInfo might be null if not returned by the service.
    /// </summary>
    @JsonProperty
    UserInfo userInfo;

    /// <summary>
    /// Gets the entire Id Token if returned by the service or null if no Id Token is returned.
    /// </summary>
    @JsonProperty
    String idToken;

    /// <summary>
    /// Gets a value indicating whether the refresh token can be used for requesting access token for other resources.
    /// </summary>
    @JsonProperty
    boolean isMultipleResourceRefreshToken;

    public String getResource() {
        return resource;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public String getAccessToken() {
        return accessToken;
    }

    /// <summary>
    /// Serializes the object to a JSON String
    /// </summary>
    /// <returns>Deserialized authentication result</returns>
    static AuthenticationResult deserialize(String serializedObject) throws IOException {
        return JsonHelper.deserialize(AuthenticationResult.class, serializedObject);
    }

    /// <summary>
    /// Creates authorization header from authentication result.
    /// </summary>
    /// <returns>Created authorization header</returns>
    public String createAuthorizationHeader() {
        return Oauth2AuthorizationHeader + this.accessToken;
    }

    /// <summary>
    /// Serializes the object to a JSON String
    /// </summary>
    /// <returns>Serialized authentication result</returns>
    String serialize() throws IOException {
        return JsonHelper.serialize(this);
    }

    void updateTenantAndUserInfo(String tenantId, String idToken, UserInfo userInfo) {
        this.tenantId = tenantId;
        this.idToken = idToken;
        if (userInfo != null) {
            this.userInfo = new UserInfo(userInfo);
        }
    }

    @JsonProperty
    String userAssertionHash;

}

