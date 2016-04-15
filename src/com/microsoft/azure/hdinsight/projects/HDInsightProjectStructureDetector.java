package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.importSources.JavaSourceRootDetector;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.project.notification.source.ScalaDirUtil;

public class HDInsightProjectStructureDetector extends JavaSourceRootDetector {

    @NotNull
    @Override
    public String getLanguageName() {
        return "Scala";
    }

    @NotNull
    @Override
    public String getFileExtension() {
        return "scala";
    }

    @NotNull
    @Override
    public NullableFunction<CharSequence, String> getPackageNameFetcher() {
        return new NullableFunction<CharSequence, String>() {
            public String fun(CharSequence var1) {
               return ScalaDirUtil.getPackageStatement(var1).toString();
            }
        };
    }
}
