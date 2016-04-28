package com.microsoft.auth;

import java.net.URI;

import org.codehaus.jackson.annotate.JsonProperty;

public final class UserInfo {
    UserInfo(){}
    
    UserInfo(String uniqueId, String displayableId, String givenName, String familyName, String identityProvider, long passwordExpiresOffest, URI passwordChangeUrl){            
        this.uniqueId = uniqueId;
        this.displayableId = displayableId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.identityProvider = identityProvider;
        this.passwordChangeUrl = passwordChangeUrl;
        this.passwordExpiresOn = passwordExpiresOffest;
    }

    UserInfo(UserInfo other) {
        this.uniqueId = other.uniqueId;
        this.displayableId = other.displayableId;
        this.givenName = other.givenName;
        this.familyName = other.familyName;
        this.identityProvider = other.identityProvider;
        this.passwordChangeUrl = other.passwordChangeUrl;
        this.passwordExpiresOn = other.passwordExpiresOn;
    }

    /// <summary>
    /// Gets identifier of the user authenticated during token acquisition. 
    /// </summary>
    @JsonProperty
    String uniqueId;

    /// <summary>
    /// Gets a displayable value in UserPrincipalName (UPN) format. The value can be null.
    /// </summary>
    @JsonProperty
    String displayableId;

    /// <summary>
    /// Gets given name of the user if provided by the service. If not, the value is null. 
    /// </summary>
    @JsonProperty
    String givenName;

    /// <summary>
    /// Gets family name of the user if provided by the service. If not, the value is null. 
    /// </summary>
    @JsonProperty
    String familyName;

    /// <summary>
    /// Gets the time when the password expires. Default value is 0.
    /// </summary>
    @JsonProperty
    long passwordExpiresOn;

    /// <summary>
    /// Gets the url where the user can change the expiring password. The value can be null.
    /// </summary>
    @JsonProperty
    URI passwordChangeUrl;

    /// <summary>
    /// Gets identity provider if returned by the service. If not, the value is null. 
    /// </summary>
    @JsonProperty
    String identityProvider;

    boolean forcePrompt;
}
