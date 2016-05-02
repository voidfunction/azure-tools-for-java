package com.microsoft.auth;

import org.codehaus.jackson.annotate.JsonProperty;

enum AuthorizationStatus
{
    Failed,
    Success
};

public class AuthorizationResult
{
    AuthorizationResult(String code)
    {
        this.status = AuthorizationStatus.Success;
        this.code = code;
    }
    
    AuthorizationResult(String error, String errorDescription)
    {
        this.status = AuthorizationStatus.Failed;
        this.error = error;
        this.errorDescription = errorDescription;
    }
    
    AuthorizationStatus status;
    
    @JsonProperty("Code")
    String code;
    
    @JsonProperty("Error")
    String error;
    
    @JsonProperty("ErrorDescription")
    String errorDescription;
}