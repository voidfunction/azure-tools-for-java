package com.microsoft.azure.hdinsight.spark.common;

public class SparkSubmissionJobConfigCheckResult {

    private SparkSubmissionJobConfigCheckStatus status;
    private String messaqge;

    public SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus status, String message){
        this.status = status;
        this.messaqge = message;
    }

    public SparkSubmissionJobConfigCheckStatus getStatus(){
        return status;
    }

    public String getMessaqge(){
        return messaqge;
    }
}
