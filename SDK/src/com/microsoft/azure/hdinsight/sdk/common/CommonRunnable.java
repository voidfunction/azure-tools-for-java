package com.microsoft.azure.hdinsight.sdk.common;

public abstract class CommonRunnable<T,E extends Exception> implements Runnable {
    private final T parameter;

    public CommonRunnable(T parameter) {
        this.parameter = parameter;
    }

    public abstract void runSpecificParameter(T parameter) throws E;

    public abstract void exceptionHandle(Exception e);

    public void run(){
        try {
            runSpecificParameter(this.parameter);
        }
        catch (Exception e) {
            exceptionHandle(e);
        }
    }
}
