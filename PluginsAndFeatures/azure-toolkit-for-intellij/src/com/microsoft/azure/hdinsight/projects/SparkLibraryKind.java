package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import org.jetbrains.annotations.NotNull;

public class SparkLibraryKind extends PersistentLibraryKind<SparkLibraryProperties> {

    private static SparkLibraryKind sparkLibraryKind = new SparkLibraryKind();

    public static SparkLibraryKind getInstance() {
        return sparkLibraryKind;
    }

    private SparkLibraryKind() {
        super("com.microsoft.azure.hdinsight.spark");
    }

    @NotNull
    @Override
    public SparkLibraryProperties createDefaultProperties() {
        return new SparkLibraryProperties();
    }
}
