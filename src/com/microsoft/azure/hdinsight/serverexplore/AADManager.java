package com.microsoft.azure.hdinsight.serverexplore;

import org.jetbrains.annotations.NotNull;

public interface AADManager {
    @NotNull
    UserInfo authenticate(@NotNull String resource, @NotNull String title)
            throws HDExploreException;

    void authenticate(@NotNull UserInfo userInfo,
                      @NotNull String resource,
                      @NotNull String title)
            throws HDExploreException;

    @NotNull
    <T> T request(@NotNull UserInfo userInfo,
                  @NotNull String resource,
                  @NotNull String title,
                  @NotNull AADManagerRequestCallback<T> AADManagerRequestCallback)
            throws HDExploreException;
}