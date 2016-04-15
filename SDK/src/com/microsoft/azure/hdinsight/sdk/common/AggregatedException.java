package com.microsoft.azure.hdinsight.sdk.common;

import java.util.ArrayList;
import java.util.List;

public class AggregatedException extends Exception{
    private List<Exception> exceptionList = new ArrayList<>();
    public AggregatedException(List<Exception> exceptionList){
        this.exceptionList = exceptionList;
    }

    public List<Exception> getExceptionList(){
        return exceptionList;
    }
}
