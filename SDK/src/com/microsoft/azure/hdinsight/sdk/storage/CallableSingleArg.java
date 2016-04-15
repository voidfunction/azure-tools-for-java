package com.microsoft.azure.hdinsight.sdk.storage;

import java.util.concurrent.Callable;

public abstract class CallableSingleArg<T, TArg> implements Callable<T> {
    @Override
    public final T call() throws Exception {
        return call(null);
    }

    public abstract T call(TArg argument) throws Exception;
}
