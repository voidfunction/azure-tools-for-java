package com.microsoft.azure.hdinsight.sdk.common;

public class AuthenticationError {
    private AuthenticationErrorDetail error;

    public AuthenticationErrorDetail getErrorDetail(){
        return error;
    }
    public String getError(){
        return error.toString();
    }
}

class AuthenticationErrorDetail{
    private String code;
    private String message;

    public String getCode(){
        return code;
    }

    public String getMessage(){
        return message;
    }

    @Override
    public String toString(){
        return String.format("code : %s, message : %s", code, message);
    }
}