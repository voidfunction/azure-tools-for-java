package com.microsoft.azure.hdinsight.common.task;


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.microsoft.tooling.msservices.helpers.NotNull;

import java.util.concurrent.Executors;

public class TaskExecutor {
    private static ListeningExecutorService executors = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    public static <T> ListenableFuture<T> submit(@NotNull Task<T> task) {
        final ListenableFuture<T> listenableFuture = executors.submit(task);
        Futures.addCallback(listenableFuture, task.callback);
        return listenableFuture;
    }
}
