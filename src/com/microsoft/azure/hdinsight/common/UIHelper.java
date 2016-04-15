package com.microsoft.azure.hdinsight.common;

import org.jetbrains.annotations.NotNull;

public interface UIHelper {
    void showException(@NotNull String message,
                       Throwable ex,
                       @NotNull String title,
                       boolean appendEx,
                       boolean suggestDetail);

    void showError(@NotNull String message, @NotNull String title);
}