package com.microsoft.azure.hdinsight.projects.samples;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.ModuleRootManagerComponent;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.DefaultLoader;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.common.StringHelper;
import com.microsoft.azure.hdinsight.projects.HDInsightTemplatesType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;

public class ProjectSampleUtil {

    //sample file path should be start with "/"
    private static final String[] Java_Local_RunSample = new String[]{"/samples/java/JavaSparkPi.java"};
    private static final String[] Scala_Cluster_Run_Sample = new String[]{
            "/samples/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
            "/samples/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
            "/samples/scala/scala_cluster_run/SparkSQL_RDDRelation.scala"
    };
    private static final String[] Scala_Local_Run_Sample = new String[]{
            "/samples/scala/scala_local_run/LogQuery.scala",
            "/samples/scala/scala_local_run/SparkML_RankingMetricsExample.scala"
    };
    private static final String[] Scala_Local_Run_Sample_Data = new String[]{"/samples/scala/scala_local_run/data/sample_movielens_data.txt"};

    private static final String MODULE_COMPONENT_NAME = "NewModuleRootManager";

    public static void copyFileToPath(Module module, HDInsightTemplatesType templatesType) {
        String sourcePath = getRootOrSourceFolder(module, true);
        String rootPath = getRootOrSourceFolder(module, false);

        if (StringHelper.isNullOrWhiteSpace(sourcePath) || StringHelper.isNullOrWhiteSpace(rootPath)) {
            DefaultLoader.getUIHelper().showError("Failed get root or resource folder of current module", "Create Sample Project");
        }else {
            try {
                if (templatesType == HDInsightTemplatesType.ScalaLocalSample) {
                    copyFileToPath(Scala_Local_Run_Sample, sourcePath);
                    copyFileToPath(Scala_Local_Run_Sample_Data, StringHelper.concat(rootPath, File.separator, "data"));
                } else if (templatesType == HDInsightTemplatesType.ScalaClusterSample) {
                    copyFileToPath(Scala_Cluster_Run_Sample, sourcePath);
                } else if(templatesType == HDInsightTemplatesType.JavaLocalSample) {
                    copyFileToPath(Java_Local_RunSample, sourcePath);
                }
            } catch (Exception e) {
                DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
            }
        }
    }

    public static String getRootOrSourceFolder(Module module, boolean isSourceFolder) {
        ModuleRootManagerComponent component = (ModuleRootManagerComponent)module.getComponent(MODULE_COMPONENT_NAME);
        VirtualFile[] files = isSourceFolder ? component.getSourceRoots() : component.getContentRoots();

        if(files.length == 0) {
            DefaultLoader.getUIHelper().showError("Source Root should be created if you want to create a new sample project", "Create Sample Project");
            return null;
        }
        return files[0].getPath();
    }

    @NotNull
    private static String getNameFromPath(@NotNull String path) {
        int index = path.lastIndexOf('/');
        return path.substring(index);
    }

    private static void copyFileToPath(String[] resources, String toPath) throws Exception {
        for (int i = 0; i < resources.length; ++i) {
            File file = StreamUtil.getResourceFile(resources[i]);

            if (file == null) {
                DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
            } else {
                String toFilePath = StringHelper.concat(toPath, getNameFromPath(resources[i]));
                FileUtil.copy(file, new File(toFilePath));
            }
        }
    }}
