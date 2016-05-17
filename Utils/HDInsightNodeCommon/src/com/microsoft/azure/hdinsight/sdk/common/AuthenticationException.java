package com.microsoft.azure.hdinsight.sdk.common;

public class AuthenticationException extends HDIException {
    public AuthenticationException(String message, int errorCode) {
        super(message, errorCode);
    }
}
