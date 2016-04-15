package com.microsoft.azure.hdinsight.spark.common;

import java.util.List;

/**
 * Created by joezhang on 15-12-30.
 */
public class SparkJobLog {
    private int id;
    private int from;
    private int total;
    private List<String> log;

    public int getId(){
        return id;
    }

    public int getFrom(){
        return from;
    }

    public int getTotal(){
        return total;
    }

    public List<String> getLog(){
        return log;
    }
}
