package com.microsoft.azure.hdinsight.common;

public class HttpResponseWithoutHeader {

    private int code;
    private String message;
    private String reason;

    public HttpResponseWithoutHeader(int code, String message, String reason){
        this.code = code;
        this.message = message;
        this.reason = reason;
    }

    public int getStatusCode(){
        return this.code;
    }

    public String getMessage(){
        return this.message;
    }

    public String getReason(){
        return this.reason;
    }
}
