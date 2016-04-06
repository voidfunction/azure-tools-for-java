package com.microsoft.azure.hdinsight.common;

public class HDInsightHelper {

    private HDInsightHelper(){}

    private static HDInsightHelper instance = null;

    public static HDInsightHelper getInstance(){
        if(instance == null){
            synchronized (HDInsightHelper.class){
                if(instance == null){
                    instance = new HDInsightHelper();
                }
            }
        }

        return instance;
    }

}

