package com.microsoft.azure.hdinsight.debug;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.tooling.msservices.helpers.azure.AzureArmManagerImpl;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureServiceModule;

/**
 * Created by ltian on 9/20/2016.
 */
public class RemoteDebug {
    public static void ff(Project project) {
//        AzureArmManagerImpl.getManager(project).
        ClusterManagerEx.getInstance().
    }
}
