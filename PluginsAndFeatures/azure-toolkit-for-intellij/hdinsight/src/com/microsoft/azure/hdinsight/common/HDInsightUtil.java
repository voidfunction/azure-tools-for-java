package com.microsoft.azure.hdinsight.common;


import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.intellij.ToolWindowKey;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureServiceModule;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;

public class HDInsightUtil {
    private static final Object LOCK = new Object();

    public static String getPluginRootDirectory() {
        IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID));
        return pluginDescriptor.getPath().getAbsolutePath();
    }

    public static void setHDInsightRootModule(@NotNull AzureServiceModule azureServiceModule) {
        HDInsightRootModuleImpl hdInsightRootModule =  new HDInsightRootModuleImpl(azureServiceModule);
        azureServiceModule.setHdInsightModule(hdInsightRootModule);
    }

    @Nullable
    public static JobStatusManager getJobStatusManager(@NotNull Project project) {
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        if(PluginUtil.isContainsToolWindowKey(key)){
            return ((SparkSubmissionToolWindowProcessor)PluginUtil.getToolWindowManager(key)).getJobStatusManager();
        } else {
            return null;
        }
    }

    public static SparkSubmissionToolWindowProcessor getSparkSubmissionToolWindowManager(Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if(!PluginUtil.isContainsToolWindowKey(key)) {
            SparkSubmissionToolWindowProcessor sparkSubmissionToolWindowProcessor = new SparkSubmissionToolWindowProcessor(toolWindow);
            PluginUtil.registerToolWindowManager(key, sparkSubmissionToolWindowProcessor);
            sparkSubmissionToolWindowProcessor.initialize();
        }

        return (SparkSubmissionToolWindowProcessor)PluginUtil.getToolWindowManager(key);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message, boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(project, Info, message, isNeedClear);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Info, message, false);
    }

    public static void showErrorMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Error, message, false);
    }

    public static void showWarningMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Warning, message, false);
    }

    private static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final MessageInfoType type, @NotNull final String message, @NotNull final boolean isNeedClear) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if (!toolWindow.isVisible()) {
            synchronized (LOCK) {
                if (!toolWindow.isVisible()) {
                    if (ApplicationManager.getApplication().isDispatchThread()) {
                        toolWindow.show(null);
                    } else {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    toolWindow.show(null);
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        showSubmissionMessage(getSparkSubmissionToolWindowManager(project), message, type, isNeedClear);
    }

    private static void showSubmissionMessage(SparkSubmissionToolWindowProcessor processor, @NotNull String message, @NotNull MessageInfoType type, @NotNull final boolean isNeedClear) {
        if (isNeedClear) {
            processor.clearAll();
        }

        switch (type) {
            case Error:
                processor.setError(message);
                break;
            case Info:
                processor.setInfo(message);
                break;
            case Warning:
                processor.setWarning(message);
                break;
        }
    }
}
