package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.sdk.storage.BlobContainer;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IDEHelper {
    void executeOnPooledThread(@NotNull Runnable runnable);

    void invokeLater(@NotNull Runnable runnable);

    void invokeAndWait(@NotNull Runnable runnable);

    void runInBackground(@Nullable Object project, @NotNull String name, boolean canBeCancelled,
                         boolean isIndeterminate, @Nullable String indicatorText,
                         Runnable runnable);
    @Nullable
    String getProperty(@NotNull Object projectObject, @NotNull String name);

    @NotNull
    String getProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String defaultValue);

    void setProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String value);

    void unsetProperty(@NotNull Object projectObject, @NotNull String name);

    boolean isPropertySet(@NotNull Object projectObject, @NotNull String name);

    @Nullable
    String getProperty(@NotNull String name);

    @NotNull
    String getProperty(@NotNull String name, @NotNull String defaultValue);

    void setProperty(@NotNull String name, @NotNull String value);

    void unsetProperty(@NotNull String name);

    boolean isPropertySet(@NotNull String name);

    @Nullable
    String[] getProperties(@NotNull String name);

    void setProperties(@NotNull String name, @NotNull String[] value);

    void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile);

    Object getOpenedFile(@NotNull Object projectObject,
                         @NotNull StorageAccount storageAccount,
                         @NotNull BlobContainer item);

    void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile);

    void openItem(@NotNull Object projectObject,
                          @Nullable StorageAccount storageAccount,
                          @NotNull  BlobContainer item,
                          @Nullable String itemType,
                          @NotNull final String itemName,
                          @Nullable final String iconName);
    void refreshBlobs(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                      @NotNull final BlobContainer container);
}