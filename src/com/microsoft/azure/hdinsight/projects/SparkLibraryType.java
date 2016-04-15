package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.NoSuchElementException;

public class SparkLibraryType extends LibraryType<SparkLibraryProperties>{

    @Nullable
    @Override
    public Icon getIcon(@Nullable LibraryProperties properties) {
        return PlatformIcons.LIBRARY_ICON;
    }

    public SparkLibraryType() {
        super(SparkLibraryKind.getInstance());
    }
    @Nullable
    @Override
    public String getCreateActionName() {
        return "Spark SDK";
    }

    public static SparkLibraryType getInstance() {
        return LibraryType.EP_NAME.findExtension(SparkLibraryType.class);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent jComponent, @Nullable VirtualFile virtualFile, @NotNull Project project) {
        return (new SparkLibraryDescription()).createNewLibrary(jComponent, virtualFile);
    }

    @Nullable
    @Override
    public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<SparkLibraryProperties> libraryEditorComponent) {
        return null;
    }
}
