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

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public String getUniqueId() {
        return uniqueId;
    }

    public String getDisplayableId() {
        return displayableId;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public long getPasswordExpiresOn() {
        return passwordExpiresOn;
    }

    public URI getPasswordChangeUrl() {
        return passwordChangeUrl;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public boolean isForcePrompt() {
        return forcePrompt;
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
