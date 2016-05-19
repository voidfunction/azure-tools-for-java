package com.microsoft.azure.hdinsight.projects;

import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;

import java.util.Arrays;

public class SparkLibraryPropertiesState {

    //TODO: add Properties State(this is a fake solution)
    public SparkLanguageLevelProxy languageLevel;

    @Tag("compiler-classpath")
    @AbstractCollection(surroundWithTag = false, elementTag = "root", elementValueAttribute = "url")
    public String[] compilerClasspath = new String[]{};

    public SparkLibraryPropertiesState() {
        languageLevel = SparkLanguageLevelProxy.SPARK_1_5_2;
    }

    @Override
    public boolean equals(Object obj) {
        SparkLibraryPropertiesState that = (SparkLibraryPropertiesState) obj;
        return languageLevel == that.languageLevel && Arrays.equals(compilerClasspath, that.compilerClasspath);
    }
}
