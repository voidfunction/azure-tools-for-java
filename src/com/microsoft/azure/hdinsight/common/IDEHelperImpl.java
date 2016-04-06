package com.microsoft.azure.hdinsight.common;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.hdinsight.sdk.storage.BlobContainer;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.UI.BlobExplorerFileEditor;
import com.microsoft.azure.hdinsight.serverexplore.UI.BlobExplorerFileEditorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class IDEHelperImpl implements IDEHelper {
    public static Key<StorageAccount> STORAGE_KEY = new Key<>("clientStorageAccount");

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Override
    public void runInBackground(@Nullable final Object project, @NotNull final String name, final boolean canBeCancelled,
                                final boolean isIndeterminate, @Nullable final String indicatorText,
                                final Runnable runnable) {
        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable((Project) project,
                        name, canBeCancelled) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        if (isIndeterminate) {
                            indicator.setIndeterminate(true);
                        }

                        if (indicatorText != null) {
                            indicator.setText(indicatorText);
                        }

                        runnable.run();
                    }
                });
            }
        }, ModalityState.any());
    }

    @Nullable
    @Override
    public String getProperty(@NotNull String name) {
        return PropertiesComponent.getInstance().getValue(name);
    }

    @NotNull
    @Override
    public String getProperty(@NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance().getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
        PropertiesComponent.getInstance().setValue(name, value);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().saveSettings();
            }
        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        PropertiesComponent.getInstance().unsetValue(name);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().saveSettings();
            }
        }, ModalityState.any());
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return PropertiesComponent.getInstance().isValueSet(name);
    }

    @Nullable
    @Override
    public String getProperty(@NotNull Object projectObject, @NotNull String name) {
        return PropertiesComponent.getInstance((Project) projectObject).getValue(name);
    }

    @NotNull
    @Override
    public String getProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance((Project) projectObject).getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull Object projectObject, @NotNull String name, @NotNull String value) {
        final Project project = (Project) projectObject;
        PropertiesComponent.getInstance(project).setValue(name, value);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                project.save();
            }
        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull Object projectObject, @NotNull String name) {
        final Project project = (Project) projectObject;
        PropertiesComponent.getInstance(project).unsetValue(name);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                project.save();
            }
        }, ModalityState.any());
    }

    @Override
    public boolean isPropertySet(@NotNull Object projectObject, @NotNull String name) {
        return PropertiesComponent.getInstance((Project) projectObject).isValueSet(name);
    }

    @Override
    public String[] getProperties(@NotNull String name) {
        return PropertiesComponent.getInstance().getValues(name);
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
        PropertiesComponent.getInstance().setValues(name, value);
        ApplicationManager.getApplication().saveSettings();
    }

    @Override
    public void refreshBlobs(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final BlobContainer container) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount, container);
                if (file != null) {
                    final BlobExplorerFileEditor containerFileEditor = (BlobExplorerFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            containerFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void openItem(@NotNull Object projectObject,
                          @Nullable StorageAccount storageAccount,
                          @NotNull  BlobContainer item,
                          @Nullable String itemType,
                          @NotNull final String itemName,
                          @Nullable final String iconPath) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData(BlobExplorerFileEditorProvider.CONTAINER_KEY, item);
        itemVirtualFile.putUserData(STORAGE_KEY, storageAccount);

        itemVirtualFile.setFileType(new FileType() {
            @NotNull
            @Override
            public String getName() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDescription() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return "";
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return PluginUtil.getIcon(iconPath);
            }

            @Override
            public boolean isBinary() {
                return true;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
                return "UTF8";
            }
        });

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).openFile((VirtualFile) itemVirtualFile, true, true);
            }
        }, ModalityState.any());
    }

    @Override
    public  Object getOpenedFile(@NotNull Object projectObject,
                                 @NotNull StorageAccount storageAccount,
                                 @NotNull BlobContainer item) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            BlobContainer editedItem = editedFile.getUserData(BlobExplorerFileEditorProvider.CONTAINER_KEY);
            StorageAccount editedStorageAccount = editedFile.getUserData(STORAGE_KEY);

            if (editedStorageAccount != null
                    && editedItem != null
                    && editedStorageAccount.getStorageName().equals(storageAccount.getStorageName())
                    && editedItem.getName().equals(item.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    @Override
    public void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile);
            }
        }, ModalityState.any());
    }
}
