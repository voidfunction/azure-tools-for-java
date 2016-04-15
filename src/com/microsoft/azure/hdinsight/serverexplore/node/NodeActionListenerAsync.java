package com.microsoft.azure.hdinsight.serverexplore.node;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.common.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public abstract class NodeActionListenerAsync extends NodeActionListener {
    private String progressMessage;

    public NodeActionListenerAsync(@NotNull String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public ListenableFuture<Void> actionPerformedAsync(final NodeActionEvent actionEvent) {
        Callable<Boolean> booleanCallable = beforeAsyncActionPerfomed();

        boolean shouldRun = true;

        try {
            shouldRun = booleanCallable.call();
        } catch (Exception ignored) {
        }

        final SettableFuture<Void> future = SettableFuture.create();

        if (shouldRun) {
            DefaultLoader.getIdeHelper().runInBackground(actionEvent.getAction().getNode().getProject(), progressMessage, true, false, null, new Runnable() {
                @Override
                public void run() {
                    try {
                        actionPerformed(actionEvent);
                        future.set(null);
                    } catch (HDExploreException e) {
                        future.setException(e);
                    }
                }
            });
        } else {
            future.set(null);
        }

        return future;
    }

    @NotNull
    protected abstract Callable<Boolean> beforeAsyncActionPerfomed();
}