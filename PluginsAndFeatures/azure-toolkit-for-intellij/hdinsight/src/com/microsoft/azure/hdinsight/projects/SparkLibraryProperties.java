package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.roots.libraries.LibraryProperties;
import org.jetbrains.annotations.Nullable;

public class SparkLibraryProperties extends LibraryProperties<SparkLibraryPropertiesState> {

    private SparkLibraryPropertiesState state;

    public SparkLibraryProperties() {
        loadState(new SparkLibraryPropertiesState());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SparkLibraryPropertiesState) {
            SparkLibraryPropertiesState that = (SparkLibraryPropertiesState)o;
            return this.state.equals(that);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return state.languageLevel.hashCode() + state.compilerClasspath.hashCode();
    }

    @Nullable
    @Override
    public SparkLibraryPropertiesState getState() {
        return new SparkLibraryPropertiesState();
    }

    @Override
    public void loadState(SparkLibraryPropertiesState sparkLibraryPropertiesState) {
        state = sparkLibraryPropertiesState;
    }
}
