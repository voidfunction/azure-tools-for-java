package com.microsoft.azuretools.authmanage.models;

import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by shch on 10/8/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthMethodDetails {

    @JsonIgnore
    private String accountEmail;

    @JsonProperty
    private String credFilePath;

    @JsonProperty
    private AuthMethod authMethod;


    // for jackson json
    public AuthMethodDetails() {
        this.authMethod = AuthMethod.AD;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getCredFilePath() {
        return credFilePath;
    }

    public void setCredFilePath(String credFilePath) {
        this.credFilePath = credFilePath;
    }



}
