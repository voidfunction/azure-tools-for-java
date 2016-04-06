package com.microsoft.azure.hdinsight.spark.common;

/**
 * Created by joezhang on 15-10-27.
 */
public class HttpResponse {

    private int code;
    private String message;
    private String reason;

    public HttpResponse(int code, String message, String reason){
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
