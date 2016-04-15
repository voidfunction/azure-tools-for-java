package com.microsoft.azure.hdinsight.serverexplore;

import org.jetbrains.annotations.NotNull;

public interface AADManagerRequestCallback<T> {
    @NotNull
    T execute(@NotNull String accessToken)
            throws Exception;
}