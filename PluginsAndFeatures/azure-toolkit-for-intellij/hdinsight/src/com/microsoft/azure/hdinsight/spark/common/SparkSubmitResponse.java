package com.microsoft.azure.hdinsight.spark.common;

/**
 * Created by joezhang on 15-12-30.
 */
public class SparkSubmitResponse {
    private int id;
    private String state;

    public int getId(){
        return id;
    }

    public String getState(){
        return state;
    }
}
