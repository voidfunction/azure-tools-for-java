package com.microsoft.auth;

import org.codehaus.jackson.annotate.JsonProperty;

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
    public String idToken;

    /// <summary>
    /// Gets a value indicating whether the refresh token can be used for requesting access token for other resources.
    /// </summary>
    @JsonProperty
    boolean isMultipleResourceRefreshToken;
    
    public String getAccessToken() {
    	return accessToken;
    }

    /// <summary>
    /// Serializes the object to a JSON String
    /// </summary>
    /// <returns>Deserialized authentication result</returns>
    public static AuthenticationResult deserialize(String serializedObject) throws Exception {
    	AuthenticationResult result = JsonHelper.deserialize(AuthenticationResult.class, serializedObject);
//        AuthenticationResult result;
//        var serializer = new DataContractJsonSerializer(typeof(AuthenticationResult));
//        byte[] serializedObjectBytes = Encoding.UTF8.GetBytes(serializedObject);
//        using (var stream = new MemoryStream(serializedObjectBytes))
//        {
//            result = (AuthenticationResult)serializer.ReadObject(stream);
//        }

        return result;
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
    String serialize() throws Exception {
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

